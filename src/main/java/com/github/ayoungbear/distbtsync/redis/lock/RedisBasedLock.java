/**
 * Copyright 2021 yangzexiong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ayoungbear.distbtsync.redis.lock;

import com.github.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.stream.Collectors;

/**
 * 基于 redis 的可重入分布式锁.
 * 可通过 {@link #newSharedLock(String, RedisLockCommands)} 方式来获取使用
 * 共享阻塞队列的公平锁对象.
 *
 * @author yangzexiong
 * @see RedisLockCommands
 */
public class RedisBasedLock extends AbstractRedisLock {

    private final Sync sync;

    /**
     * 是否公平锁模式.
     * 这里的公平指的是相对公平, 只有使用相同阻塞队列的线程之间会保持先来后到的加锁顺序, 
     * 使用不同队列或者处于不同服务节点上的锁对象之间是处于竞争关系的.
     */
    private final boolean fair;

    /**
     * 记录当前对象中有多少线程在自旋竞争锁
     */
    private AtomicInteger competitor = new AtomicInteger(0);

    public RedisBasedLock(String key, RedisLockCommands commands) {
        this(key, commands, true);
    }

    public RedisBasedLock(String key, RedisLockCommands commands, boolean fair) {
        super(key, commands);
        this.sync = Sync.newInstance(key);
        this.fair = fair;
    }

    private RedisBasedLock(String key, RedisLockCommands commands, Sync sync) {
        super(key, commands);
        this.sync = sync;
        this.fair = true;
    }

    /**
     * 创建基于 redis 的分布式锁, 所有根据此方法创建的使用相同 {@code key} 的锁对象,
     * 都使用相同的共享阻塞队列, 并且锁为公平锁.
     *
     * @see Sync#SYNC_QUEUE_CACHE
     * @param key
     * @param commands
     * @return
     */
    public static RedisBasedLock newSharedLock(String key, RedisLockCommands commands) {
        return new RedisBasedLock(key, commands, Sync.newShared(key));
    }

    /**
     * 获取已缓存的共享阻塞队列的长度.
     * @return
     */
    public static int getSharedSyncCacheSize() {
        return Sync.SYNC_QUEUE_CACHE.size();
    }

    /**
     * 获取已缓存的共享阻塞队列对应分布式锁名称的集合.
     * @return
     */
    public static Set<String> getSharedSyncCacheKeySet() {
        return Sync.SYNC_QUEUE_CACHE.keySet().stream().map((sync) -> sync.key).collect(Collectors.toSet());
    }

    @Override
    public void lock() {
        try {
            spinLock((lock) -> lock.tryLock(), false, NOT_WAIT_TIME);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        spinLock((lock) -> lock.tryLock(), true, NOT_WAIT_TIME);
    }

    @Override
    public boolean tryLock() {
        return tryLock(UNLIMIT_LEASE_TIME);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (time <= 0L) {
            return false;
        }
        return spinLock((lock) -> lock.tryLock(), true, unit.toNanos(time));
    }

    @Override
    public void unlock() throws IllegalMonitorStateException {
        boolean result = false;
        if (onceLocked()) {
            // 这里解锁时如果锁已过期并且被他人上锁了会出现解锁失败的情况
            result = releaseLock();
        }
        if (!result) {
            throw new IllegalMonitorStateException("Not locked by current thread");
        }
    }

    @Override
    public void lockTimed(long leaseTime, TimeUnit unit) {
        validateLeaseTime(leaseTime);
        try {
            spinLock((lock) -> lock.tryLockTimed(leaseTime, unit), false, NOT_WAIT_TIME);
        } catch (InterruptedException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean tryLockTimed(long leaseTime, TimeUnit unit) {
        validateLeaseTime(leaseTime);
        return tryLock(unit.toMillis(leaseTime));
    }

    @Override
    public boolean tryLockTimed(long time, long leaseTime, TimeUnit unit) throws InterruptedException {
        validateLeaseTime(leaseTime);
        if (time <= 0L) {
            return false;
        }
        return spinLock((lock) -> lock.tryLockTimed(leaseTime, unit), true, unit.toNanos(time));
    }

    @Override
    public boolean renewLeaseTime(long leaseTime, TimeUnit unit) {
        validateLeaseTime(leaseTime);
        if (onceLocked()) {
            return doExpired(getSourceIdentifier(), unit.toMillis(leaseTime));
        }
        return false;
    }

    @Override
    public boolean releaseLock() {
        if (onceLocked()) {
            String identifier = getSourceIdentifier();
            if (identifier != null) {
                int holdCount = doTryRelease(identifier);
                boolean releaseSuccessful = holdCount >= 0;
                if (holdCount <= 0) {
                    removeIdentifier();
                    // 解锁时自动唤醒阻塞线程, 虽然多触发一次自旋, 但一定程度防止了死锁的发生, 并且在高并发下减少了消息通知的延迟影响
                    sync.signal();
                }
                return releaseSuccessful;
            }
        }
        return false;
    }

    @Override
    public boolean isHeldLock() {
        boolean result = false;
        if (onceLocked()) {
            result = isAcquired(getSourceIdentifier());
        }
        if (!result) {
            removeIdentifier();
        }
        return result;
    }

    @Override
    public int getHoldCount() {
        String identifier = getSourceIdentifier();
        if (identifier != null) {
            return doGetHoldCount(identifier);
        }
        return 0;
    }

    /**
     * 获取当前对象中自旋竞争锁的线程数.
     * @return
     */
    public int getCompetitorCount() {
        return competitor.get();
    }

    @Override
    public String toString() {
        return this.key + "@" + this.commands.getClass().getSimpleName() + "@" + super.toString();
    }

    /**
     * 自旋争用分布式锁, 是所有阻塞或者超时性质加锁方法的基础方法.
     * 如果是公平模式那么所有线程会先获取争用锁的资格, 只有一个线程能获取成功,
     * 然后与其他服务节点或者对象的线程争用分布式锁.
     * 当加锁失败时会进入阻塞, 阻塞的时间与锁的失效时间(如果有)或者超时时间有关,
     * 也有可能是持续阻塞的状态(如果锁没有失效时间), 直到收到了解锁的通知信息,
     * 然后重新尝试加锁.
     * 自旋加锁持续到加锁成功, 或者超时, 或者被中断.
     *
     * @param operation
     * @param interruptible
     * @param timeoutNanos
     * @return
     * @throws InterruptedException
     */
    private final boolean spinLock(RedisLockOperation operation, boolean interruptible, long timeoutNanos)
            throws InterruptedException {
        final long deadline = System.nanoTime() + timeoutNanos;
        // 是否超时可中断模式
        boolean timeoutMode = timeoutNanos > 0;

        // 如果已持有锁则尝试重新加锁, 可重入
        if (onceLocked() && operation.doLock(this)) {
            return true;
        }

        // 获取加锁资格, 公平模式下只有一个能获取成功并执行加锁(或者是锁持有者可重入), 非公平模式下则无需获取直接竞争锁
        boolean canSpinLock = acquire(interruptible, timeoutNanos);
        if (canSpinLock) {
            // 开始自旋加锁
            competitor.incrementAndGet();
            try {
                for (; ; ) {
                    if (operation.doLock(this)) {
                        return true;
                    }

                    // 锁的剩余过期时间, ttl 小于0表示锁没有设置过期时间
                    long nanosTtl = TimeUnit.MILLISECONDS.toNanos(getTtl());
                    if (timeoutMode) {
                        long nanosTimed = deadline - System.nanoTime();
                        // 超时
                        if (nanosTimed <= 0L) {
                            return false;
                        }
                        // 如果锁无过期时间, 那么阻塞的时间以剩余的超时时间为准
                        if (nanosTtl < 0 || nanosTimed < nanosTtl) {
                            nanosTtl = nanosTimed;
                        }
                    }
                    // 当剩余时间过小则继续自旋不再阻塞
                    if (nanosTtl < 0 || nanosTtl > SPIN_FOR_BLOCK_TIMEOUT_THRESHOLD) {
                        if (sync.activeSubWorker(commands, channel)) {
                            sync.await(interruptible, nanosTtl);
                        }
                    }

                    if (interruptible && Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            } finally {
                if (competitor.decrementAndGet() == 0 && !sync.hasQueuedThreads()) {
                    // 如果没有其他线程需要加锁那么停止订阅者的工作
                    sync.terminateSubWorker();
                }
                releaseIfNecessary();
            }
        }
        return false;
    }

    private final boolean tryLock(long leaseTimeMillis) {
        String identifier = getIdentifier();
        boolean acquireSuccessful = doTryAcquire(identifier, leaseTimeMillis);
        if (acquireSuccessful) {
            setSourceIdentifier(identifier);
            return true;
        }
        return false;
    }

    /**
     * 请求争用锁的互斥资源, 公平模式下只有一个线程能争用分布式锁,
     * 获取到后才开始自旋争用分布式锁.
     * @param interruptible
     * @param timeoutNanos
     * @return
     * @throws InterruptedException
     */
    private final boolean acquire(boolean interruptible, long timeoutNanos) throws InterruptedException {
        boolean result = false;
        if (fair) {
            if (interruptible) {
                if (timeoutNanos > 0) {
                    // 超时模式
                    result = sync.acquire(timeoutNanos);
                } else {
                    // 可中断阻塞模式
                    result = sync.acquireInterruptibly();
                }
                if (!result && Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } else {
                // 阻塞模式
                result = sync.acquire();
            }
        } else {
            result = true;
        }
        return result;
    }

    /**
     * 释放互斥资源, 让下个线程争用锁(如果有).
     */
    private final void releaseIfNecessary() {
        if (fair) {
            sync.release();
        }
    }

    /**
     * 基于 {@link AbstractQueuedSynchronizer} 实现的阻塞队列, 作为内部同步器.
     * 同时还负责解锁消息的订阅工作, 在收到解锁消息后通知阻塞线程争用锁.
     *
     * @author yangzexiong
     */
    static class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = -5447636560573717767L;

        /**
         * 共享阻塞队列缓存, 利用 {@link WeakHashMap} 和 {@link WeakReference} 弱引用的特性,
         * 在内存不足时将无用的共享队列清除, 这里将回收功能交给了 GC.
         * 共享阻塞队列是公平模式的, 这么做的目的是为了减少阻塞队列对象的创建和无用的操作, 因为只能有一个对象加锁成功.
         * 可通过 {@link #newShared(String)} 来获取共享阻塞队列.
         */
        private static final WeakHashMap<Sync, WeakReference<Sync>> SYNC_QUEUE_CACHE = new WeakHashMap<>(256);
        private final String key;
        /**
         * 是否共享用队列
         */
        private final boolean shared;
        private Semaphore semaphore;
        /**
         * 解锁信息订阅工作线程
         */
        private volatile SubWorker subWorker;

        private Sync(String key, boolean shared) {
            this.key = key;
            this.shared = shared;
            this.semaphore = new Semaphore(0, false);
        }

        public static Sync newInstance(String key) {
            Sync sync = new Sync(key, false);
            return sync;
        }

        /**
         * 根据给定的 {@code key} 获取对应的共享阻塞队列 {@link Sync}, 队列是属于公平模式加锁类型的.
         * @param key the name of {@link RedisBasedLock}
         * @return the shared blocking queue.
         */
        public static Sync newShared(String key) {
            Sync sync = getRedisSyncLockQueueFromCache(key);
            if (sync == null) {
                synchronized (SYNC_QUEUE_CACHE) {
                    sync = getRedisSyncLockQueueFromCache(key);
                    if (sync == null) {
                        sync = new Sync(key, true);
                        SYNC_QUEUE_CACHE.put(sync, new WeakReference<Sync>(sync));
                    }
                }
            }
            return sync;
        }

        private static Sync getRedisSyncLockQueueFromCache(String key) {
            WeakReference<Sync> syncRef = SYNC_QUEUE_CACHE.get(new Sync(key, true));
            return syncRef == null ? null : syncRef.get();
        }

        @Override
        protected final boolean tryAcquire(int acquires) {
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected final boolean tryRelease(int releases) {
            setState(0);
            return true;
        }

        /**
         * 阻塞模式下获取争用锁的资格.
         * @return
         */
        public boolean acquire() {
            acquire(1);
            return true;
        }

        /**
         * 可中断模式下获取争用锁的资格.
         * @return
         */
        public boolean acquireInterruptibly() {
            try {
                acquireInterruptibly(1);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        /**
         * 超时模式下获取争用锁的资格.
         * @param timeoutNanos
         * @return
         */
        public boolean acquire(long timeoutNanos) {
            try {
                return tryAcquireNanos(1, timeoutNanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        /**
         * 获取到分布式锁后释放争用锁资格, 让下一等待线程争用.
         * @return
         */
        public boolean release() {
            return release(1);
        }

        /**
         * 等待解锁信息, 或者超时/被中断.
         * @param interruptible
         * @param timeoutNanos
         */
        public void await(boolean interruptible, long timeoutNanos) {
            try {
                if (timeoutNanos > 0) {
                    semaphore.tryAcquire(timeoutNanos, TimeUnit.NANOSECONDS);
                } else {
                    if (interruptible) {
                        semaphore.acquire();
                    } else {
                        semaphore.acquireUninterruptibly();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * 唤醒线程争用锁.
         */
        public void signal() {
            signal(1);
        }

        /**
         * 如果有等待线程则唤醒线程争用锁.
         */
        public void signalIfNecessary() {
            if (hasWaiterForRelease()) {
                signal();
            }
        }

        /**
         * 唤醒 {@code num} 个等待线程争用锁.
         * @param num
         */
        public void signal(int num) {
            semaphore.release(num);
        }

        /**
         * 是否有线程等待解锁消息.
         * @return
         */
        public boolean hasWaiterForRelease() {
            return semaphore.hasQueuedThreads();
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Sync)) {
                return false;
            }
            Sync other = (Sync) obj;
            if (this.shared && other.shared) {
                return this.key.equals(other.key);
            }
            return this == other;
        }

        public String getKey() {
            return key;
        }

        /**
         * 启动订阅者, 监听解锁信息.
         * @param commands
         * @param channel
         * @return 订阅者是否已正常开始订阅
         */
        public boolean activeSubWorker(RedisLockCommands commands, String channel) {
            if (!isSubWorkerAlive()) {
                synchronized (this) {
                    if (!isSubWorkerAlive()) {
                        RedisSubscription subscription = commands.getSubscription(channel, this::onReleaseMessage);
                        this.subWorker = SubWorker.create(subscription).setCloseCallback(this::signalIfNecessary)
                                .subscribe();
                    }
                }
            }
            return subWorker.isSubscribed();
        }

        /**
         * 终止订阅工作.
         */
        public void terminateSubWorker() {
            if (subWorker != null) {
                synchronized (this) {
                    if (subWorker != null) {
                        subWorker.unsubscribe();
                        this.subWorker = null;
                    }
                }
            }
        }

        /**
         * 收到解锁消息后唤醒等待线程竞争锁.
         * @param message
         */
        private void onReleaseMessage(String message) {
            signal();
        }

        private boolean isSubWorkerAlive() {
            return subWorker != null && subWorker.isAlive() && !subWorker.isTerminated();
        }

    }

    /**
     * 订阅解锁信息工作线程.
     *
     * @author yangzexiong
     */
    static class SubWorker extends Thread {

        /**
         * 订阅者
         */
        private RedisSubscription subscription;

        /**
         * 终止后的回调
         */
        private Runnable callback;

        private volatile boolean terminated = false;

        public SubWorker(RedisSubscription subscription) {
            super();
            super.setName("RedisBasedLock$SubWorker$from-" + Thread.currentThread().getName() + "$" + super.getName());
            this.subscription = subscription;
        }

        public static SubWorker create(RedisSubscription subscription) {
            return new SubWorker(subscription);
        }

        @Override
        public void run() {
            try {
                while (!isTerminated()) {
                    subscription.subscribe();
                    if (Thread.interrupted()) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw e;
            } finally {
                close();
            }
        }

        public SubWorker subscribe() {
            this.start();
            return this;
        }

        public SubWorker unsubscribe() {
            try {
                terminate();
                subscription.unsubscribe();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                close();
            }
            return this;
        }

        public boolean isTerminated() {
            return terminated;
        }

        public boolean isSubscribed() {
            return subscription.isSubscribed();
        }

        public SubWorker setCloseCallback(Runnable callback) {
            this.callback = callback;
            return this;
        }

        private void terminate() {
            terminated = true;
        }

        private synchronized void close() {
            if (this.callback != null) {
                this.callback.run();
                this.callback = null;
            }
            if (subscription != null) {
                subscription.close();
                subscription = null;
            }
        }

    }

}

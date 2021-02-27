package com.ayoungbear.distbtsync.redis.lock;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ayoungbear.distbtsync.redis.BaseSpringRedisTest;
import com.ayoungbear.distbtsync.redis.lock.support.JedisClusterAdapter;
import com.ayoungbear.distbtsync.redis.lock.support.RedisConnectionAdapter;

/**
 * 基于 redis 的可重入分布式锁单元测试
 * 
 * @author yangzexiong
 */

public class RedisBasedLockTest extends BaseSpringRedisTest {

    public static final Logger logger = LoggerFactory.getLogger(RedisBasedLockTest.class);

    protected String key = "myLock";
    
    @Before
    public void setUp() throws Exception {
        getRedisLock().forceUnlock();
    }

    @After
    public void tearDown() throws Exception {
        getRedisLock().forceUnlock();
    }

    protected RedisLock getRedisLock(String key) {
        RedisLockCommands commands = null;
        // commands = getJedisClusterAdapter(); // 使用 jedis cluster 测试
        // commands = getJedisPoolAdapter(); // 使用 jedis pool 测试
        commands = new RedisConnectionAdapter(redisConnectionFactory); // 使用 RedisConnection 测试, 默认会使用 LettuceConnection
        return new RedisBasedLock(key, commands);
    }

    protected RedisLock getRedisLock() {
        return getRedisLock(key);
    }

    /**
     * 测试是否持有锁.
     */
    @Test
    public void testIsHeldLock() {
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isHeldLock());
        lock.lock();
        Assert.assertEquals(true, lock.isHeldLock());
        lock.unlock();
        Assert.assertEquals(false, lock.isHeldLock());
    }

    /**
     * 测试加锁与获取加锁次数.
     */
    @Test
    public void testGetHoldCount() {
        RedisLock lock = getRedisLock();
        Assert.assertEquals(0, lock.getHoldCount());
        lock.lock();
        Assert.assertEquals(1, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(0, lock.getHoldCount());

        lock.lock();
        lock.lock();
        Assert.assertEquals(2, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(1, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(0, lock.getHoldCount());
    }

    /**
     * 测试加锁成功并判断是否持有锁和加锁次数.
     */
    @Test
    public void testLockSuccessful() {
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
        lock.lock();
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(1, lock.getHoldCount());
    }

    /**
     * 其他线程加锁成功, 当前线程加锁失败.
     * @throws InterruptedException
     */
    @Test
    public void testLockFail() throws InterruptedException {
        run(() -> getRedisLock().lock()).join();
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
        Assert.assertEquals(false, lock.tryLock());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
    }

    /**
     * 测试加锁成功后释放锁
     */
    @Test
    public void testReleaseLock() {
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
        lock.lock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(1, lock.getHoldCount());

        Assert.assertEquals(true, lock.releaseLock());
        Assert.assertEquals(false, lock.releaseLock());

        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
    }

    /**
     * 测试线程加锁成功, 非锁的持有者线程无法解锁.
     * @throws InterruptedException 
     */
    @Test
    public void testReleaseLockFail() throws InterruptedException {
        run(() -> getRedisLock().lock()).join();
        RedisLock lock = getRedisLock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(false, lock.releaseLock());
    }

    /**
     * 测试加锁与解锁分别使用的不同分布式锁实例, 仍然能解锁成功
     */
    @Test
    public void testAnotherReleaseLock() {
        Assert.assertEquals(true, getRedisLock().tryLock());

        RedisLock lock = getRedisLock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());

        Assert.assertEquals(true, lock.releaseLock());
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
    }

    /**
     * 测试加锁与解锁功能, 同时判断是否持有锁和加锁次数.
     * @throws InterruptedException
     */
    @Test
    public void testLockAndUnlock() throws InterruptedException {
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());

        lock.lock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(1, lock.getHoldCount());

        lock.lockInterruptibly();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(2, lock.getHoldCount());

        lock.lockTimed(1, TimeUnit.SECONDS);
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(3, lock.getHoldCount());

        Assert.assertEquals(true, lock.tryLock());
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(4, lock.getHoldCount());

        Assert.assertEquals(true, lock.tryLock(1, TimeUnit.SECONDS));
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(5, lock.getHoldCount());

        Assert.assertEquals(true, lock.tryLockTimed(1, TimeUnit.SECONDS));
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(6, lock.getHoldCount());

        lock.unlock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(5, lock.getHoldCount());

        Assert.assertEquals(true, lock.releaseLock());
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(4, lock.getHoldCount());

        lock.unlock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(3, lock.getHoldCount());

        lock.forceUnlock();
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
    }

    /**
     * 测试使用不同分布式锁加锁
     */
    @Test
    public void testMultipleLock() {
        RedisLock lock1 = getRedisLock("key1");
        RedisLock lock2 = getRedisLock("key2");
        RedisLock lock3 = getRedisLock("key3");

        lock1.forceUnlock();
        lock2.forceUnlock();
        lock3.forceUnlock();
        Assert.assertEquals(false, lock1.isLocked());
        Assert.assertEquals(false, lock1.isHeldLock());
        Assert.assertEquals(0, lock1.getHoldCount());
        Assert.assertEquals(false, lock2.isLocked());
        Assert.assertEquals(false, lock2.isHeldLock());
        Assert.assertEquals(0, lock2.getHoldCount());
        Assert.assertEquals(false, lock3.isLocked());
        Assert.assertEquals(false, lock3.isHeldLock());
        Assert.assertEquals(0, lock3.getHoldCount());

        lock1.lock();
        Assert.assertEquals(true, lock1.isLocked());
        Assert.assertEquals(true, lock1.isHeldLock());
        Assert.assertEquals(1, lock1.getHoldCount());
        Assert.assertEquals(false, lock2.isLocked());
        Assert.assertEquals(false, lock2.isHeldLock());
        Assert.assertEquals(0, lock2.getHoldCount());
        Assert.assertEquals(false, lock3.isLocked());
        Assert.assertEquals(false, lock3.isHeldLock());
        Assert.assertEquals(0, lock3.getHoldCount());

        lock2.lock();
        Assert.assertEquals(true, lock1.isLocked());
        Assert.assertEquals(true, lock1.isHeldLock());
        Assert.assertEquals(1, lock1.getHoldCount());
        Assert.assertEquals(true, lock2.isLocked());
        Assert.assertEquals(true, lock2.isHeldLock());
        Assert.assertEquals(1, lock2.getHoldCount());
        Assert.assertEquals(false, lock3.isLocked());
        Assert.assertEquals(false, lock3.isHeldLock());
        Assert.assertEquals(0, lock3.getHoldCount());

        lock2.lock();
        Assert.assertEquals(true, lock1.isLocked());
        Assert.assertEquals(true, lock1.isHeldLock());
        Assert.assertEquals(1, lock1.getHoldCount());
        Assert.assertEquals(true, lock2.isLocked());
        Assert.assertEquals(true, lock2.isHeldLock());
        Assert.assertEquals(2, lock2.getHoldCount());
        Assert.assertEquals(false, lock3.isLocked());
        Assert.assertEquals(false, lock3.isHeldLock());
        Assert.assertEquals(0, lock3.getHoldCount());

        lock3.lock();
        Assert.assertEquals(true, lock1.isLocked());
        Assert.assertEquals(true, lock1.isHeldLock());
        Assert.assertEquals(1, lock1.getHoldCount());
        Assert.assertEquals(true, lock2.isLocked());
        Assert.assertEquals(true, lock2.isHeldLock());
        Assert.assertEquals(2, lock2.getHoldCount());
        Assert.assertEquals(true, lock3.isLocked());
        Assert.assertEquals(true, lock3.isHeldLock());
        Assert.assertEquals(1, lock3.getHoldCount());

        lock1.unlock();
        Assert.assertEquals(false, lock1.isLocked());
        Assert.assertEquals(false, lock1.isHeldLock());
        Assert.assertEquals(0, lock1.getHoldCount());
        Assert.assertEquals(true, lock2.isLocked());
        Assert.assertEquals(true, lock2.isHeldLock());
        Assert.assertEquals(2, lock2.getHoldCount());
        Assert.assertEquals(true, lock3.isLocked());
        Assert.assertEquals(true, lock3.isHeldLock());
        Assert.assertEquals(1, lock3.getHoldCount());

        lock2.releaseLock();
        Assert.assertEquals(false, lock1.isLocked());
        Assert.assertEquals(false, lock1.isHeldLock());
        Assert.assertEquals(0, lock1.getHoldCount());
        Assert.assertEquals(true, lock2.isLocked());
        Assert.assertEquals(true, lock2.isHeldLock());
        Assert.assertEquals(1, lock2.getHoldCount());
        Assert.assertEquals(true, lock3.isLocked());
        Assert.assertEquals(true, lock3.isHeldLock());
        Assert.assertEquals(1, lock3.getHoldCount());

        lock3.unlock();
        Assert.assertEquals(false, lock1.isLocked());
        Assert.assertEquals(false, lock1.isHeldLock());
        Assert.assertEquals(0, lock1.getHoldCount());
        Assert.assertEquals(true, lock2.isLocked());
        Assert.assertEquals(true, lock2.isHeldLock());
        Assert.assertEquals(1, lock2.getHoldCount());
        Assert.assertEquals(false, lock3.isLocked());
        Assert.assertEquals(false, lock3.isHeldLock());
        Assert.assertEquals(0, lock3.getHoldCount());

        lock2.unlock();
        Assert.assertEquals(false, lock1.isLocked());
        Assert.assertEquals(false, lock1.isHeldLock());
        Assert.assertEquals(0, lock1.getHoldCount());
        Assert.assertEquals(false, lock2.isLocked());
        Assert.assertEquals(false, lock2.isHeldLock());
        Assert.assertEquals(0, lock2.getHoldCount());
        Assert.assertEquals(false, lock3.isLocked());
        Assert.assertEquals(false, lock3.isHeldLock());
        Assert.assertEquals(0, lock3.getHoldCount());
    }

    /**
     * 其他线程持有锁, 当前线程加锁超时失败.
     * @throws InterruptedException
     */
    @Test
    public void testTryLockTimeout() throws InterruptedException {
        run(() -> getRedisLock().lock()).join();
        RedisLock lock = getRedisLock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
        long startTime = System.currentTimeMillis();
        long waitTimeMillis = 1000L;
        Assert.assertEquals(false, lock.tryLock(waitTimeMillis, TimeUnit.MILLISECONDS));
        long time = System.currentTimeMillis() - startTime;
        Assert.assertEquals(true, waitTimeMillis - 10 <= time && time <= waitTimeMillis + 50);
    }

    /**
     * 其他线程持有锁, 当前线程使用过期模式加锁超时失败.
     * @throws InterruptedException
     */
    @Test
    public void testTryLockTimedTimeout() throws InterruptedException {
        run(() -> getRedisLock().lock()).join();
        RedisLock lock = getRedisLock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
        long startTime = System.currentTimeMillis();
        long waitTimeMillis = 2000L;
        Assert.assertEquals(false, lock.tryLockTimed(waitTimeMillis, 2000, TimeUnit.MILLISECONDS));
        long time = System.currentTimeMillis() - startTime;
        Assert.assertEquals(true, waitTimeMillis - 10 <= time && time <= waitTimeMillis + 50);
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
    }

    /**
     * 自身加锁并设置过期时间, 锁过期后不再持有锁.
     */
    @Test
    public void testLockExpire() {
        long leaseTimeMillis = 1000L;
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        lock.lockTimed(leaseTimeMillis, TimeUnit.MILLISECONDS);
        Assert.assertEquals(true, lock.isHeldLock());
        sleep(leaseTimeMillis + 300, TimeUnit.MILLISECONDS);
        Assert.assertEquals(false, lock.isHeldLock());
    }

    /**
     * 自身加锁并设置过期时间, 锁过期后解锁抛出异常.
     */
    @Test(expected = IllegalMonitorStateException.class)
    public void testLockExpireUnlockFail() {
        long leaseTimeMillis = 1000L;
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        lock.lockTimed(leaseTimeMillis, TimeUnit.MILLISECONDS);
        Assert.assertEquals(true, lock.isHeldLock());
        sleep(leaseTimeMillis + 300, TimeUnit.MILLISECONDS);
        Assert.assertEquals(false, lock.isHeldLock());
        lock.unlock();
    }

    /**
     * 测试尝试加锁并设置过期时间成功, 过期后不再持有锁.
     */
    @Test
    public void testTryLockExpire() {
        long leaseTimeMillis = 2000L;
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(true, lock.tryLockTimed(leaseTimeMillis, TimeUnit.MILLISECONDS));
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(1, lock.getHoldCount());
        sleep(leaseTimeMillis + 300, TimeUnit.MILLISECONDS);
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
    }

    /**
     * 其他线程首先持有锁并设置了过期时间, 当前线程在锁过期后加锁成功 
     * 这里测试时发现实际key的过期时间会比设置的略长...
     * @throws InterruptedException
     */
    @Test
    public void testTryLockAfterExpire() throws InterruptedException {
        long leaseTimeMillis = 4000L;
        run(() -> getRedisLock().lockTimed(leaseTimeMillis, TimeUnit.MILLISECONDS)).join();
        RedisLock lock = getRedisLock();
        long startTime = System.nanoTime();
        Assert.assertEquals(true, lock.tryLockTimed(leaseTimeMillis + 500, leaseTimeMillis, TimeUnit.MILLISECONDS));
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        Assert.assertEquals("actual cost time is " + time + "ms", true,
                leaseTimeMillis - 100 <= time && time <= leaseTimeMillis + 500);
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(1, lock.getHoldCount());
    }

    /**
     * 其他线程占有锁并设置了过期时间, 但等待超时小于过期时间,
     * 当前线程使用超时模式加锁失败.
     * @throws InterruptedException
     */
    @Test
    public void testTryLockAfterExpireFail() throws InterruptedException {
        long leaseTimeMillis = 1000L;
        RedisLock lock = getRedisLock();
        run(() -> lock.lockTimed(leaseTimeMillis * 2, TimeUnit.MILLISECONDS)).join();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        long startTime = System.currentTimeMillis();
        Assert.assertEquals(false, lock.tryLock(leaseTimeMillis, TimeUnit.MILLISECONDS));
        long time = System.currentTimeMillis() - startTime;
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
        Assert.assertEquals("actual cost time is " + time + "ms", true,
                leaseTimeMillis - 50 <= time && time <= leaseTimeMillis + 100);
    }

    /**
     * 其他线程占有锁, 当前线程使用可中断模式加锁,
     * 收到中断信号后抛出中断异常.
     * @throws InterruptedException
     */
    @Test(expected = InterruptedException.class)
    public void testLockInterruptibly() throws InterruptedException {
        Thread thisThread = Thread.currentThread();
        RedisLock lock = getRedisLock();
        run(() -> lock.lock()).join();
        run(() -> {
            sleep(500, TimeUnit.MILLISECONDS);
            thisThread.interrupt();
        });
        lock.lockInterruptibly();
    }

    /**
     * 其他线程占有锁, 当前线程使用超时等待模式加锁,
     * 在等待时间内收到中断信号后抛出中断异常.
     * @throws InterruptedException
     */
    @Test(expected = InterruptedException.class)
    public void testTryLockInterruptibly() throws InterruptedException {
        Thread thisThread = Thread.currentThread();
        RedisLock lock = getRedisLock();
        run(() -> lock.lock()).join();
        run(() -> {
            sleep(500, TimeUnit.MILLISECONDS);
            thisThread.interrupt();
        });
        lock.tryLock(5, TimeUnit.SECONDS);
    }

    /**
     * 测试可重入加锁
     * @throws InterruptedException
     */
    @Test
    public void testReentrantLock() throws InterruptedException {
        RedisLock lock = getRedisLock();
        lock.lock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(1, lock.getHoldCount());
        Thread t1 = run(() -> {
            lock.lock();
            // System.out.println("t1 lock");
            lock.unlock();
            // System.out.println("t1 unlock");
        });
        Thread t2 = run(() -> {
            lock.lock();
            // System.out.println("t2 lock");
            lock.unlock();
            // System.out.println("t2 unlock");
        });
        sleep(1);
        lock.lock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(2, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(1, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(0, lock.getHoldCount());
        t1.join();
        t2.join();
    }

    /**
     * 测试加锁后设置过期时间
     * @throws InterruptedException
     */
    @Test
    public void testRenewLeaseTime() throws InterruptedException {
        RedisLock lock = getRedisLock();

        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(false, lock.renewLeaseTime(4, TimeUnit.SECONDS));

        lock.lock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(true, lock.isHeldLock());
        Assert.assertEquals(true, lock.renewLeaseTime(4, TimeUnit.SECONDS));

        sleep(5);

        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        Assert.assertEquals(false, lock.renewLeaseTime(4, TimeUnit.SECONDS));
    }

    /**
     * 未有线程占有锁, 使用解锁抛出状态异常
     */
    @Test(expected = IllegalMonitorStateException.class)
    public void testUnlockFail() {
        RedisLock lock = getRedisLock();
        Assert.assertEquals(false, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        lock.unlock();
    }

    /**
     * 其他线程占有锁, 使用解锁抛出状态异常
     * @throws InterruptedException 
     */
    @Test(expected = IllegalMonitorStateException.class)
    public void testUnlockAnotherFail() throws InterruptedException {
        run(() -> getRedisLock().lock()).join();
        RedisLock lock = getRedisLock();
        Assert.assertEquals(true, lock.isLocked());
        Assert.assertEquals(false, lock.isHeldLock());
        lock.unlock();
    }

    /**
     * 测试自动回收无用共享队列
     */
    @Test
    public void testAutoCleanSharedQueue() {
        int i = 1;
        List<long[]> list = new LinkedList<>();
        while (i > 0) {
            list.add(new long[1024 * 1024]);
            i = RedisBasedLock.getSharedSyncCacheSize();
        }
        Assert.assertEquals(0, RedisBasedLock.getSharedSyncCacheSize());
        RedisBasedLock lock = RedisBasedLock.newSharedLock(key, new JedisClusterAdapter(getJedisCluster(20)));
        lock.lock();
        lock.unlock();
        Assert.assertEquals(1, RedisBasedLock.getSharedSyncCacheSize());
        lock = null;
        Assert.assertEquals(1, RedisBasedLock.getSharedSyncCacheSize());
        i = RedisBasedLock.getSharedSyncCacheSize();
        while (i > 0) {
            list.add(new long[1024 * 1024]);
            i = RedisBasedLock.getSharedSyncCacheSize();
        }
        Assert.assertEquals(0, RedisBasedLock.getSharedSyncCacheSize());
    }

}

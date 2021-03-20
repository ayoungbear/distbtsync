/**
 * Copyright 2021 yangzexiong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ayoungbear.distbtsync.redis.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 基于 redis 的分布式锁接口.
 * 
 * @author yangzexiong
 * @see java.util.concurrent.locks.Lock
 */
public interface RedisLock extends Lock {

    /**
     * 获取锁名称.
     * @return
     */
    String getLockName();

    /**
     * 加锁.
     */
    @Override
    void lock();

    /**
     * 可中断模式下加锁.
     */
    @Override
    void lockInterruptibly() throws InterruptedException;

    /**
     * 尝试加锁, 不设置过期时间.
     * @see java.util.concurrent.locks.Lock#tryLock()
     */
    @Override
    boolean tryLock();

    /**
     * 限时模式下加锁.
     * 
     * @see java.util.concurrent.locks.Lock#unlock()
     * @param time 超时时间
     * @param unit 时间单位
     * @return
     * @throws InterruptedException
     */
    @Override
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 解锁, 如果没有持有锁的话将抛出异常.
     * 
     * @see java.util.concurrent.locks.Lock#unlock()
     * @throws IllegalMonitorStateException
     */
    @Override
    void unlock() throws IllegalMonitorStateException;

    /**
     * 暂不支持.
     * @see java.util.concurrent.locks.Lock#newCondition()
     */
    @Override
    default Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    /**
     * 加锁并设置锁的有效期.
     * 
     * @param leaseTime 有效时间
     * @param unit 时间单位
     */
    void lockTimed(long leaseTime, TimeUnit unit);

    /**
     * 尝试加锁并设置锁的有效期.
     * 
     * @param leaseTime
     * @param unit
     * @return {@code true} 加锁成功
     */
    boolean tryLockTimed(long leaseTime, TimeUnit unit);

    /**
     * 限时模式下加锁并设置锁的有效期.
     * 
     * @param time 超时时间
     * @param leaseTime 有效时间
     * @param unit 时间单位
     * @return {@code true} 加锁成功
     * @throws InterruptedException
     */
    boolean tryLockTimed(long time, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * 重置锁的有效期, 只有锁的持有者能延长.
     * @param leaseTime 有效时间
     * @param unit 时间单位
     * @return
     */
    boolean renewLeaseTime(long leaseTime, TimeUnit unit);

    /**
     * 尝试解锁, 只有锁的持有者能解锁成功.
     * @return
     */
    boolean releaseLock();

    /**
     * 强制解锁.
     * @return
     */
    boolean forceUnlock();

    /**
     * 是否处于锁定状态.
     * @return
     */
    boolean isLocked();

    /**
     * 当前线程是否持有锁.
     * @return
     */
    boolean isHeldLock();

    /**
     * 当前线程持有该锁加锁的次数(用于可重入锁下获取加锁次数).
     * @return
     */
    default int getHoldCount() {
        throw new UnsupportedOperationException();
    }

}

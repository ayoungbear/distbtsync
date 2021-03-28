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
package com.ayoungbear.distbtsync.spring.redis;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import com.ayoungbear.distbtsync.redis.lock.RedisBasedLock;
import com.ayoungbear.distbtsync.redis.lock.RedisLock;
import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisLockOperation;

/**
 * 基于 {@link com.ayoungbear.distbtsync.redis.lock.RedisLock} 分布式锁实现的
 * 同步器提供者实现类.
 * 
 * @author yangzexiong
 */
public class RedisLockSynchronizerProvider implements RedisSynchronizerProvider {

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private Supplier<RedisLockCommands> commandsSupplier;

    public RedisLockSynchronizerProvider(RedisLockCommands commands) {
        Assert.notNull(commands, () -> "RedisLockCommands must not be null");
        this.commandsSupplier = SingletonSupplier.of(commands);
    }

    public RedisLockSynchronizerProvider(Supplier<RedisLockCommands> commandsSupplier) {
        Assert.notNull(commandsSupplier, () -> "RedisLockCommands supplier must not be null");
        this.commandsSupplier = commandsSupplier;
    }

    @Override
    public RedisSynchronizer getSynchronizer(RedisSyncAttribute attribute) {
        RedisLock lock = getRedisLock(attribute.getKey());
        RedisLockOperation lockOperation = determineLockOperation(attribute);
        return new RedisLockSynchronizer(lock, lockOperation);
    }

    /**
     * 根据给定的 {@code key} 键值, 返回相应的分布式锁对象.
     * @param key
     * @return
     */
    protected RedisLock getRedisLock(String key) {
        RedisLockCommands commands = commandsSupplier.get();
        if (commands == null) {
            throw new IllegalStateException("RedisLockCommands is required to create RedisLock");
        }
        return RedisBasedLock.newSharedLock(key, commands);
    }

    /**
     * 根据同步设置的相关信息决定加锁的实际操作和类型.
     * @param attribute
     * @return
     */
    protected RedisLockOperation determineLockOperation(RedisSyncAttribute attribute) {
        long leaseTime = attribute.getLeaseTimeMillis();
        long waitTime = attribute.getWaitTimeMillis();

        boolean expired = (leaseTime > 0);

        // 只尝试加锁一次
        boolean acquireOnce = (waitTime == 0);
        if (acquireOnce) {
            if (expired) {
                // 有设置过期时间
                return (lock) -> lock.tryLockTimed(leaseTime, TIME_UNIT);
            } else {
                // 无过期时间
                return RedisLock::tryLock;
            }
        }

        // 持续阻塞模式下加锁直到成功
        boolean blocking = waitTime < 0;
        if (blocking) {
            if (expired) {
                // 有设置过期时间
                return (lock) -> {
                    lock.lockTimed(leaseTime, TIME_UNIT);
                    return true;
                };
            } else {
                // 无过期时间
                return (lock) -> {
                    lock.lock();
                    return true;
                };
            }
        }

        // 阻塞等待超时模式下加锁, 超时或者被中断将失败退出
        if (expired) {
            // 有设置过期时间
            return (lock) -> {
                try {
                    return lock.tryLockTimed(waitTime, leaseTime, TIME_UNIT);
                } catch (InterruptedException e) {
                    return false;
                }
            };
        } else {
            // 无过期时间
            return (lock) -> {
                try {
                    return lock.tryLock(waitTime, TIME_UNIT);
                } catch (InterruptedException e) {
                    return false;
                }
            };
        }
    }

}

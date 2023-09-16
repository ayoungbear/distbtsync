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

package com.github.ayoungbear.distbtsync.spring.redis;

import com.github.ayoungbear.distbtsync.redis.lock.RedisLock;
import com.github.ayoungbear.distbtsync.redis.lock.RedisLockOperation;
import org.springframework.util.Assert;

/**
 * 基于 Redis 的同步器实现类, 使用分布式锁 {@link RedisLock} 和
 * 相应的加锁操作 {@link RedisLockOperation} 来实现互斥同步.
 *
 * @author yangzexiong
 * @see RedisLock
 * @see RedisLockOperation
 */
public class RedisLockSynchronizer implements RedisSynchronizer {

    private RedisLock lock;

    private RedisLockOperation lockOperation;

    public RedisLockSynchronizer(RedisLock lock, RedisLockOperation lockOperation) {
        Assert.notNull(lock, () -> "RedisLock must not be null");
        Assert.notNull(lockOperation, () -> "RedisLockOperation must not be null");
        this.lock = lock;
        this.lockOperation = lockOperation;
    }

    @Override
    public boolean acquire() {
        return lockOperation.doLock(lock);
    }

    @Override
    public boolean release() {
        return lock.releaseLock();
    }

    @Override
    public boolean isHeld() {
        return lock.isHeldLock();
    }

    @Override
    public String getKey() {
        return lock.getLockName();
    }

    public RedisLock getRedisLock() {
        return lock;
    }

}

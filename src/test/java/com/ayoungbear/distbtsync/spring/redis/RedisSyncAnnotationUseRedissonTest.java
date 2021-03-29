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

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.ayoungbear.distbtsync.BaseRedisTest;

/**
 * 使用 redisson 测试分布式同步注解, 作为对照比较性能.
 * 
 * @author yangzexiong
 */
public class RedisSyncAnnotationUseRedissonTest extends BaseRedisSyncAnnotationTest {

    /**
     * spring测试环境配置, 启动redis同步注解功能.
     * 测试使用基于 redisson 的同步器, 对比性能用.
     * 
     * @author yangzexiong
     */
    @Configuration
    @EnableRedisSync
    public static class RedisSyncAnnotationTestConfiguration extends BaseRedisSyncAnnotationTestConfiguration
            implements RedisSyncConfigurer {

        @Override
        public RedisSynchronizerProvider getRedisSynchronizerProvider() {
            return new RedissonSynchronizerProvider(BaseRedisTest.getRedissonClient(2000));
        }

    }

    /**
     * 基于 redisson 实现的分布式同步器提供者实现类.
     * 
     * @author yangzexiong
     */
    public static class RedissonSynchronizerProvider implements RedisSynchronizerProvider {

        private RedissonClient redissonClient;

        public RedissonSynchronizerProvider(RedissonClient redissonClient) {
            this.redissonClient = redissonClient;
        }

        @Override
        public RedisSynchronizer getSynchronizer(RedisSyncAttributes attributes) {
            return new RedissonSynchronizer(redissonClient.getLock(attributes.getKey()), attributes.getWaitTimeMillis(),
                    attributes.getLeaseTimeMillis());
        }

    }

    /**
     * 基于 redisson 实现的分布式同步器, 对照测试用, 简单实现.
     * 
     * @author yangzexiong
     */
    public static class RedissonSynchronizer implements RedisSynchronizer {
        private static final Logger logger = LoggerFactory.getLogger(RedissonSynchronizer.class);

        private RLock lock;
        private long waitTime;
        private long leaseTime;

        public RedissonSynchronizer(RLock lock, long waitTime, long leaseTime) {
            this.lock = lock;
            this.waitTime = waitTime;
            this.leaseTime = leaseTime;
        }

        @Override
        public boolean acquire() {
            boolean result = doAcquire();
            logger.info("Acquire key '{}'", getKey());
            return result;
        }

        private boolean doAcquire() {
            boolean expired = (leaseTime > 0);

            // 只尝试加锁一次
            boolean acquireOnce = (waitTime == 0);
            if (acquireOnce) {
                if (expired) {
                    // 有设置过期时间
                    try {
                        return lock.tryLock(0, leaseTime, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        return false;
                    }
                } else {
                    // 无过期时间
                    return lock.tryLock();
                }
            }

            // 持续阻塞模式下加锁直到成功
            boolean blocking = waitTime < 0;
            if (blocking) {
                if (expired) {
                    // 有设置过期时间
                    lock.lock(leaseTime, TimeUnit.MILLISECONDS);
                    return true;
                } else {
                    // 无过期时间
                    lock.lock();
                    return true;
                }
            }

            // 阻塞等待超时模式下加锁, 超时或者被中断将失败退出
            if (expired) {
                // 有设置过期时间
                try {
                    return lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return false;
                }
            } else {
                // 无过期时间
                try {
                    return lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }

        @Override
        public boolean release() {
            logger.info("Release key '{}'", getKey());
            lock.unlock();
            return true;
        }

        @Override
        public boolean isHeld() {
            return lock.isHeldByCurrentThread();
        }

        @Override
        public String getKey() {
            return lock.getName();
        }

    }

}

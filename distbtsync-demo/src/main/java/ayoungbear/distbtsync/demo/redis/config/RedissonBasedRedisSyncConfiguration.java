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
package ayoungbear.distbtsync.demo.redis.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import com.ayoungbear.distbtsync.spring.redis.RedisSyncAttributes;
import com.ayoungbear.distbtsync.spring.redis.RedisSyncConfigurer;
import com.ayoungbear.distbtsync.spring.redis.RedisSynchronizer;
import com.ayoungbear.distbtsync.spring.redis.RedisSynchronizerProvider;

/**
 * 基于 redisson 客户端实现的自定义同步器配置类.
 * 
 * @author yangzexiong
 */
@Configuration
@ConditionalOnProperty(prefix = "ayoungbear.distbtsync.spring.redis.custom", name = "synchronizer", havingValue = "redisson", matchIfMissing = false)
public class RedissonBasedRedisSyncConfiguration implements RedisSyncConfigurer {

    @Value("${spring.redis.cluster.nodes}")
    private String[] nodes;
    @Value("${redisson.pool.size:100}")
    private Integer size;

    @Override
    public RedisSynchronizerProvider getRedisSynchronizerProvider() {
        Config config = new Config();
        ClusterServersConfig clustConfig = config.useClusterServers()
                .addNodeAddress(Arrays.asList(nodes).stream().map((nodeAddress) -> "redis://" + nodeAddress.toString())
                        .collect(Collectors.toList()).toArray(new String[0]));
        clustConfig.setSubscriptionConnectionPoolSize(size);
        clustConfig.setMasterConnectionPoolSize(size);
        clustConfig.setSlaveConnectionPoolSize(size);
        RedissonClient redissonClient = Redisson.create(config);
        return new RedissonBasedSynchronizerProvider(redissonClient);
    }

    /**
     * 基于 redisson 实现的分布式同步器提供者实现类.
     * 
     * @author yangzexiong
     */
    public static class RedissonBasedSynchronizerProvider implements RedisSynchronizerProvider {

        private RedissonClient redissonClient;

        public RedissonBasedSynchronizerProvider(RedissonClient redissonClient) {
            this.redissonClient = redissonClient;
        }

        @Override
        public RedisSynchronizer getSynchronizer(RedisSyncAttributes attributes) {
            // 这里使用非公平锁
            RLock lock = redissonClient.getLock(attributes.getKey());
            return new RedissonBasedSynchronizer(lock, attributes.getWaitTimeMillis(), attributes.getLeaseTimeMillis());
        }

    }

    /**
     * 基于 redisson 实现的分布式同步器.
     * 
     * @author yangzexiong
     */
    public static class RedissonBasedSynchronizer implements RedisSynchronizer {
        private static final Logger logger = LoggerFactory.getLogger(RedissonBasedSynchronizer.class);

        private RLock lock;
        private long waitTime;
        private long leaseTime;

        public RedissonBasedSynchronizer(RLock lock, long waitTime, long leaseTime) {
            this.lock = lock;
            this.waitTime = waitTime;
            this.leaseTime = leaseTime;
        }

        @Override
        public boolean acquire() {
            boolean result = doAcquire();
            logger.info("Acquire use redisson key '{}'", getKey());
            return result;
        }

        @Override
        public boolean release() {
            logger.info("Release use redisson key '{}'", getKey());
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

    }

}

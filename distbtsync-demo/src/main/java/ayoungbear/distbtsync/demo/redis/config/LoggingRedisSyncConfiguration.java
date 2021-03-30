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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.ayoungbear.distbtsync.redis.lock.RedisLock;
import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisLockOperation;
import com.ayoungbear.distbtsync.redis.lock.support.RedisConnectionCommandsAdapter;
import com.ayoungbear.distbtsync.spring.redis.RedisLockSynchronizer;
import com.ayoungbear.distbtsync.spring.redis.RedisLockSynchronizerProvider;
import com.ayoungbear.distbtsync.spring.redis.RedisSyncAttributes;
import com.ayoungbear.distbtsync.spring.redis.RedisSyncConfigurer;
import com.ayoungbear.distbtsync.spring.redis.RedisSynchronizer;
import com.ayoungbear.distbtsync.spring.redis.RedisSynchronizerProvider;

/**
 * 自定义同步器配置类, 在同步执行前后添加日志方便观察.
 * 
 * @author yangzexiong
 */
@Configuration
@ConditionalOnProperty(prefix = "ayoungbear.distbtsync.spring.redis.custom", name = "synchronizer", havingValue = "log", matchIfMissing = false)
public class LoggingRedisSyncConfiguration implements RedisSyncConfigurer {

    private RedisConnectionFactory redisConnectionFactory;

    public LoggingRedisSyncConfiguration(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public RedisSynchronizerProvider getRedisSynchronizerProvider() {
        return new LoggingRedisSynchronizerProvider(new RedisConnectionCommandsAdapter(redisConnectionFactory));
    }

    /**
     * 自定义的基于 Redis 的同步器实现类, 在同步前后增加日志方便观察.
     * 
     * @author yangzexiong
     */
    public static class LoggingRedisSynchronizer extends RedisLockSynchronizer {
        private static final Logger logger = LoggerFactory.getLogger(LoggingRedisSynchronizer.class);

        public LoggingRedisSynchronizer(RedisLock lock, RedisLockOperation operation) {
            super(lock, operation);
        }

        @Override
        public boolean acquire() {
            boolean result = super.acquire();
            logger.info("Acquire key '{}'", getKey());
            return result;
        }

        @Override
        public boolean release() {
            logger.info("Release key '{}'", getKey());
            return super.release();
        }
    }

    /**
     * 自定义分布式日志记录同步器提供实现类, 在同步前后增加日志方便观察.
     * 
     * @author yangzexiong
     */
    public static class LoggingRedisSynchronizerProvider extends RedisLockSynchronizerProvider {

        public LoggingRedisSynchronizerProvider(RedisLockCommands commands) {
            super(commands);
        }

        @Override
        public RedisSynchronizer getSynchronizer(RedisSyncAttributes attributes) {
            RedisLock lock = getRedisLock(attributes.getKey());
            RedisLockOperation lockOperation = determineLockOperation(attributes);
            return new LoggingRedisSynchronizer(lock, lockOperation);
        }

    }

}

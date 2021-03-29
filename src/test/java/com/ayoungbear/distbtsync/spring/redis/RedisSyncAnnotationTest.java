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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.ayoungbear.distbtsync.redis.lock.support.RedisConnectionCommandsAdapter;

/**
 * 测试分布式同步注解.
 * 
 * @author yangzexiong
 */
public class RedisSyncAnnotationTest extends BaseRedisSyncAnnotationTest {

    /**
     * spring测试环境配置, 启动redis同步注解功能.
     * 测试使用我们自定义的同步器, 方便通过日志观察同步情况和使用的key.
     * 
     * @author yangzexiong
     */
    @Configuration
    @EnableRedisSync
    public static class RedisSyncAnnotationTestConfiguration extends BaseRedisSyncAnnotationTestConfiguration
            implements RedisSyncConfigurer {

        @Autowired
        private RedisConnectionFactory redisConnectionFactory;

        @Override
        public RedisSynchronizerProvider getRedisSynchronizerProvider() {
            return new MyRedisSynchronizerProvider(new RedisConnectionCommandsAdapter(redisConnectionFactory));
        }

    }

}

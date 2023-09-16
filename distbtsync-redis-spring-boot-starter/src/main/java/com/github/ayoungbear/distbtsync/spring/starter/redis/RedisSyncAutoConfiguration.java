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

package com.github.ayoungbear.distbtsync.spring.starter.redis;

import com.github.ayoungbear.distbtsync.spring.redis.EnableRedisSync;
import com.github.ayoungbear.distbtsync.spring.redis.RedisSync;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 基于 Redis 的分布式同步注解 {@link RedisSync} 自动配置类.
 *
 * @author yangzexiong
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnProperty(prefix = "ayoungbear.distbtsync.spring.redis", name = "auto", havingValue = "true", matchIfMissing = true)
public class RedisSyncAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableRedisSync(proxyTargetClass = false)
    @ConditionalOnProperty(prefix = "ayoungbear.distbtsync.spring.redis", name = "proxy-target-class", havingValue = "false", matchIfMissing = false)
    static class RedisSyncJdkDynamicAutoProxyConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @EnableRedisSync(proxyTargetClass = true)
    @ConditionalOnProperty(prefix = "ayoungbear.distbtsync.spring.redis", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
    static class RedisSyncCglibAutoProxyConfiguration {

    }

}

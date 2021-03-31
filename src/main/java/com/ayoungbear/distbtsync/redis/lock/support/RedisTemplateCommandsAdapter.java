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
package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Collections;
import java.util.Objects;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.sub.MessageConsumer;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisConnectionSubscription;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;

/**
 * 用 {@link org.springframework.data.redis.core.RedisTemplate} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see org.springframework.data.redis.core.RedisTemplate
 */
public class RedisTemplateCommandsAdapter implements RedisLockCommands {

    private StringRedisTemplate redisTemplate;

    private RedisConnectionFactory connectionFactory;

    private StringRedisSerializer serializer = new StringRedisSerializer();

    public RedisTemplateCommandsAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "RedisTemplate must not be null");
        this.connectionFactory = Objects.requireNonNull(redisTemplate.getConnectionFactory(),
                "RedisConnectionFactory is required");
    }

    @Override
    public String eval(String script, String key, String... args) {
        RedisScript<String> redisScript = new DefaultRedisScript<String>(script, String.class);
        Object[] argsObj = args;
        String result = redisTemplate.execute(redisScript, serializer, serializer, Collections.singletonList(key),
                argsObj);
        return result;
    }

    @Override
    public RedisSubscription getSubscription(String channel, MessageConsumer<String> messageConsumer) {
        return new RedisConnectionSubscription(() -> connectionFactory.getConnection(), channel, messageConsumer);
    }

}

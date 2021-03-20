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
package com.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import java.util.function.Consumer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 基于 {@link redis.clients.jedis.JedisPool} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class JedisPoolSubscription extends AbstractJedisSubscription implements RedisSubscription {

    private JedisPool jedisPool;

    public JedisPoolSubscription(JedisPool jedisPool, String channel, Consumer<String> onMessageRun) {
        super(channel, onMessageRun);
        this.jedisPool = Objects.requireNonNull(jedisPool, "JedisPool must not be null");
    }

    @Override
    public void subscribe() {
        if (!isSubscribed()) {
            Jedis jedis = jedisPool.getResource();
            String channel = getChannel();
            try {
                jedis.subscribe(this, channel);
            } finally {
                jedis.close();
            }
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

}

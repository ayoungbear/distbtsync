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

package com.github.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import redis.clients.jedis.JedisCluster;

/**
 * 基于 {@link redis.clients.jedis.JedisCluster} 实现的 redis 订阅者.
 *
 * @author yangzexiong
 * @see RedisSubscription
 */
public class JedisClusterSubscription extends AbstractJedisSubscription implements RedisSubscription {

    private JedisCluster jedisCluster;

    public JedisClusterSubscription(JedisCluster jedisCluster, String channel,
            MessageConsumer<String> messageConsumer) {
        super(channel, messageConsumer);
        this.jedisCluster = Objects.requireNonNull(jedisCluster, "JedisCluster must not be null");
    }

    @Override
    public void subscribe() {
        if (!isSubscribed()) {
            String channel = getChannel();
            jedisCluster.subscribe(this, channel);
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

}

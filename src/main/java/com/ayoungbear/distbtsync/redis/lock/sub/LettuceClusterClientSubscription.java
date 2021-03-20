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

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * 基于 {@link io.lettuce.core.cluster.RedisClusterClient} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class LettuceClusterClientSubscription extends AbstractLettuceClientSubscription implements RedisSubscription {

    private RedisClusterClient client;

    public LettuceClusterClientSubscription(RedisClusterClient client, String channel, Consumer<String> onMessageRun) {
        super(channel, onMessageRun);
        this.client = Objects.requireNonNull(client, "RedisClusterClient must not be null");
    }

    @Override
    protected StatefulRedisPubSubConnection<String, String> providePubSubConnection() {
        return client.connectPubSub();
    }

}

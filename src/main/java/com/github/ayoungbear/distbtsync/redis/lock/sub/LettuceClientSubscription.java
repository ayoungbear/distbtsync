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

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.util.Objects;

/**
 * 基于 {@link io.lettuce.core.RedisClient} 实现的 redis 订阅者.
 *
 * @author yangzexiong
 * @see RedisSubscription
 */
public class LettuceClientSubscription extends AbstractLettuceClientSubscription implements RedisSubscription {

    private RedisClient client;

    public LettuceClientSubscription(RedisClient client, String channel, MessageConsumer<String> messageConsumer) {
        super(channel, messageConsumer);
        this.client = Objects.requireNonNull(client, "RedisClient must not be null");
    }

    @Override
    protected StatefulRedisPubSubConnection<String, String> providePubSubConnection() {
        return client.connectPubSub();
    }

}

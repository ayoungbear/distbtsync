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

import redis.clients.jedis.JedisPubSub;

/**
 * 基于 {@link redis.clients.jedis.JedisPubSub} 实现的 redis 订阅者基类.
 * 
 * @author yangzexiong
 */
public abstract class AbstractJedisSubscription extends JedisPubSub implements RedisSubscription {

    private final String channel;

    private MessageConsumer<String> messageConsumer;

    protected AbstractJedisSubscription(String channel, MessageConsumer<String> messageConsumer) {
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void unsubscribe() {
        if (isSubscribed()) {
            super.unsubscribe();
        }
    }

    @Override
    public boolean isSubscribed() {
        return super.isSubscribed();
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (messageConsumer != null) {
            messageConsumer.consume(message);
        }
    }

    public void setMessageConsumer(MessageConsumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

}

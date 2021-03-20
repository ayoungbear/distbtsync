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

import redis.clients.jedis.JedisPubSub;

/**
 * 基于 {@link redis.clients.jedis.JedisPubSub} 实现的 redis 订阅者基类.
 * 
 * @author yangzexiong
 */
public abstract class AbstractJedisSubscription extends JedisPubSub implements RedisSubscription {

    private final String channel;

    private Consumer<String> onMessageRun;

    protected AbstractJedisSubscription(String channel, Consumer<String> onMessageRun) {
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.onMessageRun = onMessageRun;
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
        if (onMessageRun != null) {
            onMessageRun.accept(message);
        }
    }

    public void setOnMessageRun(Consumer<String> onMessageRun) {
        this.onMessageRun = onMessageRun;
    }

}

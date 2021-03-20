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
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 基于 {@link org.springframework.data.redis.connection.RedisConnection} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class RedisConnectionSubscription implements RedisSubscription, MessageListener {

    private final String channel;

    private Supplier<RedisConnection> redisConnectionSupplier;

    private RedisConnection subRedisConnection;

    private Consumer<String> onMessageRun;

    private Semaphore latch = new Semaphore(0);

    private StringRedisSerializer serializer = new StringRedisSerializer();

    public RedisConnectionSubscription(Supplier<RedisConnection> redisConnectionSupplier, String channel,
            Consumer<String> onMessageRun) {
        this.redisConnectionSupplier = Objects.requireNonNull(redisConnectionSupplier,
                "RedisConnectionSupplier must not be null");
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.onMessageRun = onMessageRun;
    }

    @Override
    public void subscribe() {
        if (!isSubscribed()) {
            // RedisConnection 根据实现类不同, 订阅方法可能是异步的, 比如 LettuceConnection
            getRedisConnection().subscribe(this, serializer.serialize(channel));
            // 如果是异步订阅, 那么订阅线程在此阻塞, 等待解除订阅
            latch.acquireUninterruptibly();
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

    @Override
    public void unsubscribe() {
        if (isSubscribed()) {
            try {
                Subscription subscription = getRedisConnection().getSubscription();
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            } finally {
                latch.release();
            }
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (onMessageRun != null) {
            String messageStr = serializer.deserialize(message.getBody());
            onMessageRun.accept(messageStr);
        }
    }

    @Override
    public boolean isSubscribed() {
        return getRedisConnection().isSubscribed();
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public void close() {
        if (subRedisConnection != null) {
            RedisConnection redisConnection = this.subRedisConnection;
            subRedisConnection = null;
            redisConnection.close();
        }
    }

    public void setOnMessageRun(Consumer<String> onMessageRun) {
        this.onMessageRun = onMessageRun;
    }

    private RedisConnection getRedisConnection() {
        if (subRedisConnection == null) {
            subRedisConnection = redisConnectionSupplier.get();
            if (subRedisConnection == null) {
                throw new NullPointerException();
            }
        }
        return subRedisConnection;
    }

}

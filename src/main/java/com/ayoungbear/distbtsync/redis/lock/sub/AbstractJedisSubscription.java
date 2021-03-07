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

    protected AbstractJedisSubscription(String channel, Consumer<String> onMessageRun) {
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.onMessageRun = onMessageRun;
    }

    private final String channel;

    private Consumer<String> onMessageRun;

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

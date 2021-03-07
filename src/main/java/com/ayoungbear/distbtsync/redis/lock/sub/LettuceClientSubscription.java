package com.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import java.util.function.Consumer;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * 基于 {@link io.lettuce.core.RedisClient} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class LettuceClientSubscription extends AbstractLettuceClientSubscription implements RedisSubscription {

    private final RedisClient client;

    public LettuceClientSubscription(RedisClient client, String channel, Consumer<String> onMessageRun) {
        super(channel, onMessageRun);
        this.client = Objects.requireNonNull(client, "RedisClient must not be null");
    }

    @Override
    protected StatefulRedisPubSubConnection<String, String> providePubSubConnection() {
        return client.connectPubSub();
    }

}

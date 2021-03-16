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

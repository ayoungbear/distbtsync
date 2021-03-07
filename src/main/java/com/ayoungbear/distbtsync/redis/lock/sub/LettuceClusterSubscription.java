package com.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.sync.RedisClusterPubSubCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

/**
 * 基于 {@link io.lettuce.core.cluster.RedisClusterClient} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class LettuceClusterSubscription extends RedisPubSubAdapter<String, String> implements RedisSubscription {

    private final RedisClusterClient client;

    private final String channel;

    private Consumer<String> onMessageRun;

    private StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;

    private Semaphore latch = new Semaphore(0);

    private volatile boolean isSubscribed = false;

    public LettuceClusterSubscription(RedisClusterClient client, String channel, Consumer<String> onMessageRun) {
        this.client = Objects.requireNonNull(client, "RedisClusterClient must not be null");
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.onMessageRun = onMessageRun;
    }

    @Override
    public void subscribe() {
        RedisClusterPubSubCommands<String, String> commands = getCommands();
        if (!isSubscribed()) {
            isSubscribed = true;
            commands.subscribe(channel);
            latch.acquireUninterruptibly();
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

    @Override
    public void unsubscribe() {
        try {
            RedisClusterPubSubCommands<String, String> commands = getCommands();
            commands.unsubscribe(channel);
            isSubscribed = false;
        } finally {
            release();
        }
    }

    @Override
    public boolean isSubscribed() {
        return isSubscribed;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public void message(String channel, String message) {
        if (onMessageRun != null) {
            onMessageRun.accept(message);
        }
    }

    public void setOnMessageRun(Consumer<String> onMessageRun) {
        this.onMessageRun = onMessageRun;
    }

    private RedisClusterPubSubCommands<String, String> getCommands() {
        if (pubSubConnection == null) {
            StatefulRedisClusterPubSubConnection<String, String> pubSubConnection = client.connectPubSub();
            pubSubConnection.addListener(this);
            this.pubSubConnection = pubSubConnection;
        }
        return pubSubConnection.sync();
    }

    private void release() {
        latch.release();
        if (pubSubConnection != null) {
            StatefulRedisClusterPubSubConnection<String, String> connection = this.pubSubConnection;
            this.pubSubConnection = null;
            connection.close();
        }
    }

}

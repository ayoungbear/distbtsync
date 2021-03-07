package com.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

/**
 * 基于 {@link io.lettuce.core.pubsub.RedisPubSubListener} 实现的 redis 订阅者基类.
 * 
 * @author yangzexiong
 */
public abstract class AbstractLettuceClientSubscription extends RedisPubSubAdapter<String, String>
        implements RedisSubscription {

    private final String channel;

    private Consumer<String> onMessageRun;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private Semaphore latch = new Semaphore(0);

    private volatile boolean isSubscribed = false;

    protected AbstractLettuceClientSubscription(String channel, Consumer<String> onMessageRun) {
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.onMessageRun = onMessageRun;
    }

    @Override
    public void subscribe() {
        RedisPubSubCommands<String, String> commands = getCommands();
        if (!isSubscribed()) {
            isSubscribed = true;
            commands.subscribe(channel);
            // 异步订阅, 在此阻塞
            latch.acquireUninterruptibly();
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

    @Override
    public void unsubscribe() {
        try {
            RedisPubSubCommands<String, String> commands = getCommands();
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

    protected RedisPubSubCommands<String, String> getCommands() {
        StatefulRedisPubSubConnection<String, String> pubSubConnection = getPubSubConnection();
        pubSubConnection.addListener(this);
        return pubSubConnection.sync();
    }

    /**
     * 提供 {@link io.lettuce.core.pubsub.StatefulRedisPubSubConnection} 连接
     * @return
     */
    protected abstract StatefulRedisPubSubConnection<String, String> providePubSubConnection();

    /**
     * 去掉订阅后释放拦截器并关闭连接
     */
    private void release() {
        latch.release();
        if (pubSubConnection != null) {
            StatefulRedisPubSubConnection<String, String> connection = this.pubSubConnection;
            this.pubSubConnection = null;
            connection.close();
        }
    }

    private StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        if (pubSubConnection == null) {
            StatefulRedisPubSubConnection<String, String> pubSubConnection = providePubSubConnection();
            pubSubConnection.addListener(this);
            this.pubSubConnection = pubSubConnection;
        }
        return pubSubConnection;
    }

}
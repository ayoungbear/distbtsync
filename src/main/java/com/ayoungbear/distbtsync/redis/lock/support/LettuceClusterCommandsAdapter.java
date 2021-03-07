package com.ayoungbear.distbtsync.redis.lock.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisSubscription;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.sync.RedisClusterPubSubCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

/**
 * 用 {@link io.lettuce.core.cluster.RedisClusterClient} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see io.lettuce.core.cluster.RedisClusterClient
 */
public class LettuceClusterCommandsAdapter implements RedisLockCommands, Closeable {

    private RedisClusterClient client;

    private volatile StatefulRedisClusterConnection<String, String> connection;

    public LettuceClusterCommandsAdapter(RedisClusterClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        StatefulRedisClusterConnection<String, String> connection = getConnection();
        RedisAdvancedClusterCommands<String, String> commands = connection.sync();
        String result = commands.eval(script, ScriptOutputType.VALUE, new String[] { key }, args);
        return result;
    }

    @Override
    public RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun) {
        return new LettuceClusterSubscription(client, channel, onMessageRun);
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    private StatefulRedisClusterConnection<String, String> getConnection() {
        if (connection == null) {
            synchronized (this) {
                if (connection == null) {
                    connection = client.connect();
                }
            }
        }
        return connection;
    }

    /**
     * 基于 {@link io.lettuce.core.cluster.RedisClusterClient} 实现的 redis 订阅者.
     * 
     * @author yangzexiong
     * @see RedisSubscription
     */
    static class LettuceClusterSubscription extends RedisPubSubAdapter<String, String> implements RedisSubscription {

        private final RedisClusterClient client;

        private final String channel;

        private Consumer<String> onMessageRun;

        private StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;

        private Semaphore latch = new Semaphore(0);

        private volatile boolean isSubscribed = false;

        public LettuceClusterSubscription(RedisClusterClient client, String channel, Consumer<String> onMessageRun) {
            this.client = Objects.requireNonNull(client, "client must not be null");
            this.channel = Objects.requireNonNull(channel, "channel must not be null");
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

}

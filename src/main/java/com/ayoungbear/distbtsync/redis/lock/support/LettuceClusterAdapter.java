package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;

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
public class LettuceClusterAdapter implements RedisLockCommands {

    private RedisClusterClient client;

    private RedisClusterSubListener subListener;

    private volatile StatefulRedisClusterConnection<String, String> connection;

    public LettuceClusterAdapter(RedisClusterClient client) {
        this.client = Objects.requireNonNull(client, "RedisClusterClient must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        StatefulRedisClusterConnection<String, String> connection = getConnection();
        RedisAdvancedClusterCommands<String, String> commands = connection.sync();
        String result = commands.eval(script, ScriptOutputType.VALUE, new String[] { key }, args);
        return result;
    }

    @Override
    public void subscribe(String channel, Consumer<String> onMessageRun) {
        if (subListener == null) {
            subListener = new RedisClusterSubListener(client, channel, onMessageRun);
        }
        subListener.subscribed();
    }

    @Override
    public void unsubscribe() {
        if (subListener != null && subListener.isSubscribed()) {
            subListener.unsubscribed();
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
     * 订阅消息监听消费者
     * 
     * @author yangzexiong
     */
    static class RedisClusterSubListener extends RedisPubSubAdapter<String, String> {

        private RedisClusterClient client;

        private String channel;

        private Consumer<String> onMessageRun;

        private RedisClusterPubSubCommands<String, String> pubSubCommands;

        private Semaphore latch = new Semaphore(0);

        private boolean isSubscribed = false;

        public RedisClusterSubListener(RedisClusterClient client, String channel, Consumer<String> onMessageRun) {
            this.client = Objects.requireNonNull(client, "client must not be null");
            this.channel = Objects.requireNonNull(channel, "channel must not be null");
            this.onMessageRun = Objects.requireNonNull(onMessageRun, "onMessageRun must not be null");
        }

        @Override
        public void message(String channel, String message) {
            if (this.channel.equals(channel)) {
                onMessageRun.accept(message);
            }
        }

        public void subscribed() {
            RedisClusterPubSubCommands<String, String> commands = getCommands();
            if (!isSubscribed()) {
                isSubscribed = true;
                commands.subscribe(channel);
                latch.acquireUninterruptibly();
            } else {
                throw new IllegalMonitorStateException("Already in a subscription");
            }
        }

        public void unsubscribed() {
            try {
                RedisClusterPubSubCommands<String, String> commands = getCommands();
                commands.unsubscribe(channel);
                isSubscribed = false;
            } finally {
                latch.release();
            }
        }

        public boolean isSubscribed() {
            return isSubscribed;
        }

        public String getChannel() {
            return channel;
        }

        private RedisClusterPubSubCommands<String, String> getCommands() {
            if (pubSubCommands == null) {
                StatefulRedisClusterPubSubConnection<String, String> pubSubConnection = client.connectPubSub();
                pubSubConnection.addListener(this);
                pubSubCommands = pubSubConnection.sync();
            }
            return pubSubCommands;
        }

    }

}

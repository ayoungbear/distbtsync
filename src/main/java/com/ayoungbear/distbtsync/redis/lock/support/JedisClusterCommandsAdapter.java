package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisSubscription;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

/**
 * 用 {@link redis.clients.jedis.JedisCluster} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see redis.clients.jedis.JedisCluster
 */
public class JedisClusterCommandsAdapter implements RedisLockCommands {

    private final JedisCluster jedisCluster;

    public JedisClusterCommandsAdapter(JedisCluster jedisCluster) {
        this.jedisCluster = Objects.requireNonNull(jedisCluster, "JedisCluster must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        Object result = jedisCluster.eval(script, 1, mergeParams(key, args));
        return result == null ? null : String.valueOf(result);
    }

    @Override
    public RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun) {
        return new JedisClusterSubscription(jedisCluster, channel, onMessageRun);
    }

    protected String[] mergeParams(String key, String... args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = key;
        if (args.length > 0) {
            System.arraycopy(args, 0, newArgs, 1, args.length);
        }
        return newArgs;
    }

    /**
     * 基于 {@link redis.clients.jedis.JedisCluster} 实现的 redis 订阅者.
     * 
     * @author yangzexiong
     * @see RedisSubscription
     */
    public static class JedisClusterSubscription extends JedisPubSub implements RedisSubscription {

        public JedisClusterSubscription(JedisCluster jedisCluster, String channel,
                Consumer<String> onMessageRun) {
            this.jedisCluster = Objects.requireNonNull(jedisCluster, "JedisCluster must not be null");
            this.channel = Objects.requireNonNull(channel, "Channel must not be null");
            this.onMessageRun = onMessageRun;
        }

        private final JedisCluster jedisCluster;

        private final String channel;

        private Consumer<String> onMessageRun;

        @Override
        public void subscribe() {
            if (!isSubscribed()) {
                jedisCluster.subscribe(this, channel);
            } else {
                throw new IllegalMonitorStateException("Already in a subscription");
            }
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

}

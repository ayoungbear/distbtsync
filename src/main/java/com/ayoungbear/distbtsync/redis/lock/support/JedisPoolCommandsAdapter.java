package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisSubscription;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * 用 {@link redis.clients.jedis.JedisPool} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see redis.clients.jedis.JedisPool
 */
public class JedisPoolCommandsAdapter implements RedisLockCommands {

    private final JedisPool jedisPool;

    public JedisPoolCommandsAdapter(JedisPool jedisPool) {
        this.jedisPool = Objects.requireNonNull(jedisPool, "JedisPool must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        Jedis jedis = jedisPool.getResource();
        try {
            return String.valueOf(jedis.eval(script, 1, mergeParams(key, args)));
        } finally {
            jedis.close();
        }
    }

    @Override
    public RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun) {
        return new JedisPoolSubscription(jedisPool, channel, onMessageRun);
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
     * 基于 {@link redis.clients.jedis.JedisPool} 实现的 redis 订阅者.
     * 
     * @author yangzexiong
     * @see RedisSubscription
     */
    public static class JedisPoolSubscription extends JedisPubSub implements RedisSubscription {

        public JedisPoolSubscription(JedisPool jedisPool, String channel, Consumer<String> onMessageRun) {
            this.jedisPool = Objects.requireNonNull(jedisPool, "Jedis must not be null");
            this.channel = Objects.requireNonNull(channel, "Channel must not be null");
            this.onMessageRun = onMessageRun;
        }

        private final JedisPool jedisPool;

        private final String channel;

        private Consumer<String> onMessageRun;

        @Override
        public void subscribe() {
            if (!isSubscribed()) {
                Jedis jedis = jedisPool.getResource();
                try {
                    jedis.subscribe(this, channel);
                } finally {
                    jedis.close();
                }
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

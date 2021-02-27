package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * 用 {@link redis.clients.jedis.JedisPool} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see redis.clients.jedis.JedisPool
 */
public class JedisPoolAdapter implements RedisLockCommands {

    private final JedisPool jedisPool;

    private JedisPubSub jedisPubSub;

    public JedisPoolAdapter(JedisPool jedisPool) {
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
    public void subscribe(String channel, Consumer<String> onMessageRun) {
        if (jedisPubSub == null) {
            jedisPubSub = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    onMessageRun.accept(message);
                }
            };
        }
        if (!jedisPubSub.isSubscribed()) {
            Jedis jedis = jedisPool.getResource();
            try {
                jedis.subscribe(jedisPubSub, channel);
            } finally {
                jedis.close();
            }
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

    @Override
    public void unsubscribe() {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe();
        }
    }

    protected String[] mergeParams(String key, String... args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = key;
        if (args.length > 0) {
            System.arraycopy(args, 0, newArgs, 1, args.length);
        }
        return newArgs;
    }

}

package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.sub.JedisPoolSubscription;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 用 {@link redis.clients.jedis.JedisPool} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see redis.clients.jedis.JedisPool
 */
public class JedisPoolCommandsAdapter implements RedisLockCommands {

    private JedisPool jedisPool;

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

}

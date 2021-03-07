package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.sub.JedisClusterSubscription;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;

import redis.clients.jedis.JedisCluster;

/**
 * 用 {@link redis.clients.jedis.JedisCluster} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see redis.clients.jedis.JedisCluster
 */
public class JedisClusterCommandsAdapter implements RedisLockCommands {

    private JedisCluster jedisCluster;

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

}

package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

/**
 * 用于 {@link JedisCluster} 的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see redis.clients.jedis.JedisCluster
 */
public class JedisClusterAdapter implements RedisLockCommands {

    private final JedisCluster jedisCluster;

    private volatile JedisPubSub jedisPubSub;

    private Object sync = new Object();

    public JedisClusterAdapter(JedisCluster jedisCluster) {
        this.jedisCluster = Objects.requireNonNull(jedisCluster, "JedisCluster must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        return String.valueOf(jedisCluster.eval(script, 1, mergeParams(key, args)));
    }

    @Override
    public void subscribe(String channel, Consumer<String> onMessageRun) {
        if (jedisPubSub == null) {
            synchronized (sync) {
                if (jedisPubSub == null) {
                    jedisPubSub = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            onMessageRun.accept(message);
                        }
                    };
                }
            }
        }
        if (!jedisPubSub.isSubscribed()) {
            jedisCluster.subscribe(jedisPubSub, channel);
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

    @Override
    public void unsubscribe() {
        if (jedisPubSub != null) {
            synchronized (sync) {
                if (jedisPubSub != null) {
                    if (jedisPubSub.isSubscribed()) {
                        jedisPubSub.unsubscribe();
                    }
                    jedisPubSub = null;
                }
            }
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

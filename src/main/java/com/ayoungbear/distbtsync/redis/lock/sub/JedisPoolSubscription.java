package com.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import java.util.function.Consumer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 基于 {@link redis.clients.jedis.JedisPool} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class JedisPoolSubscription extends AbstractJedisSubscription implements RedisSubscription {

    public JedisPoolSubscription(JedisPool jedisPool, String channel, Consumer<String> onMessageRun) {
        super(channel, onMessageRun);
        this.jedisPool = Objects.requireNonNull(jedisPool, "JedisPool must not be null");
    }

    private final JedisPool jedisPool;

    @Override
    public void subscribe() {
        if (!isSubscribed()) {
            Jedis jedis = jedisPool.getResource();
            String channel = getChannel();
            try {
                jedis.subscribe(this, channel);
            } finally {
                jedis.close();
            }
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

}

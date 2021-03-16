package com.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import java.util.function.Consumer;

import redis.clients.jedis.JedisCluster;

/**
 * 基于 {@link redis.clients.jedis.JedisCluster} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class JedisClusterSubscription extends AbstractJedisSubscription implements RedisSubscription {

    private JedisCluster jedisCluster;

    public JedisClusterSubscription(JedisCluster jedisCluster, String channel, Consumer<String> onMessageRun) {
        super(channel, onMessageRun);
        this.jedisCluster = Objects.requireNonNull(jedisCluster, "JedisCluster must not be null");
    }

    @Override
    public void subscribe() {
        if (!isSubscribed()) {
            String channel = getChannel();
            jedisCluster.subscribe(this, channel);
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

}

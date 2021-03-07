package com.ayoungbear.distbtsync.redis.lock.sub;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 基于 {@link org.springframework.data.redis.connection.RedisConnection} 实现的 redis 订阅者.
 * 
 * @author yangzexiong
 * @see RedisSubscription
 */
public class RedisConnectionSubscription implements RedisSubscription, MessageListener {

    private final RedisConnection redisConnection;

    private final String channel;

    private Consumer<String> onMessageRun;

    private Semaphore latch = new Semaphore(0);

    private StringRedisSerializer serializer = new StringRedisSerializer();

    public RedisConnectionSubscription(RedisConnection redisConnection, String channel, Consumer<String> onMessageRun) {
        this.redisConnection = Objects.requireNonNull(redisConnection, "RedisConnection must not be null");
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.onMessageRun = onMessageRun;
    }

    @Override
    public void subscribe() {
        if (!isSubscribed()) {
            // RedisConnection 根据实现类不同, 订阅方法可能是异步的, 比如 LettuceConnection
            redisConnection.subscribe(this, serializer.serialize(channel));
            // 如果是异步订阅, 那么订阅线程在此阻塞, 等待解除订阅
            latch.acquireUninterruptibly();
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

    @Override
    public void unsubscribe() {
        if (isSubscribed()) {
            try {
                Subscription subscription = redisConnection.getSubscription();
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            } finally {
                latch.release();
            }
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (onMessageRun != null) {
            String messageStr = serializer.deserialize(message.getBody());
            onMessageRun.accept(messageStr);
        }
    }

    @Override
    public boolean isSubscribed() {
        return redisConnection.isSubscribed();
    }

    @Override
    public String getChannel() {
        return channel;
    }

    public void setOnMessageRun(Consumer<String> onMessageRun) {
        this.onMessageRun = onMessageRun;
    }

}

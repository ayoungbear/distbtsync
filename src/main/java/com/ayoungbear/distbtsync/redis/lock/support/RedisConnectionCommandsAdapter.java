package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisSubscription;

/**
 * 用 {@link org.springframework.data.redis.connection.RedisConnection} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see org.springframework.data.redis.connection.RedisConnection
 * @see org.springframework.data.redis.connection.jedis.JedisScriptingCommands
 * @see org.springframework.data.redis.connection.lettuce.LettuceScriptingCommands
 */
public class RedisConnectionCommandsAdapter implements RedisLockCommands {

    private Supplier<RedisConnection> connectionSupplier;

    private StringRedisSerializer serializer = new StringRedisSerializer();

    public RedisConnectionCommandsAdapter(RedisConnection redisConnection) {
        Assert.notNull(redisConnection, () -> "RedisConnection must not be null");
        this.connectionSupplier = SingletonSupplier.of(redisConnection);
    }

    public RedisConnectionCommandsAdapter(Supplier<RedisConnection> redisConnectionSupplier) {
        Assert.notNull(redisConnectionSupplier, () -> "RedisConnection supplier must not be null");
        this.connectionSupplier = redisConnectionSupplier;
    }

    public RedisConnectionCommandsAdapter(RedisConnectionFactory redisConnectionFactory) {
        Assert.notNull(redisConnectionFactory, () -> "RedisConnectionFactory must not be null");
        this.connectionSupplier = () -> redisConnectionFactory.getConnection();
    }

    @Override
    public String eval(String script, String key, String... args) {
        RedisConnection connection = connectionSupplier.get();
        Object result = connection.eval(serialize(script), ReturnType.VALUE, 1, keyAndArgs(key, args));
        return deserializeResult(result);
    }

    @Override
    public RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun) {
        RedisConnection redisConnection = connectionSupplier.get();
        return new RedisConnectionSubscription(redisConnection, channel, onMessageRun);
    }

    protected byte[] serialize(String script) {
        return getRedisSerializer().serialize(script);
    }

    protected String deserializeResult(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof byte[]) {
            return getRedisSerializer().deserialize((byte[]) result);
        }
        return String.valueOf(result);
    }

    protected byte[][] keyAndArgs(String key, String... args) {
        byte[][] keyAndArgs = new byte[args.length + 1][];
        int i = 0;
        keyAndArgs[i++] = getRedisSerializer().serialize(key);
        for (String arg : args) {
            keyAndArgs[i++] = getRedisSerializer().serialize(arg);
        }
        return keyAndArgs;
    }

    private RedisSerializer<String> getRedisSerializer() {
        return serializer;
    }

    /**
     * 基于 {@link org.springframework.data.redis.connection.RedisConnection} 实现的 redis 订阅者.
     * 
     * @author yangzexiong
     * @see RedisSubscription
     */
    public static class RedisConnectionSubscription implements RedisSubscription, MessageListener {

        private final RedisConnection redisConnection;

        private final String channel;

        private Consumer<String> onMessageRun;

        private Semaphore latch = new Semaphore(0);

        private StringRedisSerializer serializer = new StringRedisSerializer();

        public RedisConnectionSubscription(RedisConnection redisConnection, String channel,
                Consumer<String> onMessageRun) {
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

}

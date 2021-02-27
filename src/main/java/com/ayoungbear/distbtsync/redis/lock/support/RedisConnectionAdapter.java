package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;

/**
 * 用 {@link org.springframework.data.redis.connection.RedisConnection} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see org.springframework.data.redis.connection.RedisConnection
 * @see org.springframework.data.redis.connection.jedis.JedisScriptingCommands
 * @see org.springframework.data.redis.connection.lettuce.LettuceScriptingCommands
 */
public class RedisConnectionAdapter implements RedisLockCommands {

    private Supplier<RedisConnection> connectionSupplier;

    private StringRedisSerializer serializer = new StringRedisSerializer();

    private RedisConnection subRedisConnection;

    private Semaphore latch = new Semaphore(0);

    public RedisConnectionAdapter(RedisConnection redisConnection) {
        Assert.notNull(redisConnection, () -> "RedisConnection must not be null");
        this.connectionSupplier = SingletonSupplier.of(redisConnection);
    }

    public RedisConnectionAdapter(Supplier<RedisConnection> redisConnectionSupplier) {
        Assert.notNull(redisConnectionSupplier, () -> "RedisConnection supplier must not be null");
        this.connectionSupplier = redisConnectionSupplier;
    }

    public RedisConnectionAdapter(RedisConnectionFactory redisConnectionFactory) {
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
    public void subscribe(String channel, Consumer<String> onMessageRun) {
        if (subRedisConnection == null) {
            subRedisConnection = connectionSupplier.get();
        }
        if (!subRedisConnection.isSubscribed()) {
            // RedisConnection 根据实现类不同, 订阅方法可能是异步的, 比如 LettuceConnection
            subRedisConnection.subscribe((message, pattern) -> {
                onMessageRun.accept(deserializeResult(message.getBody()));
            }, serialize(channel));
            // 如果是异步订阅, 那么订阅线程在此阻塞, 等待解除订阅
            latch.acquireUninterruptibly();
        } else {
            throw new IllegalMonitorStateException("Already in a subscription");
        }
    }

    @Override
    public void unsubscribe() {
        if (subRedisConnection != null && subRedisConnection.isSubscribed()) {
            try {
                Subscription subscription = subRedisConnection.getSubscription();
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            } finally {
                latch.release();
            }
        }
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

}

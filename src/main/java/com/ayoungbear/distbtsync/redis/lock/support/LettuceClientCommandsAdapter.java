package com.ayoungbear.distbtsync.redis.lock.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.sub.LettuceClientSubscription;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;

import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * 用 {@link io.lettuce.core.RedisClient} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see io.lettuce.core.cluster.RedisClusterClient
 */
public class LettuceClientCommandsAdapter implements RedisLockCommands, Closeable {

    private RedisClient client;

    private volatile StatefulRedisConnection<String, String> connection;

    public LettuceClientCommandsAdapter(RedisClient client) {
        this.client = Objects.requireNonNull(client, "RedisClient must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        StatefulRedisConnection<String, String> connection = getConnection();
        RedisCommands<String, String> commands = connection.sync();
        String result = commands.eval(script, ScriptOutputType.VALUE, new String[] { key }, args);
        return result;
    }

    @Override
    public RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun) {
        return new LettuceClientSubscription(client, channel, onMessageRun);
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    private StatefulRedisConnection<String, String> getConnection() {
        if (connection == null) {
            synchronized (this) {
                if (connection == null) {
                    connection = client.connect();
                }
            }
        }
        return connection;
    }

}

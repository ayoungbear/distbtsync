package com.ayoungbear.distbtsync.redis.lock.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.sub.LettuceClusterSubscription;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

/**
 * 用 {@link io.lettuce.core.cluster.RedisClusterClient} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see io.lettuce.core.cluster.RedisClusterClient
 */
public class LettuceClusterCommandsAdapter implements RedisLockCommands, Closeable {

    private RedisClusterClient client;

    private volatile StatefulRedisClusterConnection<String, String> connection;

    public LettuceClusterCommandsAdapter(RedisClusterClient client) {
        this.client = Objects.requireNonNull(client, "RedisClusterClient must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        StatefulRedisClusterConnection<String, String> connection = getConnection();
        RedisAdvancedClusterCommands<String, String> commands = connection.sync();
        String result = commands.eval(script, ScriptOutputType.VALUE, new String[] { key }, args);
        return result;
    }

    @Override
    public RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun) {
        return new LettuceClusterSubscription(client, channel, onMessageRun);
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    private StatefulRedisClusterConnection<String, String> getConnection() {
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

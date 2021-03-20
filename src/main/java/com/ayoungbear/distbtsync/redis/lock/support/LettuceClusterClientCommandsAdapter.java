/**
 * Copyright 2021 yangzexiong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ayoungbear.distbtsync.redis.lock.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.sub.LettuceClusterClientSubscription;
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
public class LettuceClusterClientCommandsAdapter implements RedisLockCommands, Closeable {

    private RedisClusterClient client;

    private volatile StatefulRedisClusterConnection<String, String> connection;

    public LettuceClusterClientCommandsAdapter(RedisClusterClient client) {
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
        return new LettuceClusterClientSubscription(client, channel, onMessageRun);
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

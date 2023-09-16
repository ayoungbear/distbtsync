/**
 * Copyright 2021 yangzexiong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ayoungbear.distbtsync.redis.lock.support;

import com.github.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.github.ayoungbear.distbtsync.redis.lock.sub.LettuceClientSubscription;
import com.github.ayoungbear.distbtsync.redis.lock.sub.MessageConsumer;
import com.github.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

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
        String result = commands.eval(script, ScriptOutputType.VALUE, new String[]{key}, args);
        return result;
    }

    @Override
    public RedisSubscription getSubscription(String channel, MessageConsumer<String> messageConsumer) {
        return new LettuceClientSubscription(client, channel, messageConsumer);
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

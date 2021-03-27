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

import java.util.function.Supplier;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.sub.MessageConsumer;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisConnectionSubscription;
import com.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;

/**
 * 用 {@link org.springframework.data.redis.connection.RedisConnection} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see org.springframework.data.redis.connection.RedisConnection
 */
public class RedisConnectionCommandsAdapter implements RedisLockCommands {

    private Supplier<RedisConnection> connectionSupplier;

    private StringRedisSerializer serializer = new StringRedisSerializer();

    public RedisConnectionCommandsAdapter(RedisConnection redisConnection) {
        Assert.notNull(redisConnection, "RedisConnection must not be null");
        this.connectionSupplier = () -> redisConnection;
    }

    public RedisConnectionCommandsAdapter(Supplier<RedisConnection> redisConnectionSupplier) {
        Assert.notNull(redisConnectionSupplier, "RedisConnection supplier must not be null");
        this.connectionSupplier = redisConnectionSupplier;
    }

    public RedisConnectionCommandsAdapter(RedisConnectionFactory redisConnectionFactory) {
        Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
        this.connectionSupplier = () -> redisConnectionFactory.getConnection();
    }

    @Override
    public String eval(String script, String key, String... args) {
        RedisConnection connection = connectionSupplier.get();
        Object result = connection.eval(serialize(script), ReturnType.VALUE, 1, keyAndArgs(key, args));
        return deserializeResult(result);
    }

    @Override
    public RedisSubscription getSubscription(String channel, MessageConsumer<String> messageConsumer) {
        return new RedisConnectionSubscription(connectionSupplier, channel, messageConsumer);
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

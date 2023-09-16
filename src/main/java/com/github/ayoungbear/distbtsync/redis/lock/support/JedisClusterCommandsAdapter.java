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
import com.github.ayoungbear.distbtsync.redis.lock.sub.JedisClusterSubscription;
import com.github.ayoungbear.distbtsync.redis.lock.sub.MessageConsumer;
import com.github.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;
import java.util.Objects;
import redis.clients.jedis.JedisCluster;

/**
 * 用 {@link redis.clients.jedis.JedisCluster} 实现的 redis 分布式锁操作接口的适配器.
 *
 * @author yangzexiong
 * @see redis.clients.jedis.JedisCluster
 */
public class JedisClusterCommandsAdapter implements RedisLockCommands {

    private JedisCluster jedisCluster;

    public JedisClusterCommandsAdapter(JedisCluster jedisCluster) {
        this.jedisCluster = Objects.requireNonNull(jedisCluster, "JedisCluster must not be null");
    }

    @Override
    public String eval(String script, String key, String... args) {
        Object result = jedisCluster.eval(script, 1, mergeParams(key, args));
        return result == null ? null : String.valueOf(result);
    }

    @Override
    public RedisSubscription getSubscription(String channel, MessageConsumer<String> messageConsumer) {
        return new JedisClusterSubscription(jedisCluster, channel, messageConsumer);
    }

    protected String[] mergeParams(String key, String... args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = key;
        if (args.length > 0) {
            System.arraycopy(args, 0, newArgs, 1, args.length);
        }
        return newArgs;
    }

}

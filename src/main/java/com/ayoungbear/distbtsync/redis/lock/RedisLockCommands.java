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
package com.ayoungbear.distbtsync.redis.lock;

import java.util.function.Consumer;

import com.ayoungbear.distbtsync.redis.lock.sub.RedisSubscription;

/**
 * 定义 Redis 分布式锁实现所需的基础操作接口.
 * 
 * @author yangzexiong
 */
public interface RedisLockCommands {

    /**
     * 计算给定的脚本并将结果作为字符串返回.
     * @param script LUA脚本内容
     * @param key 键值
     * @param args 参数
     * @return
     */
    String eval(String script, String key, String... args);

    /**
     * 根据给定的频道和消息消费操作, 返回相应 redis 订阅者的实现类.
     * @param channel
     * @param onMessageRun
     * @return
     */
    RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun);

}

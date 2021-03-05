package com.ayoungbear.distbtsync.redis.lock;

import java.util.function.Consumer;

/**
 * 定义 Redis 分布式锁实现所需的基础操作接口.
 * 
 * @author yangzexiong
 */
public interface RedisLockCommands {

    /**
     * 计算给定的脚本并将结果作为字符串返回
     * @param script LUA脚本内容
     * @param key 键值
     * @param args 参数
     * @return
     */
    String eval(String script, String key, String... args);

    /**
     * 根据给定的频道和消息消费操作, 返回相应 redis 订阅者的实现类
     * @param channel
     * @param onMessageRun
     * @return
     */
    RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun);

}

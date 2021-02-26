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
     * 订阅功能, 订阅时需要阻塞当前线程, 用于解锁时通知等待者竞争锁
     * @param channel
     * @param onMessageRun
     */
    void subscribe(String channel, Consumer<String> onMessageRun);

    /**
     * 取消订阅, 如果没订阅则无影响
     */
    void unsubscribe();

}

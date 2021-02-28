package com.ayoungbear.distbtsync.redis.lock;

import java.util.function.Consumer;

/**
 * 定义 Redis 订阅功能操作接口.
 * 
 * @author yangzexiong
 */
public interface RedisSubCommands {

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

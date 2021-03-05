package com.ayoungbear.distbtsync.redis.lock;

/**
 * 定义 Redis 订阅功能操作接口.
 * 
 * @author yangzexiong
 */
public interface RedisSubscription {

    /**
     * 执行订阅, 并阻塞线程
     */
    void subscribe();

    /**
     * 取消订阅
     */
    void unsubscribe();

}

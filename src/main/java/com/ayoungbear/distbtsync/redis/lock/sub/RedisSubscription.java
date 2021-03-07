package com.ayoungbear.distbtsync.redis.lock.sub;

/**
 * 定义 Redis 单频道订阅功能操作接口.
 * 
 * @author yangzexiong
 */
public interface RedisSubscription {

    /**
     * 订阅指定的频道并阻塞线程
     */
    void subscribe();

    /**
     * 取消订阅
     */
    void unsubscribe();

    /**
     * 是否处于订阅中
     * @return
     */
    boolean isSubscribed();

    /**
     * 获取订阅的频道
     * @return
     */
    String getChannel();

}

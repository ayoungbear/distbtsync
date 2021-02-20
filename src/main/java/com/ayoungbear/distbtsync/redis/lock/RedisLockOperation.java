package com.ayoungbear.distbtsync.redis.lock;

/**
 * redis 加锁操作接口.
 * 
 * @author yangzexiong
 */
@FunctionalInterface
public interface RedisLockOperation {

    /**
     * 使用给定的 {@link RedisLock} 加锁
     * @param lock
     * @return
     */
    boolean doLock(RedisLock lock);

}

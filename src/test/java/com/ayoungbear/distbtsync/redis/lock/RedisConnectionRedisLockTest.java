package com.ayoungbear.distbtsync.redis.lock;

/**
 * 基于 redis 的可重入分布式锁单元测试
 * 使用 RedisConnection
 * 
 * @author yangzexiong
 */
public class RedisConnectionRedisLockTest extends AbstractRedisBasedLockTest {

    @Override
    protected RedisLock getRedisLock(String key) {
        RedisLockCommands commands = getRedisConnectionCommandsAdapter(); // 使用 RedisConnection 测试, 默认会使用 LettuceConnection
        return new RedisBasedLock(key, commands);
    }

}

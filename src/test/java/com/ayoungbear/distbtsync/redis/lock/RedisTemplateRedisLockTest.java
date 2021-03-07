package com.ayoungbear.distbtsync.redis.lock;

/**
 * 基于 redis 的可重入分布式锁单元测试
 * 使用 RedisTemplate
 * 
 * @author yangzexiong
 */
public class RedisTemplateRedisLockTest extends AbstractRedisBasedLockTest {

    @Override
    protected RedisLock getRedisLock(String key) {
        RedisLockCommands commands = getRedisTemplateCommandsAdapter();
        return new RedisBasedLock(key, commands);
    }

}

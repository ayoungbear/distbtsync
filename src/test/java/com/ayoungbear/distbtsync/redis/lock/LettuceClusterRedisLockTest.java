package com.ayoungbear.distbtsync.redis.lock;

/**
 * 基于 redis 的可重入分布式锁单元测试
 * 使用 LettuceCluster
 * 
 * @author yangzexiong
 */
public class LettuceClusterRedisLockTest extends AbstractRedisBasedLockTest {

    @Override
    protected RedisLock getRedisLock(String key) {
        RedisLockCommands commands = getLettuceClusterCommandsAdapter(); // 使用 Lettuce 测试
        return new RedisBasedLock(key, commands);
    }

}

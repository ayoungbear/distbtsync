package com.github.ayoungbear.distbtsync.redis.lock;

/**
 * 基于 redis 的可重入分布式锁单元测试
 * 使用 LettuceCluster
 *
 * @author yangzexiong
 */
public class LettuceClientRedisLockTest extends AbstractRedisBasedLockTest {

    @Override
    protected RedisLock getRedisLock(String key) {
        RedisLockCommands commands = null;
        commands = getLettuceClusterClientCommandsAdapter(); // 使用 LettuceClusterClient 测试
        // commands = getLettuceClientCommandsAdapter(); // 使用 LettuceClient 测试
        return new RedisBasedLock(key, commands);
    }

}

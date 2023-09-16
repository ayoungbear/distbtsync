package com.github.ayoungbear.distbtsync.redis.lock;

/**
 * 基于 redis 的可重入分布式锁单元测试
 * 使用 JedisCluster
 *
 * @author yangzexiong
 */
public class JedisRedisLockTest extends AbstractRedisBasedLockTest {

    @Override
    protected RedisLock getRedisLock(String key) {
        RedisLockCommands commands = null;
        commands = getJedisClusterCommandsAdapter(); // 使用 jedis cluster 测试
        // commands = getJedisPoolCommandsAdapter(); // 使用 jedis pool 测试
        return new RedisBasedLock(key, commands);
    }

}

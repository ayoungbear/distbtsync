package com.ayoungbear.distbtsync.redis.lock;

/**
 * 定义 Redis 分布式锁实现所需的基础操作接口.
 * 
 * @author yangzexiong
 */
public interface RedisLockCommands extends RedisSubCommands {

    /**
     * 计算给定的脚本并将结果作为字符串返回
     * @param script LUA脚本内容
     * @param key 键值
     * @param args 参数
     * @return
     */
    String eval(String script, String key, String... args);

}

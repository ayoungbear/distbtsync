package com.ayoungbear.distbtsync.redis.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 基于 redis 的分布式锁基础类, 定义加锁的实际操作并提供了基础功能方法,
 * 同时还实现了一些接口方法.
 * 
 * @author yangzexiong
 * @see RedisLock
 */
public abstract class AbstractRedisLock implements RedisLock {
    
    protected static final int UNLIMIT_LEASE_TIME = 0;
    protected static final int NOT_WAIT_TIME = 0;
    protected static final int spinForBlockTimeoutThreshold = 1000;
    
    protected static final String FAIL = "0";
    protected static final String SUCCESS = "1";
    protected static final String TRY_ACQUIRE_SUCCESS = "OK";

    private static final String CHANNEL_PREFIX = "distbtsync_redis_lock_";

    private static final String TRY_ACQUIRE_SCRIPT = "if (redis.call('exists', KEYS[1]) == 0) or (redis.call('hexists', KEYS[1], ARGV[1])) == 1 then " +
                                                         "redis.call('hincrby', KEYS[1], ARGV[1], 1); " +
                                                         "if (tonumber(ARGV[2]) > 0) then "+
                                                             "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                                                         "end; "+
                                                         "return 'OK'; " +
                                                     "end; " +
                                                     "return tostring(redis.call('pttl', KEYS[1])); ";

    private static final String TRY_RELEASE_SCRIPT = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then " +
                                                         "return '-1';" +
                                                     "end; " +    
                                                     "local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1); " +
                                                     "if (counter > 0) then "+    
                                                         "return tostring(counter); " +
                                                     "else " +
                                                         "redis.call('del', KEYS[1]); " +
                                                         "redis.call('publish', ARGV[2], KEYS[1]); " +
                                                         "return '0'; " +
                                                     "end; ";
    
    private static final String DELETE_SCRIPT = "return tostring(redis.call('del', KEYS[1])); ";

    private static final String IS_ACQUIRED_SCRIPT = "return tostring(redis.call('hexists', KEYS[1], ARGV[1])); ";

    private static final String EXISTS_SCRIPT = "return tostring(redis.call('exists', KEYS[1])); ";

    private static final String HOLD_COUNT_SCRIPT = "return tostring(redis.call('hget', KEYS[1], ARGV[1])); ";
    
    private static final String EXPIRED_SCRIPT = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then " +
                                                     "return '0';" +
                                                 "end; " +    
                                                 "return tostring(redis.call('pexpire', KEYS[1], ARGV[2])); ";

    /**
     * 锁名称
     */
    protected final String key;
    /**
     * 锁标识前缀
     */
    protected final String id;
    /**
     * 锁释放通知频道
     */
    protected final String channel;
    /**
     * redis 锁基础操作接口
     */
    protected final RedisLockCommands commands;

    /*
     * 锁的失效时间本地缓存(ms)
     */
    private volatile long ttl = -1;
    /**
     * 如果锁没有设置过期时间, 使用该值阻塞超时时间(30s), 避免死锁
     */
    private long defaultTimeoutMillisForUnlimitTtl = 5000;

    protected AbstractRedisLock(String key, RedisLockCommands commands) {
        this.key = Objects.requireNonNull(key, "Key must not be null");
        this.commands = Objects.requireNonNull(commands, "Redis commands adapter must not be null");
        this.id = key + ":" + UUID.randomUUID().toString() + ":";
        this.channel = CHANNEL_PREFIX + getLockName();
    }

    @Override
    public String getLockName() {
        return key;
    }

    @Override
    public boolean forceUnlock() {
        return doDelete();
    }

    @Override
    public boolean isLocked() {
        return doExists();
    }

    /**
     * 获取锁失效时间缓存(ms)
     * @return the ttl
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * 使用给定的标识尝试加锁, 同时会缓存锁的失效时间ttl
     * @param identifier 锁标识
     * @param leaseTimeMillis 过期时间(ms)
     * @return {@code true} 加锁成功
     */
    protected boolean doTryAcquire(String identifier, long leaseTimeMillis) {
        String result = eval(TRY_ACQUIRE_SCRIPT, key, identifier, String.valueOf(leaseTimeMillis));
        if (TRY_ACQUIRE_SUCCESS.equals(result)) {
            // 加锁成功
            this.ttl = leaseTimeMillis;
            return true;
        } else {
            this.ttl = Long.valueOf(result);
            return false;
        }
    }

    /**
     * 使用给定的标识尝试解锁, 只有锁的持有者才能解锁成功
     * @param identifier 锁标识
     * @return 返回剩余的加锁次数(可重入)
     */
    protected int doTryRelease(String identifier) {
        Integer result = Integer.valueOf(eval(TRY_RELEASE_SCRIPT, key, identifier, channel));
        return result;
    }

    /**
     * 删除锁对应的key
     * @return
     */
    protected boolean doDelete() {
        return SUCCESS.equals(eval(DELETE_SCRIPT, key));
    }

    /**
     * 判断锁是否被指定锁标识的持有者持有
     * 通过校验锁对应key和相应标识符是否匹配
     * @param identifier
     * @return
     */
    protected boolean isAcquired(String identifier) {
        return SUCCESS.equals(eval(IS_ACQUIRED_SCRIPT, key, identifier));
    }

    /**
     * 判断锁对应的key是否已存在
     * @return
     */
    protected boolean doExists() {
        return SUCCESS.equals(eval(EXISTS_SCRIPT, key));
    }

    /**
     * 判断当前线程持有可重入锁的次数
     * @param identifier
     * @return
     */
    protected int doGetHoldCount(String identifier) {
        Object count = eval(HOLD_COUNT_SCRIPT, key, identifier);
        return count == null ? 0 : Integer.valueOf(count.toString());
    }

    /**
     * 使用给定的锁标识延长锁的过期时间, 只有锁的持有者可以延长
     * @param identifier
     * @param leaseTimeMillis
     * @return
     */
    protected boolean doExpired(String identifier, long leaseTimeMillis) {
        return SUCCESS.equals(eval(EXPIRED_SCRIPT, key, identifier, String.valueOf(leaseTimeMillis)));
    }

    /**
     * 获取加锁标识, 如果没有则新生成
     * @return
     */
    protected String getIdentifier() {
        String identifier = getSourceIdentifier();
        if (identifier == null) {
            return id + Thread.currentThread().getId();
        }
        return identifier;
    }

    /**
     * 获取源加锁标识
     * @return
     */
    protected String getSourceIdentifier() {
        String identifier = RedisLockIdentifierManager.getIdentifier(key);
        if (identifier == null) {
            // 删除无用的标识符或清除标识符缓存
            removeIdentifier();
        }
        return identifier;
    }

    /**
     * 记录锁标识
     * @param identifier
     */
    protected void setSourceIdentifier(String identifier) {
        RedisLockIdentifierManager.setIdentifier(key, identifier);
    }

    /**
     * 清除锁标识
     */
    protected void removeIdentifier() {
        RedisLockIdentifierManager.removeIdentifier(key);
    }
    
    /**
     * 执行加锁操作脚本命令
     * @param script
     * @param key
     * @param args
     * @return
     */
    protected final String eval(String script, String key, String... args) {
        return commands.eval(script, key, args);
    }

    /**
     * 校验锁过期时间是否合法
     * @param leaseTime
     */
    protected void validateLeaseTime(long leaseTime) {
        if (leaseTime <= 0) {
            throw new IllegalArgumentException("Invalid lease time '" + leaseTime + "'");
        }
    }

    /**
     * 判断是否曾经加锁成功(有持有相应的锁标识)
     * @return
     */
    protected boolean onceLocked() {
        return getSourceIdentifier() != null;
    }

    /**
     * 用于管理各线程持有 redis 锁标识的类, 锁标识用于判断是否持有该锁
     * 
     * @author yangzexiong
     */
    private static class RedisLockIdentifierManager {

        /**
         * 用于验证锁是否被自己持有的标识符
         */
        private static final ThreadLocal<Map<String, String>> lockIdentifierManager = new ThreadLocal<>();

        /**
         * 获取锁对应的标识符
         * @param key
         * @return
         */
        protected static String getIdentifier(String key) {
            return getLocalIdentifierMap().get(key);
        }

        /**
         * 记录锁对应的标识符
         * @param key
         * @param identifier
         * @return 旧的锁标识符(如果有)
         */
        protected static String setIdentifier(String key, String identifier) {
            return getLocalIdentifierMap().put(key, identifier);
        }

        /**
         * 清除锁对应的标识符
         * @param key
         * @return
         */
        protected static String removeIdentifier(String key) {
            Map<String, String> localIdentifierMap = getLocalIdentifierMap();
            String identifier = localIdentifierMap.remove(key);
            if (localIdentifierMap.isEmpty()) {
                lockIdentifierManager.remove();
            }
            return identifier;
        }

        private static Map<String, String> getLocalIdentifierMap() {
            Map<String, String> localIdentifierMap = lockIdentifierManager.get();
            if (localIdentifierMap == null) {
                localIdentifierMap = new HashMap<>(8);
                lockIdentifierManager.set(localIdentifierMap);
            }
            return localIdentifierMap;
        }

    }

    public void setDefaultTimeoutMillisForUnlimitTtl(long defaultTimeoutMillisForUnlimitTtl) {
        this.defaultTimeoutMillisForUnlimitTtl = defaultTimeoutMillisForUnlimitTtl;
    }

    public long getDefaultTimeoutMillisForUnlimitTtl() {
        return defaultTimeoutMillisForUnlimitTtl;
    }

}

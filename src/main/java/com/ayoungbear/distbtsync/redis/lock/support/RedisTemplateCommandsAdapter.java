package com.ayoungbear.distbtsync.redis.lock.support;

import java.util.Collections;
import java.util.function.Consumer;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisSubscription;
import com.ayoungbear.distbtsync.redis.lock.support.RedisConnectionCommandsAdapter.RedisConnectionSubscription;

/**
 * 用 {@link org.springframework.data.redis.core.RedisTemplate} 实现的 redis 分布式锁操作接口的适配器.
 * 
 * @author yangzexiong
 * @see org.springframework.data.redis.core.RedisTemplate
 */
public class RedisTemplateCommandsAdapter implements RedisLockCommands {

    private RedisTemplate<String, String> redisTemplate;

    private StringRedisSerializer serializer = new StringRedisSerializer();

    public RedisTemplateCommandsAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String eval(String script, String key, String... args) {
        RedisScript<String> redisScript = new DefaultRedisScript<String>(script, String.class);
        Object[] argsObj = args;
        String result = redisTemplate.execute(redisScript, serializer, serializer, Collections.singletonList(key),
                argsObj);
        return result;
    }

    @Override
    public RedisSubscription getSubscription(String channel, Consumer<String> onMessageRun) {
        RedisConnection redisConnection = redisTemplate.getRequiredConnectionFactory().getConnection();
        return new RedisConnectionSubscription(redisConnection, channel, onMessageRun);
    }

}

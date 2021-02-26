package com.ayoungbear.distbtsync.redis;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * spring 自动配置 redis 测试基础类
 * 
 * @author yangzexiong
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BaseSpringRedisTest.BaseSpringRedisTestConfiguration.class)
@TestPropertySource(properties = { "spring.redis.cluster.nodes=" + BaseRedisTest.HOST_AND_PORT,
        "spring.redis.timeout=60000" })
public abstract class BaseSpringRedisTest extends BaseRedisTest {

    @Configuration
    @Import({ RedisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
    public static class BaseSpringRedisTestConfiguration {
    }

    @Autowired
    protected RedisConnectionFactory redisConnectionFactory;

}
package com.ayoungbear.distbtsync.redis;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;

import com.ayoungbear.distbtsync.BaseTest;
import com.ayoungbear.distbtsync.redis.lock.support.JedisClusterCommandsAdapter;
import com.ayoungbear.distbtsync.redis.lock.support.JedisPoolCommandsAdapter;
import com.ayoungbear.distbtsync.redis.lock.support.LettuceClusterCommandsAdapter;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.RedisClusterURIUtil;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

/**
 * redis相关测试基础类
 * 
 * @author yangzexiong
 */
public abstract class BaseRedisTest extends BaseTest {

    public static final String HOST_AND_PORT = "192.168.42.217:6379," + 
                                               "192.168.42.217:6380," + 
                                               "192.168.42.217:6381," + 
                                               "192.168.42.217:6382," + 
                                               "192.168.42.217:6383," + 
                                               "192.168.42.217:6384";

    protected static final List<HostAndPort> redisClusterNodes = Arrays.asList(HOST_AND_PORT.split(",")).stream()
            .map((hp) -> hp.split(":")).map((hp) -> new HostAndPort(hp[0], Integer.valueOf(hp[1])))
            .collect(Collectors.toList());

    protected static final JedisCluster jedisCluster = getJedisCluster(8);

    protected static final JedisPool jedisPool = getJedisPool();

    protected static final RedisClusterClient redisClusterClient = newRedisClusterClient();

    protected static JedisCluster getJedisCluster(int maxTotal) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxTotal);
        JedisCluster jedisCluster = new JedisCluster(new HashSet<>(redisClusterNodes), config);
        return jedisCluster;
    }

    protected static JedisPool getJedisPool() {
        JedisPool jedisPool = new JedisPool("192.168.42.217", 6370);
        return jedisPool;
    }

    protected static RedissonClient getRedissonClient(int subscriptionConnectionPoolSize) {
        Config config = new Config();
        ClusterServersConfig clustConfig = config.useClusterServers()
                .addNodeAddress(redisClusterNodes.stream().map((nodeAddress) -> "redis://" + nodeAddress.toString())
                        .collect(Collectors.toList()).toArray(new String[0]));
        clustConfig.setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

    protected static RedisClusterClient getRedisClusterClient() {
        return redisClusterClient;
    }

    protected static RedisClusterClient newRedisClusterClient() {
        return RedisClusterClient.create(RedisClusterURIUtil.toRedisURIs(URI.create("redis://" + HOST_AND_PORT)));
    }

    protected JedisClusterCommandsAdapter getJedisClusterCommandsAdapter() {
        return new JedisClusterCommandsAdapter(getJedisCluster(20));
    }

    protected JedisPoolCommandsAdapter getJedisPoolCommandsAdapter() {
        return new JedisPoolCommandsAdapter(getJedisPool());
    }

    protected LettuceClusterCommandsAdapter getLettuceClusterCommandsAdapter() {
        return new LettuceClusterCommandsAdapter(getRedisClusterClient());
    }

}

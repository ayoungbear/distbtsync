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
import com.ayoungbear.distbtsync.redis.lock.support.LettuceClientCommandsAdapter;
import com.ayoungbear.distbtsync.redis.lock.support.LettuceClusterClientCommandsAdapter;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
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

    /**
     * redis 集群地址
     */
    public static final String HOST_AND_PORT = "192.168.42.217:6379," + 
                                               "192.168.42.217:6380," + 
                                               "192.168.42.217:6381," + 
                                               "192.168.42.217:6382," + 
                                               "192.168.42.217:6383," + 
                                               "192.168.42.217:6384";

    /**
     * redis 单机地址
     */
    public static final String SINGLE_REDIS_HOST = "192.168.42.217:6370";

    protected static final List<HostAndPort> redisClusterNodes = Arrays.asList(HOST_AND_PORT.split(",")).stream()
            .map((hp) -> hp.split(":")).map((hp) -> new HostAndPort(hp[0], Integer.valueOf(hp[1])))
            .collect(Collectors.toList());

    protected static final JedisCluster jedisCluster = getJedisCluster(8);

    protected static final JedisPool jedisPool = getJedisPool();

    protected static final RedisClusterClient redisClusterClient = newRedisClusterClient();

    protected static final RedisClient redisClient = newRedisClient();

    protected static JedisCluster getJedisCluster(int maxTotal) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxTotal);
        JedisCluster jedisCluster = new JedisCluster(new HashSet<>(redisClusterNodes), config);
        return jedisCluster;
    }

    protected static JedisPool getJedisPool() {
        JedisPool jedisPool = new JedisPool("redis://" + SINGLE_REDIS_HOST);
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

    protected static RedisClient getRedisclient() {
        return redisClient;
    }

    protected static RedisClient newRedisClient() {
        return RedisClient.create(RedisURI.create("redis://" + SINGLE_REDIS_HOST));
    }

    protected JedisClusterCommandsAdapter getJedisClusterCommandsAdapter() {
        return new JedisClusterCommandsAdapter(getJedisCluster(20));
    }

    protected JedisPoolCommandsAdapter getJedisPoolCommandsAdapter() {
        return new JedisPoolCommandsAdapter(getJedisPool());
    }

    protected LettuceClusterClientCommandsAdapter getLettuceClusterClientCommandsAdapter() {
        return new LettuceClusterClientCommandsAdapter(getRedisClusterClient());
    }

    protected LettuceClientCommandsAdapter getLettuceClientCommandsAdapter() {
        return new LettuceClientCommandsAdapter(getRedisclient());
    }

}

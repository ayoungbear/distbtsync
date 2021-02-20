package com.ayoungbear.distbtsync.redis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

public abstract class BaseRedisTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    public static final String HOST_AND_PORT = "192.168.42.128:6379," + 
                                               "192.168.42.128:6380," + 
                                               "192.168.42.128:6381," + 
                                               "192.168.42.128:6382," + 
                                               "192.168.42.128:6383," + 
                                               "192.168.42.128:6384";

    protected static final List<HostAndPort> redisClusterNodes = Arrays.asList(HOST_AND_PORT.split(",")).stream()
            .map((hp) -> hp.split(":")).map((hp) -> new HostAndPort(hp[0], Integer.valueOf(hp[1])))
            .collect(Collectors.toList());

    protected static final JedisCluster jedisCluster = getJedisCluster(8);

    protected static final JedisPool jedisPool = getJedisPool();

    protected static JedisCluster getJedisCluster(int maxTotal) {
        GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<Object>();
        config.setMaxTotal(maxTotal);
        JedisCluster jedisCluster = new JedisCluster(new HashSet<>(redisClusterNodes), config);
        return jedisCluster;
    }

    protected static JedisPool getJedisPool() {
        JedisPool jedisPool = new JedisPool("192.168.42.128", 6370);
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

    @BeforeClass
    public static void baseSetUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void baseTearDownAfterClass() throws Exception {
    }

    @Before
    public void baseSetUp() throws Exception {
    }

    @After
    public void baseTearDown() throws Exception {
    }

    public static Thread run(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.start();
        return t;
    }

    public static Thread run(Runnable runnable, String name) {
        Thread t = new Thread(runnable, name);
        t.start();
        return t;
    }

    public static void sleep(long time, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(time));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sleep(long seconds) {
        sleep(seconds, TimeUnit.SECONDS);
    }

    /**
     * 并发执行任务
     * @param nodeNum 节点数, 用线程模拟分布式服务
     * @param threadNum 每个节点执行线程数
     * @param tasks 执行的任务
     */
    protected void concurrentExecute(int nodeNum, int threadNum, Runnable... tasks) {
        logger.info("ConcurrentExecute use nodeNum={} threadNum={}", nodeNum, threadNum);
        CountDownLatch countDownLatch = new CountDownLatch(nodeNum);
        for (int n = 1; n <= nodeNum; n++) {
            run(() -> {
                CountDownLatch threadCountDownLatch = new CountDownLatch(threadNum);
                try {
                    for (int t = 1; t <= threadNum; t++) {
                        run(() -> {
                            try {
                                for (Runnable task : tasks) {
                                    task.run();
                                }
                            } catch (Exception e) {
                                logger.error("concurrentExecute error", e);
                            } finally {
                                try {
                                    threadCountDownLatch.countDown();
                                } catch (Exception e) {
                                    logger.error("Await error", e);
                                }
                            }
                        }, Thread.currentThread().getName() + "-runner-" + t);
                    }
                } finally {
                    try {
                        threadCountDownLatch.await();
                        countDownLatch.countDown();
                    } catch (Exception e) {
                        logger.error("Await error", e);
                    }
                }
            }, "node-" + n);
        }
        try {
            logger.info("Waiting for the end");
            countDownLatch.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Await error", e);
        }
    }

}

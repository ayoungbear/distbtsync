package com.ayoungbear.distbtsync.redis.lock.scenario;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ayoungbear.distbtsync.redis.BaseSpringRedisTest;
import com.ayoungbear.distbtsync.redis.lock.RedisBasedLock;
import com.ayoungbear.distbtsync.redis.lock.support.JedisClusterCommandsAdapter;

/**
 * 测试多节点+多线程的分布式并发计数的场景,
 * 主要验证分布式并发下的分布式锁同步控制情况.
 * 
 * @author yangzexiong
 */
public class DistributedConcurrentCountTest extends BaseSpringRedisTest {

    /**
     * 加锁使用的key
     */
    private final String key = "ConcurrentCountLockKey";
    /**
     * 公共计数变量
     */
    private static Integer count = 0;
    /**
     * 分布式锁模式-公平/非公平
     */
    private static boolean fair = true;

    @Before
    public void setUp() throws Exception {
        jedisCluster.del(key);
        count = 0;
    }

    @After
    public void tearDown() throws Exception {
        jedisCluster.del(key);
    }

    /**
     * 使用 JedisCluster 来测试分布式锁并发计数, 测试稳定性和并发处理场景
     * @throws Exception
     */
    @Test
    public void testConcurrentCountUseJedisCluster() throws Exception {
        testConcurrentCount(
                () -> new RedisBasedLock(key, new JedisClusterCommandsAdapter(getJedisCluster(1000)), fair));
    }

    /**
     * 使用 RedisConnection 来测试分布式锁并发计数, 测试稳定性和并发处理场景
     * @throws Exception
     */
    @Test
    public void testConcurrentCountUseRedisConnection() throws Exception {
        testConcurrentCount(() -> new RedisBasedLock(key, getRedisConnectionCommandsAdapter(), fair));
    }

    /**
     * 使用 RedisTemplate 来测试分布式锁并发计数, 测试稳定性和并发处理场景
     * @throws Exception
     */
    @Test
    public void testConcurrentCountUseRedisTemplate() throws Exception {
        testConcurrentCount(() -> new RedisBasedLock(key, getRedisTemplateCommandsAdapter(), fair));
    }

    /**
     * 使用 LettuceCluster 来测试分布式锁并发计数, 测试稳定性和并发处理场景
     * @throws Exception
     */
    @Test
    public void testConcurrentCountUseLettuceCluster() throws Exception {
        testConcurrentCount(() -> new RedisBasedLock(key, getLettuceClusterCommandsAdapter(), fair));
    }

    /**
     * 使用 Redisson 来测试分布式锁并发计数, 主要用于比较
     * @throws Exception
     */
    // @Test
    public void testConcurrentCountUseRedisson() throws Exception {
        testConcurrentCount(
                () -> fair ? getRedissonClient(1000).getFairLock(key) : getRedissonClient(1000).getLock(key));
    }

    /**
     * 使用 ReentrantLock 来测试分布式锁并发计数, 重入锁只能使共用该锁的线程之间达成同步, 也就是分布式场景中该锁是没法实现同步的
     * @throws Exception
     */
    // @Test
    public void testConcurrentCountUseReentrantLock() throws Exception {
        testConcurrentCount(() -> new ReentrantLock());
    }

    /**
     * 模拟测试多节点+多线程的分布式并发计数, 并断言最终数值的准确性.
     * 可以通过日志观察到计数过程和执行耗时.
     * 如果增加了测试线程数, 注意增加连接数限制.
     * 
     * @param lockSupplier 提供具体锁的实现
     * @throws Exception
     */
    protected void testConcurrentCount(Supplier<Lock> lockSupplier) throws Exception {
        // 分布式应用节点数, 限于单机情况, 用线程来模拟分布式各应用节点
        int nodeNum = 10;
        // 每个应用节点多线程并发计数的线程数
        int threadNum = 100;
        // 每个线程计数的次数
        int countTimes = 5;
        // 并发计数开始时间
        long beginTime = System.currentTimeMillis() + 4000L;

        CountDownLatch countDownLatch = new CountDownLatch(nodeNum);
        for (int n = 1; n <= nodeNum; n++) {
            run(() -> {
                try {
                    // 每个节点另起多个线程, 使用不同的分布式锁实例
                    doConcurrentCount(threadNum, countTimes, beginTime, lockSupplier);
                    countDownLatch.countDown();
                    ;
                } catch (Exception e) {
                    logger.error("Do concurrent count run error", e);
                }
            }, "node-" + n);
        }
        countDownLatch.await();
        logger.info("Concurrent count test end use lock={} cost {}ms sharedCount={}.",
                lockSupplier.get().getClass().getSimpleName(), System.currentTimeMillis() - beginTime, count);
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum * countTimes, count.intValue());
    }

    /**
     * 模拟每个应用节点内部多线程并发对公共变量进行计数增操作,
     * 可选择具体的分布式锁实现并观察不同场景下的执行情况.
     * 
     * @param threadNum 线程数
     * @param countTimes 每个线程计数增的次数
     * @param beginTime 计数开始时间
     * @throws Exception
     */
    private void doConcurrentCount(int threadNum, int countTimes, long beginTime, Supplier<Lock> lockSupplier)
            throws Exception {
        Lock lock = lockSupplier.get();
        
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        for (int n = 1; n <= threadNum; n++) {
            run(() -> {
                try {
                    // 等待开始计数
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(beginTime - System.currentTimeMillis()));
                    logger.info("count begin use lock={}", lock.getClass());
                    for (int countTime = 0; countTime < countTimes; countTime++) {
                        // 加锁同步, 如果没有同步, 多线程情况下基本是断言失败的
                        lock.lock();
                        // logger.info("lock");

                        int localCount = count.intValue();
                        count = localCount + 1;
                        logger.info("count num={}", count);
                        // logger.info("unlock");
                        lock.unlock();
                    }
                    countDownLatch.countDown();
                } catch (Exception e) {
                    logger.error("Count error", e);
                }
            }, Thread.currentThread().getName() + "-thread-" + n);
        }
        countDownLatch.await();
    }

}

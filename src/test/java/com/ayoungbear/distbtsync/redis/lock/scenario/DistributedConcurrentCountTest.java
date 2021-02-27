package com.ayoungbear.distbtsync.redis.lock.scenario;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ayoungbear.distbtsync.redis.BaseSpringRedisTest;
import com.ayoungbear.distbtsync.redis.lock.RedisBasedLock;
import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.support.JedisClusterAdapter;

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
     * 模拟测试多节点+多线程的分布式并发计数, 并断言最终数值的准确性.
     * 可以通过日志观察到计数过程和执行耗时.
     * 如果增加了测试线程数, 注意增加连接数限制.
     * 
     * @throws Exception
     */
    @Test
    public void testConcurrentCount() throws Exception {
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
                    doConcurrentCount(threadNum, countTimes, beginTime);
                    countDownLatch.countDown();
                    ;
                } catch (Exception e) {
                    logger.error("Do concurrent count run error", e);
                }
            }, "node-" + n);
        }
        countDownLatch.await();
        logger.info("Concurrent count test end cost {}ms sharedCount={}.", System.currentTimeMillis() - beginTime,
                count);
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum * countTimes, count.intValue());
    }

    /**
     * 模拟每个应用节点内部多线程并发对公共变量进行计数增操作,
     * 可选择具体的分布式锁实现并观察不同场景下的执行情况.
     * <<特指如下的分布式快速并发计数的场景>>, 简单比较了 redisson 的分布式锁和 RedisBasedLock
     * 公平锁模式下: RedisBasedLock 的处理速度要远快于 RedissonFairLock , cpu 消耗相当,  当然可能因为 RedisBasedLock 只是"相对的公平".
     * 非公平模式下: RedisBasedLock 的处理速度要略慢于 RedissonLock, 但 cpu 消耗少很多.
     * 
     * @param threadNum 线程数
     * @param countTimes 每个线程计数增的次数
     * @param beginTime 计数开始时间
     * @throws Exception
     */
    private void doConcurrentCount(int threadNum, int countTimes, long beginTime) throws Exception {
        // 分布式锁模式-公平/非公平
        boolean fair = true;

        RedisLockCommands commands = null;
        // commands = getRedisConnectionAdapter();
        commands = new JedisClusterAdapter(getJedisCluster(1000)); // 用 jedis 比 LettuceConnection 快啊...

        // ----选择需测试的分布式锁实现----
        RedisBasedLock lock = new RedisBasedLock(key, commands, fair);
        // RLock lock = fair ? getRedissonClient(1000).getFairLock(key) : getRedissonClient(1000).getLock(key);
        
        // 共享阻塞队列形式
        // RedisBasedLock lock = RedisBasedLock.newSharedLock(key, commands);

        // 重入锁只能使共用该锁的线程之间达成同步, 也就是分布式场景中该锁是没法实现同步的
        // Lock lock = new ReentrantLock();

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

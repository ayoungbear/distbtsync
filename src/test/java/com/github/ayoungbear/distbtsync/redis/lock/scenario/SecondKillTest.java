package com.github.ayoungbear.distbtsync.redis.lock.scenario;

import com.github.ayoungbear.distbtsync.BaseSpringRedisTest;
import com.github.ayoungbear.distbtsync.redis.lock.RedisBasedLock;
import com.github.ayoungbear.distbtsync.redis.lock.RedisLock;
import com.github.ayoungbear.distbtsync.redis.lock.support.JedisClusterCommandsAdapter;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 应用分布式锁模拟测试秒杀商品场景
 *
 * @author yangzexiong
 */
public class SecondKillTest extends BaseSpringRedisTest {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    /**
     * 分布式锁模式-公平/非公平
     */
    private static boolean fair = true;
    /**
     * 秒杀商品的数量
     */
    private static int goodsNum = 10;
    /**
     * 秒杀的商品货物存量
     */
    private static int goods = 0;
    /**
     * 秒杀系统的应用服务节点数
     */
    private static int nodeNum = 10;
    /**
     * 每个服务节点处理的并发请求数(线程数)
     */
    private static int requestNum = 1000;
    /**
     * 缓存用于判断是否还有商品, 如果没有了直接秒杀失败, 无需再等待了
     */
    private static boolean hasGoods = true;
    /**
     * 分布式加锁使用的key
     */
    private final String key = "SecKillLockKey";
    /**
     * 成功抢到商品的线程名与抢到的商品数
     */
    private Map<String, Integer> luckyDogs = new ConcurrentHashMap<>();

    @Before
    public void setUp() throws Exception {
        jedisCluster.del(key);
        luckyDogs.clear();
        goods = goodsNum;
        hasGoods = true;
    }

    @After
    public void tearDown() throws Exception {
        jedisCluster.del(key);
        luckyDogs.clear();
        goods = goodsNum;
        hasGoods = true;
    }

    /**
     * 使用 JedisCluster 来测试分布式锁控制秒杀场景, 测试稳定性和并发处理场景
     *
     * @throws Exception
     */
    @Test
    public void testSecKillUseJedisCluster() throws Exception {
        testSecKill(() -> new RedisBasedLock(key, new JedisClusterCommandsAdapter(getJedisCluster(2000)), fair));
    }

    /**
     * 使用 RedisConnection 来测试分布式锁控制秒杀场景, 测试稳定性和并发处理场景
     *
     * @throws Exception
     */
    @Test
    public void testSecKillUseRedisConnection() throws Exception {
        testSecKill(() -> new RedisBasedLock(key, getRedisConnectionCommandsAdapter(), fair));
    }

    /**
     * 使用 RedisTemplate 来测试分布式锁控制秒杀场景, 测试稳定性和并发处理场景
     *
     * @throws Exception
     */
    @Test
    public void testSecKillUseRedisTemplate() throws Exception {
        testSecKill(() -> new RedisBasedLock(key, getRedisTemplateCommandsAdapter(), fair));
    }

    /**
     * 使用 LettuceCluster 来测试分布式锁控制秒杀场景, 测试稳定性和并发处理场景
     *
     * @throws Exception
     */
    @Test
    public void testSecKillUseLettuceCluster() throws Exception {
        testSecKill(() -> new RedisBasedLock(key, getLettuceClusterClientCommandsAdapter(), fair));
    }

    /**
     * 使用 Redisson 来测试分布式锁控制秒杀场景, 主要用于比较
     *
     * @throws Exception
     */
    // @Test
    public void testSecKillUseRedisson() throws Exception {
        testSecKill(() -> fair ? getRedissonClient(2000).getFairLock(key) : getRedissonClient(2000).getLock(key));
    }

    /**
     * 使用 ReentrantLock 来测试分布式锁控制秒杀场景, 重入锁只能使共用该锁的线程之间达成同步, 也就是分布式场景中该锁是没法实现同步的
     *
     * @throws Exception
     */
    // @Test
    public void testSecKillUseReentrantLock() throws Exception {
        testSecKill(() -> new ReentrantLock());
    }

    /**
     * 模拟测试秒杀单个商品场景, 配置原因只能模拟简单的有限的并发场景.
     * 该测试模拟的是后台并发处理秒杀请求的情况, 前端的限流分流场景不在考虑范围内.
     *
     * 分布式锁的阻塞队列本身提供了缓冲, 避免大量请求消耗资源导致服务崩溃.
     * 商品在被秒杀完后, 可利用加锁超时方法 {@link RedisLock#tryLock(long, TimeUnit)} 的特性来加快处理请求的速度,
     * 在一定时间内没加锁成功则判断商品是否被秒空, 如被秒空则秒杀失败直接返回.
     *
     * 假设商品数量有很多呢?
     * 分布式锁会导致同时只能有一个线程去消费, 速度被限制住了.
     * 针对这种场景可以考虑将秒杀商品分块, 比如总共有100个商品, 将商品分成10份每份10个,
     * 每份使用不同的分布式锁控制同步, 那么消费的速度也就提升上去了.
     *
     * @param lockSupplier 提供具体锁的实现
     * @throws Exception
     */
    protected void testSecKill(Supplier<Lock> lockSupplier) throws Exception {
        logger.info("SecKill test begin use lock={}", lockSupplier.get());
        // 成功率
        BigDecimal rate = BigDecimal.valueOf(goodsNum).multiply(new BigDecimal("100.00")).divide(
                BigDecimal.valueOf(nodeNum).multiply(BigDecimal.valueOf(requestNum)), 2, BigDecimal.ROUND_HALF_UP);

        // 秒杀开始时间, x秒后开始
        long xs = 5L;
        long beginTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(xs);

        CountDownLatch goodsCountDownLatch = new CountDownLatch(goodsNum);
        CountDownLatch countDownLatch = new CountDownLatch(nodeNum);
        for (int n = 1; n <= nodeNum; n++) {
            run(() -> {
                try {
                    // 每个节点另起多个线程, 使用不同的分布式锁实例
                    doSecKill(requestNum, beginTime, goodsCountDownLatch, lockSupplier);
                    countDownLatch.countDown();
                } catch (Exception e) {
                    logger.error("Do SecKill run error", e);
                }
            }, "node-" + n);
        }

        while (goods > 0) {
            goodsCountDownLatch.await();
        }
        // 秒杀抢购结束, 商品已被秒完
        long secKillTime = System.currentTimeMillis() - beginTime;
        logger.info("Goods has been secKill! The success rate={}% cost {}ms.", rate, secKillTime);
        logger.info("Wait for processing the rest of requests...");

        // 等待堆积的请求处理完毕
        countDownLatch.await();
        long costTime = System.currentTimeMillis() - beginTime;
        logger.info("SecKill test end success rate={}% cost {}ms total request num={}.", rate, costTime,
                nodeNum * requestNum);

        // 总共抢到的商品数
        int actGoodsNum = luckyDogs.values().stream().mapToInt((num) -> num.intValue()).sum();
        logger.info("SecKill end the actual goods num={} use lock={} and congratulations the lucky dogs! {}",
                actGoodsNum, lockSupplier.get(), getLukyNames());

        // 断言
        Assert.assertEquals(goodsNum, actGoodsNum);
        Assert.assertEquals(0, goods);
    }

    /**
     * 模拟单个服务节点并发请求秒杀商品.
     * 分布式锁使用公平模式可以让同个线程只能秒杀成功1次, 因为在秒杀成功后想再秒杀需要排队
     * (但有时测试会有秒杀多个的场景, 因为一个来回太快然后还排在第一位, 使用公平锁后一个线程秒杀多个的场景会很少出现);
     * 非公平模式下可能会出现同个线程秒杀获得多个商品的情况(可在最终秒杀成功者日志中看到).
     *
     * @param requestNum 秒杀并发请求线程数
     * @param beginTime 秒杀开始时间
     * @param goodsCountDownLatch 秒杀成功通知
     * @throws Exception
     */
    private void doSecKill(int requestNum, long beginTime, CountDownLatch goodsCountDownLatch,
            Supplier<Lock> lockSupplier) throws Exception {
        Lock lock = lockSupplier.get();

        CountDownLatch countDownLatch = new CountDownLatch(requestNum);
        for (int n = 1; n <= requestNum; n++) {
            run(() -> {
                try {
                    // 等待秒杀活动开始
                    sleep(beginTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

                    // 不停尝试秒杀商品直到商品被秒完为止
                    while (hasGoods) {
                        // 分布式锁控制同步, 否则会导致商品超发; 线程在timeout秒后获取锁超时, 检查是否还有商品, 若无商品则无需再抢直接返回失败
                        long timeout = 5L;
                        if (lock.tryLock(timeout, TimeUnit.SECONDS)) {
                            try {
                                // 没有商品了
                                if (!hasGoods) {
                                    break;
                                }
                                // 获取当前商品数
                                int goodsNum = goods;
                                if (goodsNum <= 0) {
                                    // 没有商品了, 更新缓存标志, 没必要再抢了
                                    hasGoods = false;
                                    break;
                                }
                                // 成功抢到商品, 商品存量减少
                                goods = goodsNum - 1;
                            } finally {
                                lock.unlock();
                            }
                            goodsCountDownLatch.countDown();
                            // 成功抢到商品, 登记获得商品数
                            registerName(Thread.currentThread().getName());
                        }
                    }

                    countDownLatch.countDown();
                } catch (Exception e) {
                    logger.error("SecKill error", e);
                }
            }, Thread.currentThread().getName() + "-thread-" + n);
        }

        // 秒杀请求线程准备完毕
        logger.info("SecKill {} ready!", Thread.currentThread().getName());
        sleep(beginTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        // 秒杀开始
        logger.info("SecKill begin!");
        countDownLatch.await();
    }

    /**
     * 成功抢到了商品, 登记获得的商品数
     */
    private void registerName(String name) {
        if (luckyDogs.containsKey(name)) {
            luckyDogs.put(name, luckyDogs.get(name) + 1);
        } else {
            luckyDogs.put(name, 1);
        }
    }

    /**
     * 获取成功抢到商品的名单和对应抢到的商品数
     *
     * @return
     */
    private String getLukyNames() {
        StringBuilder sb = new StringBuilder();
        sb.append(LINE_SEPARATOR);
        for (Entry<String, Integer> name : luckyDogs.entrySet()) {
            sb.append("[ ").append(String.format("%18s", name.getKey())).append(" ]").append(" secKill ")
                    .append(name.getValue()).append(" goods!").append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

}

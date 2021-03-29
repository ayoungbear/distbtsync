/**
 * Copyright 2021 yangzexiong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ayoungbear.distbtsync.spring.redis;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import com.ayoungbear.distbtsync.BaseRedisTest;
import com.ayoungbear.distbtsync.BaseSpringTest;
import com.ayoungbear.distbtsync.redis.lock.RedisLock;
import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.RedisLockOperation;
import com.ayoungbear.distbtsync.spring.SyncFailureException;

/**
 * 同步注解测试上下文基础配置类.
 * 
 * @author yangzexiong
 */
@TestPropertySource(properties = { "spring.redis.cluster.nodes=" + BaseRedisTest.HOST_AND_PORT,
        "spring.redis.timeout=60000", "spring.redis.jedis.pool.maxActive=2000",
        "spring.redis.lettuce.pool.maxActive=2000" })
@TestPropertySource(properties = { "classKey=class-key", "name=bear",
        "waitTime=" + BaseRedisSyncAnnotationTest.WAIT_TIME,
        "ayoungbear.distbtsync.spring.redis.defaultLeaseTime=" + BaseRedisSyncAnnotationTest.DEFAULT_LEASE_IME })
@Import({ RedisAutoConfiguration.class })
public abstract class BaseRedisSyncAnnotationTest extends BaseSpringTest {

    public static final int WAIT_TIME = 4000;

    public static final int DEFAULT_LEASE_IME = 10000;

    @Autowired
    private RedisSyncService redisSyncService;
    @Autowired
    private RedisSyncOnClassService redisSyncOnClassService;
    @Autowired
    private RedisSyncOnInterface redisSyncOnInterface;
    @Autowired
    private RedisSyncInterface redisSyncInterface;

    @Autowired
    protected RedisTemplate<String, String> stringRedisTemplate;

    /**
     * 测试同步注解过期时间设置.
     */
    @Test
    public void testRedisSyncAnnotationExpired() throws Exception {
        String key = RedisSyncService.class.getDeclaredMethod("testExpired", String.class).toGenericString();
        clear(key);
        Assert.assertEquals(false, exist(key));
        // 进入同步方法后, 加锁成功, key被占用
        Assert.assertEquals(true, redisSyncService.testExpired(key));
        // key过期
        Assert.assertEquals(false, exist(key));
    }

    /**
     * 测试同步注解等待阻塞.
     * @throws Exception
     */
    @Test
    public void testRedisSyncAnnotationWait() throws Exception {
        String key = RedisSyncService.class.getDeclaredMethod("testWait", String.class).toGenericString();
        clear(key);
        Assert.assertEquals(false, exist(key));
        stringRedisTemplate.opsForHash().put(key, key, key);
        stringRedisTemplate.expire(key, 3, TimeUnit.SECONDS);
        // 进入同步方法后, key被占用, 加锁时等待
        long begin = System.currentTimeMillis();
        Assert.assertEquals(true, redisSyncService.testWait(key));
        long time = System.currentTimeMillis() - begin;
        Assert.assertEquals("wait time is " + time + " ms", true, 3000 - 100 <= time && time <= WAIT_TIME + 100);
    }

    /**
     * 测试同步注解等待超时.
     * @throws Exception
     */
    @Test(expected = SyncFailureException.class)
    public void testRedisSyncAnnotationWaitTimeout() throws Exception {
        String key = RedisSyncService.class.getDeclaredMethod("testWaitTimeout", String.class).toGenericString();
        clear(key);
        Assert.assertEquals(false, exist(key));
        stringRedisTemplate.opsForHash().put(key, key, key);
        stringRedisTemplate.expire(key, WAIT_TIME * 2, TimeUnit.MILLISECONDS);
        // 进入同步方法后, key被占用, 加锁时等待
        long begin = System.currentTimeMillis();
        try {
            redisSyncService.testWaitTimeout(key);
        } catch (Exception e) {
            long time = System.currentTimeMillis() - begin;
            Assert.assertEquals("wait time is " + time + " ms", true, WAIT_TIME <= time);
            throw e;
        }
    }

    /**
     * 测试同步注解指定加锁使用的key.
     * @throws Exception
     */
    @Test
    public void testRedisSyncAnnotationName() throws Exception {
        String key = "key";
        String lockKey = "my_name_is_bear_" + key;
        clear(lockKey);
        Assert.assertEquals(false, exist(lockKey));
        Assert.assertEquals(true, redisSyncService.testName(key));
    }

    /**
     * 测试同步注解添加在类上, 类中所有方法都同步执行.
     */
    @Test
    public void testRedisSyncAnnotationOnClass() throws Exception {
        RedisSyncOnClassService.count = 0;
        clear("my-class-key");
        int nodeNum = 10;
        int threadNum = 100;
        concurrentExecute(nodeNum, threadNum, () -> redisSyncOnClassService.syncAddOne(),
                () -> redisSyncOnClassService.syncAddTwo());
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum * 3, RedisSyncOnClassService.count);
    }

    /**
     * 测试同步注解添加在接口上, 则实现类在执行接口的所有方法时都会同步执行.
     */
    @Test
    public void testRedisSyncAnnotationOnInterfaceClass() throws Exception {
        RedisSyncOnInterfaceImpl.count = 0;
        clear("class-" + RedisSyncOnInterfaceImpl.class.toString());
        int nodeNum = 10;
        int threadNum = 100;
        concurrentExecute(nodeNum, threadNum, () -> redisSyncOnInterface.syncAddOne(),
                () -> redisSyncOnInterface.syncAddTwo());
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum * 3, RedisSyncOnInterfaceImpl.count);
    }

    /**
     * 测试同步注解添加在方法上, 方法同步执行.
     */
    @Test
    public void testRedisSyncAnnotationOnMethod() throws Exception {
        RedisSyncService.count = 0;
        clear(RedisSyncService.class.getDeclaredMethod("syncAdd").toGenericString());
        int nodeNum = 10;
        int threadNum = 100;
        concurrentExecute(nodeNum, threadNum, () -> redisSyncService.syncAdd());
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum, RedisSyncService.count);
    }

    /**
     * 同步对照测试.
     */
    // @Test
    public void testRedisSyncAnnotationOnMethodFail() throws Exception {
        RedisSyncService.count = 0;
        int nodeNum = 10;
        int threadNum = 1000;
        int count = 10;
        concurrentExecute(nodeNum, threadNum, () -> {
            for (int i = 0; i < count; i++) {
                redisSyncService.add();
            }
        });
        // 断言最终计数的准确性
        Assert.assertNotEquals(nodeNum * threadNum * count, RedisSyncService.count);
    }

    /**
     * 测试默认过期时间设置.
     */
    @Test
    public void testRedisSyncAnnotationDefaultLeaseTime() throws Exception {
        RedisSyncService.count = 0;
        Semaphore semaphore = new Semaphore(0);
        Thread thread = run(() -> redisSyncService.mutext(semaphore));
        semaphore.acquire();
        long begin = System.currentTimeMillis();
        // 自动过期后加锁成功 10s
        redisSyncService.mutext(null);
        long time = System.currentTimeMillis() - begin;
        // 实际耗时比过期时间要长
        Assert.assertEquals("wait time is " + time + " ms", true,
                DEFAULT_LEASE_IME - 500 <= time && time <= DEFAULT_LEASE_IME + 1500);
        thread.join();
    }

    /**
     * 测试不同的同步方法同时调用, 使用不同的key, 不会互相影响.
     */
    @Test
    public void testRedisSyncAnnotationUseMultipleKey() throws Exception {
        RedisSyncService.KEY1 = 0;
        RedisSyncService.KEY2 = 0;
        int nodeNum = 10;
        int threadNum = 100;
        int count = 10;
        concurrentExecute(nodeNum, threadNum, () -> {
            for (int i = 0; i < count; i++) {
                if ((i & 1) == 1) {
                    redisSyncService.syncAddkey1();
                } else {
                    redisSyncService.syncAddkey2();
                }
            }
        });
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum * count / 2, RedisSyncService.KEY1);
        Assert.assertEquals(nodeNum * threadNum * count / 2, RedisSyncService.KEY2);
    }

    /**
     * 测试不同的同步方法同时调用, 使用不同的key, 不会互相影响.
     */
    @Test
    public void testRedisSyncAnnotationConcurrentUseMultipleKey() throws Exception {
        RedisSyncService.KEY1 = 0;
        RedisSyncService.KEY2 = 0;
        RedisSyncService.KEY3 = 0;
        RedisSyncService.KEY4 = 0;
        int nodeNum = 10;
        int threadNum = 1000;
        concurrentExecute(nodeNum, threadNum, () -> {
            redisSyncService.syncAddkey1();
            redisSyncService.syncAddkey2();
            redisSyncService.syncAddkey3();
            redisSyncService.syncAddkey4();
        });
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum, RedisSyncService.KEY1);
        Assert.assertEquals(nodeNum * threadNum, RedisSyncService.KEY2);
        Assert.assertEquals(nodeNum * threadNum, RedisSyncService.KEY3);
        Assert.assertEquals(nodeNum * threadNum, RedisSyncService.KEY4);
    }

    /**
     * 测试同步注解添加在接口方法上, 接口实现类在执行接口方法时会同步执行.
     */
    @Test
    public void testRedisSyncAnnotationOnInterfaceMethod() throws Exception {
        RedisSyncInterfaceImpl.count = 0;
        clear(RedisSyncInterfaceImpl.class.getDeclaredMethod("syncAdd").toGenericString());
        int nodeNum = 10;
        int threadNum = 100;
        concurrentExecute(nodeNum, threadNum, () -> redisSyncInterface.syncAdd());
        // 断言最终计数的准确性
        Assert.assertEquals(nodeNum * threadNum, RedisSyncInterfaceImpl.count);
    }

    protected boolean clear(String key) {
        return stringRedisTemplate.delete(key);
    }

    protected boolean exist(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 测试配置类.
     * 
     * @author yangzexiong
     */
    public static class BaseRedisSyncAnnotationTestConfiguration {

        @Bean
        public RedisSyncService redisSyncService() {
            return new RedisSyncService();
        }

        @Bean
        public RedisSyncOnClassService redisSyncOnClassService() {
            return new RedisSyncOnClassService();
        }

        @Bean
        public RedisSyncOnInterface redisSyncOnInterface() {
            return new RedisSyncOnInterfaceImpl();
        }

        @Bean
        public RedisSyncInterface redisSyncInterface() {
            return new RedisSyncInterfaceImpl();
        }
    }

    /**
     * 自定义同步器提供者.
     * 
     * @author yangzexiong
     */
    public static class MyRedisSynchronizerProvider extends RedisLockSynchronizerProvider {

        public MyRedisSynchronizerProvider(RedisLockCommands commands) {
            super(commands);
        }

        @Override
        public RedisSynchronizer getSynchronizer(RedisSyncAttribute attribute) {
            RedisLock lock = getRedisLock(attribute.getKey());
            RedisLockOperation lockOperation = determineLockOperation(attribute);
            return new MyRedisSynchronizer(lock, lockOperation);
        }

    }

    /**
     * 自定义同步器, 在同步执行前后打印日志方便测试时观察同步执行情况.
     * 
     * @author yangzexiong
     */
    public static class MyRedisSynchronizer extends RedisLockSynchronizer {
        private static final Logger logger = LoggerFactory.getLogger(MyRedisSynchronizer.class);

        public MyRedisSynchronizer(RedisLock lock, RedisLockOperation operation) {
            super(lock, operation);
        }

        @Override
        public boolean acquire() {
            boolean result = super.acquire();
            logger.info("Acquire key '{}'", getKey());
            return result;
        }

        @Override
        public boolean release() {
            logger.info("Release key '{}'", getKey());
            return super.release();
        }
    }

    /**
     * 测试同步注解属性设置工具类.
     * 
     * @author yangzexiong
     */
    public static class RedisSyncService {

        protected final Logger logger = LoggerFactory.getLogger(getClass());

        static int count = 0;

        static int KEY1 = 0;
        static int KEY2 = 0;
        static int KEY3 = 0;
        static int KEY4 = 0;

        @Autowired
        protected RedisTemplate<String, String> stringRedisTemplate;

        @RedisSync(leaseTime = "${leaseTime:3}", timeUnit = TimeUnit.SECONDS)
        public boolean testExpired(String key) {
            boolean result = stringRedisTemplate.hasKey(key);
            sleep(4);
            return result;
        }

        @RedisSync(waitTime = "${waitTime}")
        public boolean testWait(String key) {
            return stringRedisTemplate.hasKey(key);
        }

        @RedisSync(waitTime = "${waitTime}")
        public boolean testWaitTimeout(String key) {
            return stringRedisTemplate.hasKey(key);
        }

        @RedisSync("my_name_is_${name}_#{#key}")
        public boolean testName(String key) {
            return stringRedisTemplate.hasKey("my_name_is_bear_" + key);
        }

        /**
         * 在方法上添加分布式同步注解, 自动根据方法名设置key, 实现方法的同步执行.
         */
        @RedisSync
        public void syncAdd() {
            count = count + 1;
            logger.info("Add end count={}", count);
        }

        /**
         * 非同步情况下对照测试用方法.
         */
        public void add() {
            count = count + 1;
            logger.info("Add end count={}", count);
        }

        @RedisSync
        public void mutext(Semaphore semaphore) {
            if (semaphore != null) {
                semaphore.release();
                sleep(TimeUnit.MILLISECONDS.toSeconds(DEFAULT_LEASE_IME + 1000));
            }
        }

        @RedisSync("KEY1_#{#methodName}")
        public void syncAddkey1() {
            KEY1 = KEY1 + 1;
            logger.info("Add end KEY1 count={}", KEY1);
        }

        @RedisSync("KEY2_#{#methodName}")
        public void syncAddkey2() {
            KEY2 = KEY2 + 1;
            logger.info("Add end KEY2 count={}", KEY2);
        }

        @RedisSync("KEY3_#{#methodName}")
        public void syncAddkey3() {
            KEY3 = KEY3 + 1;
            logger.info("Add end KEY3 count={}", KEY3);
        }

        @RedisSync("KEY4_#{#methodName}")
        public void syncAddkey4() {
            KEY4 = KEY4 + 1;
            logger.info("Add end KEY4 count={}", KEY4);
        }

    }

    /**
     * 在类上添加分布式同步注解, 该类下的所有方法都会同步执行.
     * 此处设置手工了一个公共的字符串+属性占位符标志, 表示该类方法公用一个key进行同步,
     * 如果不手工设置那么则默认根据方法名设置key进行同步, 这样在并发执行两个方法时实际
     * 并没有同步(线程1执行 syncAddOne 的同时, 线程2也可以执行 syncAddTwo), 断言会失败.
     * 使用默认自动或者方法名作为key, 并不能实现所有方法的同步, 例如:
     * @RedisSync("my-#{#methodName}")
     */
    @RedisSync("my-${classKey}")
    public static class RedisSyncOnClassService {
        protected final Logger logger = LoggerFactory.getLogger(getClass());

        static int count = 0;

        public void syncAddOne() {
            count = count + 1;
            logger.info("Add one end count={}", count);
        }

        public void syncAddTwo() {
            count = count + 2;
            logger.info("Add two end count={}", count);
        }
    }

    /**
     * 同步注解添加在接口上, 则接口实现类在执行该接口方法时会同步执行.
     */
    @RedisSync("class-#{#targetClass.toString()}")
    public static interface RedisSyncOnInterface {
        void syncAddOne();

        void syncAddTwo();
    }

    /**
     * 虽然同步注解是标记在接口类上, 但是实际同步时默认使用的key是实现类的类名, 而不是接口的类名.
     */
    public static class RedisSyncOnInterfaceImpl implements RedisSyncOnInterface {
        protected final Logger logger = LoggerFactory.getLogger(getClass());

        static int count = 0;

        @Override
        public void syncAddOne() {
            count = count + 1;
            logger.info("Add one end count={}", count);
        }

        @Override
        public void syncAddTwo() {
            count = count + 2;
            logger.info("Add two end count={}", count);
        }
    }

    /**
     * 在接口上添加注解, 实现类在执行该接口方法时也会同步执行.
     */
    public static interface RedisSyncInterface {
        @RedisSync
        void syncAdd();
    }

    /**
     * 虽然同步注解是标记在接口方法上, 但是实际同步时默认使用的key是实现类中的方法名, 而不是接口中的方法名.
     */
    public static class RedisSyncInterfaceImpl implements RedisSyncInterface {
        protected final Logger logger = LoggerFactory.getLogger(getClass());

        static int count = 0;

        @Override
        public void syncAdd() {
            count = count + 1;
            logger.info("Add one end count={}", count);
        }
    }

}

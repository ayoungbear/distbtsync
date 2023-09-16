/**
 * Copyright 2021 yangzexiong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ayoungbear.distbtsync.spring.starter.redis;

import com.github.ayoungbear.distbtsync.spring.redis.RedisSync;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 测试 Spring boot 自动配置功能.
 *
 * @author yangzexiong
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RedisSyncAutoConfigurationTest.RedisSyncApplication.class, properties = {
        "spring.redis.cluster.nodes=192.168.42.217:6379,192.168.42.217:6380,192.168.42.217:6381,192.168.42.217:6382,192.168.42.217:6383,192.168.42.217:6384",
        "spring.redis.timeout=60000", "spring.redis.jedis.pool.maxActive=2000",
        "spring.redis.lettuce.pool.maxActive=2000", "ayoungbear.distbtsync.spring.redis.defaultLeaseTime=10000"})
public class RedisSyncAutoConfigurationTest {

    @Autowired
    private RedisSyncService redisSyncService;

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 100, 10, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    @Test
    public void test() throws Exception {
        int tNum = 10;
        int spin = 1000;
        CountDownLatch latch = new CountDownLatch(tNum);
        for (int i = 0; i < tNum; i++) {
            executor.execute(() -> {
                try {
                    for (int j = 0; j < spin; j++) {
                        redisSyncService.add();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        Assert.assertEquals(tNum * spin, RedisSyncService.count);
    }

    @SpringBootApplication
    public static class RedisSyncApplication {

        public static void main(String[] args) {
            SpringApplication.run(RedisSyncApplication.class, args);
        }

        @Bean
        public RedisSyncService redisSyncService() {
            return new RedisSyncService();
        }

    }

    public static class RedisSyncService {

        private static final Logger logger = LoggerFactory.getLogger(RedisSyncService.class);
        static int count = 0;

        @RedisSync
        public void add() {

            count = count + 1;
            logger.info("add count={}", count);
        }
    }

}

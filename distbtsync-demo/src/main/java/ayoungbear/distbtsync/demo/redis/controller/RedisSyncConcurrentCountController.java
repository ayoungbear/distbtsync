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
package ayoungbear.distbtsync.demo.redis.controller;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ayoungbear.distbtsync.demo.config.GetRequestPublishConfiguration.Publish;
import ayoungbear.distbtsync.demo.redis.BaseRedisSupport;
import ayoungbear.distbtsync.demo.redis.RedisSyncTestUtils;
import ayoungbear.distbtsync.demo.redis.service.RedisSyncConcurrentCountService;

/**
 * 模拟测试分布式场景下并发计数请求触发接口.
 * 
 * @author yangzexiong
 */
@RestController
@RequestMapping("redis-sync/concurrent-count")
public class RedisSyncConcurrentCountController extends BaseRedisSupport {

    private static final Logger logger = LoggerFactory.getLogger(RedisSyncConcurrentCountController.class);

    private RedisSyncConcurrentCountService redisSyncConcurrentCountService;

    /**
     * 测试分布式场景下的多线程并发计数.
     * 会自动广播给其他端口的本地服务, 模拟分布式不同服务实例并发调用的情况.
     * @param num 增加的数值
     */
    @Publish
    @GetMapping("test")
    public void testConcurrentCount(long num) {
        logger.info("concurrentCount num={}", num);
        if (num <= 0) {
            return;
        }
        // 先记录真实值
        increment(RedisSyncTestUtils.CONCURRENT_COUNT_KEY,
                RedisSyncTestUtils.CONCURRENT_COUNT_ACTUAL_KEY, num);
        expire(RedisSyncTestUtils.CONCURRENT_COUNT_KEY);
        Executors.newSingleThreadExecutor();
        // 然后多线程并发调用计数
        for (long i = 0; i < num; i++) {
            redisSyncConcurrentCountService.concurrentAdd();
        }
    }

    /**
     * 校验分布式并发计数的结果是否准确, 以验证分布式场景下的并发控制情况.
     */
    @GetMapping("verify")
    public void verifyConcurrentCount() {
        // 获取真实值
        long actualNum = getLongValue(RedisSyncTestUtils.CONCURRENT_COUNT_KEY,
                RedisSyncTestUtils.CONCURRENT_COUNT_ACTUAL_KEY);
        // 获取计数值
        long countNum = getLongValue(RedisSyncTestUtils.CONCURRENT_COUNT_KEY,
                RedisSyncTestUtils.CONCURRENT_COUNT_COUNT_KEY);
        logger.info("verifyConcurrentCount equals={} actualNum={} countNum={}", actualNum == countNum, actualNum,
                countNum);
    }

    /**
     * 清除分布式并发计数的结果.
     */
    @GetMapping("clear")
    public void clearConcurrentCount() {
        logger.info("clearConcurrentCount clear={}", delete(RedisSyncTestUtils.CONCURRENT_COUNT_KEY));
    }

    @Autowired
    public void setRedisSyncConcurrentCountService(RedisSyncConcurrentCountService redisSyncConcurrentCountService) {
        this.redisSyncConcurrentCountService = redisSyncConcurrentCountService;
    }

}

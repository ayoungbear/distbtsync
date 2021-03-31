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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ayoungbear.distbtsync.demo.config.GetRequestPublishConfiguration.Publish;
import ayoungbear.distbtsync.demo.redis.BaseRedisSupport;
import ayoungbear.distbtsync.demo.redis.RedisSyncTestUtils;
import ayoungbear.distbtsync.demo.redis.service.RedisSyncAccountAmountService;
import ayoungbear.distbtsync.demo.redis.service.RedisSyncAccountAmountService.Account;

/**
 * 模拟测试分布式场景下给各账户增加资金的触发接口.
 * 
 * @author yangzexiong
 */
@RestController
@RequestMapping("redis-sync/account-amount")
public class RedisSyncAccountAmountController extends BaseRedisSupport {

    private static final Logger logger = LoggerFactory.getLogger(RedisSyncAccountAmountController.class);

    private RedisSyncAccountAmountService redisSyncAccountAmountService;

    /**
     * 账户数量.
     */
    private int accountNum = 10;
    /**
     * 随机账户号起始值.
     */
    private int accountIdBegin = 1000000;

    /**
     * 测试分布式场景下的多线程并发给各账户增加资金数. 
     * 会自动广播给其他端口的本地服务, 模拟分布式不同服务实例并发调用的情况.
     * @param spin 随机选择账户增加随机金额的循环次数
     */
    @Publish
    @GetMapping("test")
    public void testAccountAmount(long spin) {
        logger.info("addAccountAmount accountNum={} spin={}", accountNum, spin);
        for (int i = 0; i < spin; i++) {
            // 随机选择账户
            String accountId = String.valueOf(RandomUtils.nextInt(accountIdBegin, accountIdBegin + accountNum + 1));
            // 随机生成增加的金额数
            BigDecimal amount = BigDecimal.valueOf(RandomUtils.nextDouble(0.01d, 123.45d)).setScale(2,
                    BigDecimal.ROUND_HALF_UP);
            // 先记录真实值
            redisSyncAccountAmountService.increment(accountId, amount);
            // 随机使用增加的方式 账户号 or 对象, 两种都会根据账户号进行同步
            if (RandomUtils.nextBoolean()) {
                redisSyncAccountAmountService.addAccountAmount(accountId, amount);
            } else {
                redisSyncAccountAmountService.addAccountAmount(new Account(accountId, amount));
            }
        }
    }

    /**
     * 校验结果是否准确, 以验证分布式场景下的并发控制情况.
     */
    @GetMapping("verify")
    public void verifyAccountAmount() {
        // 真实值
        Map<Object, Object> actualAccounts = stringRedisTemplate.opsForHash()
                .entries(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_ACTUAL_KEY);
        // 计算值
        Map<Object, Object> countAccounts = stringRedisTemplate.opsForHash()
                .entries(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_COUNTT_KEY);
        // 校验结果
        Map<String, String> result = new HashMap<String, String>(actualAccounts.size());
        actualAccounts.forEach((key, vavlue) -> {
            String accountId = String.valueOf(key);
            // 真实值
            BigDecimal amountTotal = new BigDecimal(String.valueOf(vavlue)).setScale(2, BigDecimal.ROUND_HALF_UP);
            // 获取计算值
            BigDecimal countTotal = Optional.ofNullable(countAccounts.get(key))
                    .map((v) -> new BigDecimal(String.valueOf(v))).orElse(BigDecimal.ZERO)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            String msg = (amountTotal.compareTo(countTotal) == 0) + " actual=" + amountTotal + " count=" + countTotal;
            result.put(accountId, msg);
        });
        countAccounts.forEach((key, vavlue) -> {
            if (!result.containsKey(key)) {
                result.put(String.valueOf(key), "false none");
            }
        });

        logger.info("verifyAccountAmount result={}",
                result.entrySet().stream().map((entry) -> entry.getKey() + ": " + entry.getValue())
                        .<StringJoiner> collect(() -> {
                            return new StringJoiner(System.getProperty("line.separator")).add("");
                        }, StringJoiner::add, (t, u) -> {
                        }));
    }

    /**
     * 清除结果.
     */
    @GetMapping("clear")
    public void clearAccountAmount() {
        logger.info("clearAccountAmount clear={}", delete(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_ACTUAL_KEY,
                RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_COUNTT_KEY));
    }

    @Autowired
    public void setRedisSyncAccountAmountService(RedisSyncAccountAmountService redisSyncAccountAmountService) {
        this.redisSyncAccountAmountService = redisSyncAccountAmountService;
    }

}

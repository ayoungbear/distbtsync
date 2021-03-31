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
package ayoungbear.distbtsync.demo.redis.service;

import java.math.BigDecimal;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ayoungbear.distbtsync.spring.redis.RedisSync;

import ayoungbear.distbtsync.demo.redis.BaseRedisSupport;
import ayoungbear.distbtsync.demo.redis.RedisSyncTestUtils;

/**
 * 模拟测试分布式场景下并发给各账户增加资金数的功能, 这里用redis作为数据库.
 * 各个账户之间不会互相阻塞影响, 但分布式服务中各线程如果操作的是同个账户那将会触发同步.
 * 
 * @author yangzexiong
 */
@Service
public class RedisSyncAccountAmountService extends BaseRedisSupport {

    private static final Logger logger = LoggerFactory.getLogger(RedisSyncAccountAmountService.class);

    private Executor executor = Executors.newSingleThreadExecutor();

    /**
     * 异步调用方式, 给指定的账户号 id 增加 amount 金额数, 这里直接接收账户号.
     * 这里根据账户号来进行同步, 不同账户之间不会互相影响, 但对同一账户的操作将会同步.
     * 这里同步使用的 key 是 SpEL 表达式, 最终解析的结果为 'test_addAccountAmount_{id}'. 
     * 这里账户号也可以换成 '#p1' 或者 '#a1' 或者 '#args[0]', 例如 "test_#{#methodName}_#{#a1}"
     * @param accountId
     * @param amount
     */
    @Async
    @RedisSync("test_#{#methodName}_#{#accountId}")
    public void addAccountAmount(String accountId, BigDecimal amount) {
        // 先获取数据
        BigDecimal amountTotal = getAsBigDecimal(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_COUNTT_KEY, accountId);
        // 增加
        amountTotal = amountTotal.add(amount);
        logger.info("addAccountAmount accountId={} amountTotal={}", accountId, amountTotal);
        // 保存回数据库
        stringRedisTemplate.opsForHash().put(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_COUNTT_KEY, accountId,
                amountTotal.toString());
    }

    /**
     * 异步调用方式, 给指定的账户号 id 增加 amount 金额数, 这里接收账户对象作为参数.
     * 这里根据账户号来进行同步, 不同账户之间不会互相影响, 但对同一账户的操作将会同步.
     * 这里同步使用的 key 是 SpEL 表达式, 最终解析的结果为 'test_addAccountAmount_{id}'. 
     * 这里账户号也可以换成 "test_#{#methodName}_#{#account.getAccountId()}"
     * @param accountId
     * @param amount
     */
    @Async
    @RedisSync("test_#{#methodName}_#{#account.accountId}")
    public void addAccountAmount(Account account) {
        String accountId = account.getAccountId();
        BigDecimal amount = account.getAmount();
        // 先获取数据
        BigDecimal amountTotal = getAsBigDecimal(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_COUNTT_KEY, accountId);
        // 增加
        amountTotal = amountTotal.add(amount);
        logger.info("addAccountAmount accountId={} amountTotal={}", accountId, amountTotal);
        // 保存回数据库
        stringRedisTemplate.opsForHash().put(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_COUNTT_KEY, accountId,
                amountTotal.toString());
    }

    /**
     * 记录真实值
     * @param accountId
     * @param amount
     */
    public void increment(String accountId, BigDecimal amount) {
        executor.execute(
                () -> increment(RedisSyncTestUtils.ACCOUNT_AMOUNT_PREFIX_ACTUAL_KEY, accountId, amount.doubleValue()));
    }

    /**
     * 账户.
     * 
     * @author yangzexiong
     */
    public static class Account {
        /**
         * 账户号
         */
        private String accountId;
        /**
         * 增加的金额数
         */
        private BigDecimal amount;

        public Account(String accountId, BigDecimal amount) {
            this.accountId = accountId;
            this.amount = amount;
        }

        /**
         * @return the accountId
         */
        public String getAccountId() {
            return accountId;
        }

        /**
         * @param accountId the accountId to set
         */
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        /**
         * @return the amount
         */
        public BigDecimal getAmount() {
            return amount;
        }

        /**
         * @param amount the amount to set
         */
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

    }

}

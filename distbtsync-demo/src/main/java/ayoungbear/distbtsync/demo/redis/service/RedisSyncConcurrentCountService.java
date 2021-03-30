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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.ayoungbear.distbtsync.spring.redis.RedisSync;

import ayoungbear.distbtsync.demo.redis.RedisSyncTestUtils;

/**
 * 测试 @{@link RedisSync} 注解分布式并发计数场景.
 * 
 * @author yangzexiong
 */
@Service
public class RedisSyncConcurrentCountService {

    private static final Logger logger = LoggerFactory.getLogger(RedisSyncConcurrentCountService.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 测试分布式场景下的并发计数, 这里使用 redis 来作为数据库.
     * 这里同步使用的 key 是 SpEL 表达式, 最终解析的结果为 'test_RedisSyncConcurrentCountService_concurrentAdd'.
     */
    @RedisSync("test_#{#targetClass.getSimpleName()}_#{#methodName}")
    public void concurrentAdd() {
        // 先获取数据
        long count = RedisSyncTestUtils.getLongValue(stringRedisTemplate, RedisSyncTestUtils.CONCURRENT_COUNT_KEY,
                RedisSyncTestUtils.CONCURRENT_COUNT_COUNT_KEY);
        // 自增
        count = count + 1;
        logger.info("concurrentAdd count={}", count);
        // 保存回数据库
        stringRedisTemplate.opsForHash().put(RedisSyncTestUtils.CONCURRENT_COUNT_KEY,
                RedisSyncTestUtils.CONCURRENT_COUNT_COUNT_KEY, String.valueOf(count));
    }


}

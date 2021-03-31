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
package ayoungbear.distbtsync.demo.redis;

/**
 * 测试用工具类.
 * 
 * @author yangzexiong
 */
public class RedisSyncTestUtils {

    /**
     * 测试并发计数统计数据保存用的key
     */
    public static final String CONCURRENT_COUNT_KEY = "testConcurrentAdd";
    /**
     * 测试并发计数统计数据真实值使用的hashkey 使用redis自增的方式
     */
    public static final String CONCURRENT_COUNT_ACTUAL_KEY = "actualNum";
    /**
     * 测试并发计数统计数据真实值使用的hashkey 使用redis自增的方式
     */
    public static final String CONCURRENT_COUNT_COUNT_KEY = "countNum";

    /**
     * 测试分布式场景下账户资金数据保存 真实值用的key hashkey为各账户号
     */
    public static final String ACCOUNT_AMOUNT_PREFIX_ACTUAL_KEY = "testAccountAmount_actual";
    /**
     * 测试分布式场景下账户资金数据保存 计算值用的key hashkey为各账户号
     */
    public static final String ACCOUNT_AMOUNT_PREFIX_COUNTT_KEY = "testAccountAmount_count";

}

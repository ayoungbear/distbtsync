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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * redis 相关操作支持基础类.
 * 
 * @author yangzexiong
 */
public abstract class BaseRedisSupport {

    protected StringRedisTemplate stringRedisTemplate;

    /**
     * hash类型获取数值, 没有则为 0.
     * @param key
     * @param hashkey
     * @return
     */
    public long getLongValue(String key, String hashkey) {
        return Optional.ofNullable(stringRedisTemplate.opsForHash().get(key, hashkey))
                .map((v) -> Long.valueOf(String.valueOf(v))).orElse(0L);
    }

    /**
     * 自增.
     * @param key
     * @param hashkey
     * @param num
     */
    public long increment(String key, String hashkey, long num) {
        return stringRedisTemplate.opsForHash().increment(key, hashkey, num);
    }

    /**
     * 设置测试数据过期时间, 默认为1天.
     * @param key
     */
    public boolean expire(String key) {
        return stringRedisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    /**
     * 删除指定key.
     * @param key
     * @return
     */
    public boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

}

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

package com.github.ayoungbear.distbtsync.redis.lock;

/**
 * redis 加锁操作接口.
 *
 * @author yangzexiong
 */
@FunctionalInterface
public interface RedisLockOperation {

    /**
     * 使用给定的 {@link RedisLock} 加锁.
     * @param lock
     * @return
     */
    boolean doLock(RedisLock lock);

}

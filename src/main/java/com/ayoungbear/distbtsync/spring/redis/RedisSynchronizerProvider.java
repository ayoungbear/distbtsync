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

/**
 * 定义基于 Redis 的同步器提供接口.
 * 
 * @author yangzexiong
 */
@FunctionalInterface
public interface RedisSynchronizerProvider {

    /**
     * 根据给定的同步相关属性 {@code attributes}, 返回相应的同步器 {@link RedisSynchronizer} 实现类.
     * @param attributes
     * @return
     */
    RedisSynchronizer getSynchronizer(RedisSyncAttributes attributes);

}

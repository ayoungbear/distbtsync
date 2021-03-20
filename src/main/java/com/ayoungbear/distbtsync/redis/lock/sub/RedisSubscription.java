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
package com.ayoungbear.distbtsync.redis.lock.sub;

/**
 * 定义 Redis 单频道订阅功能操作接口.
 * 
 * @author yangzexiong
 */
public interface RedisSubscription {

    /**
     * 订阅指定的频道并阻塞线程.
     */
    void subscribe();

    /**
     * 取消订阅.
     */
    void unsubscribe();

    /**
     * 是否处于订阅中.
     * @return
     */
    boolean isSubscribed();

    /**
     * 获取订阅的频道.
     * @return
     */
    String getChannel();

    /**
     * 如果有必要关闭资源则实现该方法.
     */
    default void close() {
    }

}

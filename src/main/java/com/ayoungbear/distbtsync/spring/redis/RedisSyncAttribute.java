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

import java.util.function.Supplier;

/**
 * 同步所需的相关属性, 值已经是解析后的结果.
 * 
 * @author yangzexiong
 * @see RedisSync
 */
public class RedisSyncAttribute {

    /**
     * 同步所用键值
     */
    private String name;
    /**
     * 过期时间(ms)
     */
    private long leaseTimeMillis;
    /**
     * 阻塞等待的最大超时时间(ms)
     */
    private long waitTimeMillis;
    /**
     * 同步异常处理器限定符
     */
    private String handlerQualifier;

    private Supplier<String> handlerQualifierSupplier;

    public static RedisSyncAttribute create() {
        return new RedisSyncAttribute();
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return name;
    }

    public RedisSyncAttribute setName(String name) {
        this.name = name;
        return this;
    }

    public long getLeaseTimeMillis() {
        return leaseTimeMillis;
    }

    public RedisSyncAttribute setLeaseTimeMillis(long leaseTimeMillis) {
        this.leaseTimeMillis = leaseTimeMillis;
        return this;
    }

    public long getWaitTimeMillis() {
        return waitTimeMillis;
    }

    public RedisSyncAttribute setWaitTimeMillis(long waitTimeMillis) {
        this.waitTimeMillis = waitTimeMillis;
        return this;
    }

    public String getHandlerQualifier() {
        if (handlerQualifier == null && handlerQualifierSupplier != null) {
            handlerQualifier = handlerQualifierSupplier.get();
        }
        return handlerQualifier;
    }

    public RedisSyncAttribute setHandlerQualifier(String handlerQualifier) {
        this.handlerQualifier = handlerQualifier;
        return this;
    }

    public RedisSyncAttribute setHandlerQualifier(Supplier<String> handlerQualifierSupplier) {
        this.handlerQualifierSupplier = handlerQualifierSupplier;
        return this;
    }

}

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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 基于 Redis 的同步注解相关配置类.
 * 
 * @author yangzexiong
 */
@ConfigurationProperties(prefix = "ayoungbear.distbtsync.spring.redis")
public class RedisSyncProperties {

    /**
     * 默认的过期时间(ms)
     */
    private Long defaultLeaseTime;
    /**
     * 默认的阻塞等待超时时间(ms)
     */
    private Long defaultWaitTime;

    /**
     * @return the defaultLeaseTime
     */
    public Long getDefaultLeaseTime() {
        return defaultLeaseTime;
    }

    /**
     * @param defaultLeaseTime the defaultLeaseTime to set
     */
    public void setDefaultLeaseTime(Long defaultLeaseTime) {
        this.defaultLeaseTime = defaultLeaseTime;
    }

    /**
     * @return the defaultWaitTime
     */
    public Long getDefaultWaitTime() {
        return defaultWaitTime;
    }

    /**
     * @param defaultWaitTime the defaultWaitTime to set
     */
    public void setDefaultWaitTime(Long defaultWaitTime) {
        this.defaultWaitTime = defaultWaitTime;
    }

}

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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * 分布式同步相关工具类
 * 
 * @author yangzexiong
 */
public class RedisSyncUtils {

    /**
     * 默认过期时间 - 永不过期
     */
    public static final long DEFAULT_LEASE_TIME = -1;

    /**
     * 默认阻塞等待时间 - 持续阻塞直到成功
     */
    public static final long DEFAULT_WAIT_TIME = -1;

    public static final String REDIS_SYNC_POST_PROCESSOR_NAME = "com.ayoungbear.distbtsync.spring.redis.RedisSyncAnnotationPostProcessor";

    /**
     * 根据给定的限定符由 {@linkplain org.springframework.beans.factory.BeanFactory getBean} 获取指定的 bean.
     * @param qualifier
     * @return
     * @see org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils#qualifiedBeanOfType
     */
    public static <T> T getQualifierBean(BeanFactory beanFactory, String qualifier, Class<T> beanClass) {
        if (StringUtils.hasText(qualifier)) {
            if (beanFactory == null) {
                throw new IllegalStateException("BeanFactory must be provided to access qualified bean '" + qualifier
                        + "' of type '" + beanClass.getSimpleName() + "'");
            }
            return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, beanClass, qualifier);
        }
        return null;
    }

}

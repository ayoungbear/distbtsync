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

import org.aopalliance.aop.Advice;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import com.ayoungbear.distbtsync.spring.MethodBasedExpressionResolver;
import com.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;
import com.ayoungbear.distbtsync.spring.aop.AbstractAnnotationPointcutAdvisor;
import com.ayoungbear.distbtsync.spring.support.StringMethodExpressionResolver;

/**
 * 基于 Redis 的分布式注解同步切面, 通过 @{@link RedisSync} 注解激活基于 Redis 实现同步的方法调用,
 * 该注解可以在方法或者类型级别上使用.
 * 
 * @author yangzexiong
 * @see RedisSync
 * @see RedisSyncMethodInterceptor
 * @see StringMethodExpressionResolver
 */
@SuppressWarnings("serial")
public class RedisSyncAnnotationAdvisor extends AbstractAnnotationPointcutAdvisor<RedisSync>
        implements BeanFactoryAware {

    private final RedisSyncMethodInterceptor advice;

    public RedisSyncAnnotationAdvisor(RedisSynchronizerProvider synchronizerProvider,
            @Nullable Supplier<MethodBasedExpressionResolver<String>> exprResolverSupplier,
            @Nullable Supplier<SyncMethodFailureHandler> defaultHandlerSupplier) {
        Assert.notNull(synchronizerProvider, () -> "RedisSynchronizerProvider must not be null");
        this.advice = new RedisSyncMethodInterceptor(synchronizerProvider,
                new SingletonSupplier<>(exprResolverSupplier, StringMethodExpressionResolver::new).obtain(),
                defaultHandlerSupplier);
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        advice.setBeanFactory(beanFactory);
    }

    /**
     * 设置默认情况下的过期时间(ms).
     * @param defaultLeaseTimeMillis the defaultLeaseTimeMillis to set
     */
    public void setDefaultLeaseTimeMillis(long defaultLeaseTimeMillis) {
        advice.setDefaultLeaseTimeMillis(defaultLeaseTimeMillis);
    }

    /**
     * 设置默认情况下的阻塞等待超时时间(ms).
     * @param defaultWaitTimeMillis the defaultWaitTimeMillis to set
     */
    public void setDefaultWaitTimeMillis(long defaultWaitTimeMillis) {
        advice.setDefaultWaitTimeMillis(defaultWaitTimeMillis);
    }

}

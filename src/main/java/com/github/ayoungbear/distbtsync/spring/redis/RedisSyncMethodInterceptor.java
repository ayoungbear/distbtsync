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

package com.github.ayoungbear.distbtsync.spring.redis;

import com.github.ayoungbear.distbtsync.spring.MethodBasedExpressionResolver;
import com.github.ayoungbear.distbtsync.spring.MethodInvoker;
import com.github.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;
import com.github.ayoungbear.distbtsync.spring.Synchronizer;
import com.github.ayoungbear.distbtsync.spring.aop.AbstractSyncInvocationSupport;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 基于 Redis 的同步方法调用拦截器, 根据方法上下文提供相应的 {@link Synchronizer} 来执行同步操作.
 * 需要同步的方法或者方法所属的类需要标记 @{@link RedisSync} 注解, 并指定同步操作相关的属性信息.
 *
 * @author yangzexiong
 * @see RedisSync
 */
public class RedisSyncMethodInterceptor extends AbstractSyncInvocationSupport
        implements MethodBasedExpressionResolver<String> {

    private Map<Method, RedisSync> redisSyncCache = new ConcurrentHashMap<>(256);

    private MethodBasedExpressionResolver<String> exprResolver;

    private RedisSynchronizerProvider synchronizerProvider;

    private long defaultLeaseTimeMillis = RedisSyncUtils.DEFAULT_LEASE_TIME;

    private long defaultWaitTimeMillis = RedisSyncUtils.DEFAULT_WAIT_TIME;

    public RedisSyncMethodInterceptor(RedisSynchronizerProvider synchronizerProvider,
            @Nullable MethodBasedExpressionResolver<String> exprResolver,
            @Nullable SyncMethodFailureHandler defaultHandler) {
        this(synchronizerProvider, exprResolver, () -> defaultHandler);
    }

    public RedisSyncMethodInterceptor(RedisSynchronizerProvider synchronizerProvider,
            @Nullable MethodBasedExpressionResolver<String> exprResolver,
            @Nullable Supplier<SyncMethodFailureHandler> defaultHandlerSupplier) {
        super(defaultHandlerSupplier);
        Assert.notNull(synchronizerProvider, "RedisSynchronizerProvider must not be null");
        this.synchronizerProvider = synchronizerProvider;
        this.exprResolver = exprResolver;
    }

    @Override
    public Synchronizer determineSynchronizer(MethodInvoker methodInvoker) {
        RedisSyncAttributes redisSyncAttributes = resolveRedisSyncAttributes(methodInvoker);
        // 提供同步相关的属性, 将获取同步器的实际操作委托给提供者实现
        RedisSynchronizer redisSynchronizer = synchronizerProvider.getSynchronizer(redisSyncAttributes);
        if (redisSynchronizer == null) {
            throw new IllegalStateException(
                    "Synchronizer is required for method '" + methodInvoker.getMethodDescription() + "'");
        }
        return redisSynchronizer;
    }

    @Override
    public String getHandlerQualifier(Method method) {
        RedisSync redisSync = getRedisSyncAnnotation(method);
        return redisSync.handlerQualifier();
    }

    @Override
    public String evaluate(String expr, Method method, Object target, Object[] arguments) {
        MethodBasedExpressionResolver<String> resolver = this.exprResolver;
        if (resolver != null) {
            return resolver.evaluate(expr, method, target, arguments);
        }
        return expr;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        if (exprResolver instanceof BeanFactoryAware) {
            ((BeanFactoryAware) exprResolver).setBeanFactory(beanFactory);
        }
        if (synchronizerProvider instanceof BeanFactoryAware) {
            ((BeanFactoryAware) synchronizerProvider).setBeanFactory(beanFactory);
        }
    }

    /**
     * 设置默认情况下的过期时间(ms).
     * @param defaultLeaseTimeMillis the defaultLeaseTimeMillis to set
     */
    public void setDefaultLeaseTimeMillis(long defaultLeaseTimeMillis) {
        this.defaultLeaseTimeMillis = defaultLeaseTimeMillis;
    }

    /**
     * 设置默认情况下的阻塞等待超时时间(ms).
     * @param defaultWaitTimeMillis the defaultWaitTimeMillis to set
     */
    public void setDefaultWaitTimeMillis(long defaultWaitTimeMillis) {
        this.defaultWaitTimeMillis = defaultWaitTimeMillis;
    }

    /**
     * 根据方法执行上下文解析出同步所需相关属性.
     * @param methodInvoker
     * @return
     */
    protected RedisSyncAttributes resolveRedisSyncAttributes(MethodInvoker methodInvoker) {
        // 解析所需相关信息
        Method method = methodInvoker.getMethod();
        Object target = methodInvoker.getTarget();
        Object[] arguments = methodInvoker.getArguments();
        RedisSync redisSync = getRedisSyncAnnotation(method);

        // 解析同步所用键值
        String key = redisSync.name();
        if (StringUtils.hasText(key)) {
            key = evaluate(key, method, target, arguments);
        } else {
            // 不指定则获取默认值
            key = getDefaultSyncKey(methodInvoker);
        }

        // 时间单位
        TimeUnit timeUnit = redisSync.timeUnit();
        // 解析过期时间
        long leaseTimeMillis = this.defaultLeaseTimeMillis;
        String leaseTimeString = redisSync.leaseTime();
        if (StringUtils.hasText(leaseTimeString)) {
            leaseTimeString = evaluate(leaseTimeString, method, target, arguments);
            Long leaseTime = convertTimeStrValue(leaseTimeString);
            leaseTimeMillis = timeUnit.toMillis(leaseTime);
        }
        // 解析等待超时时间
        long waitTimeMillis = this.defaultWaitTimeMillis;
        String waitTimeString = redisSync.waitTime();
        if (StringUtils.hasText(waitTimeString)) {
            waitTimeString = evaluate(waitTimeString, method, target, arguments);
            Long waitTime = convertTimeStrValue(waitTimeString);
            waitTimeMillis = timeUnit.toMillis(waitTime);
        }

        return RedisSyncAttributes.create().setName(key).setLeaseTimeMillis(leaseTimeMillis)
                .setWaitTimeMillis(waitTimeMillis).setHandlerQualifier(() -> getHandlerQualifier(method));
    }

    /**
     * 根据方法执行上下文获取默认的同步用键值, 默认为方法全限定名.
     * @param methodInvoker
     * @return
     * @see java.lang.reflect.Method#toGenericString()
     */
    protected String getDefaultSyncKey(MethodInvoker methodInvoker) {
        return methodInvoker.getMethod().toGenericString();
    }

    /**
     * 将解析后的时间字符串值转换为数值类型.
     * @param timeString
     * @return
     */
    protected Long convertTimeStrValue(String timeString) {
        try {
            return Long.valueOf(timeString);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid time string value '" + timeString + "'", e);
        }
    }

    /**
     * 获取给定方法所标记的 @{@link RedisSync} 注解, 如果方法未被标记则从类上获取.
     * @param method
     * @return
     */
    protected RedisSync getRedisSyncAnnotation(Method method) {
        if (method == null) {
            throw new NullPointerException();
        }
        if (redisSyncCache.containsKey(method)) {
            return redisSyncCache.get(method);
        }
        RedisSync redisSync = AnnotatedElementUtils.findMergedAnnotation(method, RedisSync.class);
        if (redisSync == null) {
            redisSync = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RedisSync.class);
        }
        if (redisSync == null) {
            throw new IllegalStateException("No @RedisSync specified in method '" + method + "'");
        }
        redisSyncCache.put(method, redisSync);
        return redisSync;
    }

}

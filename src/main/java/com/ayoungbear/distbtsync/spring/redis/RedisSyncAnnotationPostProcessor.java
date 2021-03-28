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

import java.lang.reflect.AnnotatedElement;
import java.util.function.Supplier;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;

import com.ayoungbear.distbtsync.spring.MethodBasedExpressionResolver;
import com.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;

/**
 * Spring Bean 后置处理器，通过向代理添加相应的 {@link RedisSyncAnnotationAdvisor} 切面,
 * 自动将同步调用行为应用到任何在类级或方法级携带注解 @{@link RedisSync} 的 bean 上.
 * 这个后置处理器还会在初始化后验证符合条件的 bean 实例中的标记注解, 包括注解属性中所必须配置的占位符,
 * 如果有指定的 {@link SyncMethodFailureHandler} 处理器限定符还会验证上下文中是否存在该实例.
 * 
 * @author yangzexiong
 * @see EnableRedisSync
 * @see RedisSync
 * @see RedisSyncAnnotationAdvisor
 */
@SuppressWarnings("serial")
public class RedisSyncAnnotationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

    @Nullable
    private ConfigurableBeanFactory beanFactory;

    public RedisSyncAnnotationPostProcessor(RedisSyncAnnotationAdvisor advisor) {
        Assert.notNull(advisor, () -> "RedisSyncAnnotationAdvisor must not be null");
        this.advisor = advisor;
    }

    public RedisSyncAnnotationPostProcessor(RedisSynchronizerProvider synchronizerProvider) {
        this(synchronizerProvider, null, null);
    }

    public RedisSyncAnnotationPostProcessor(RedisSynchronizerProvider synchronizerProvider,
            @Nullable Supplier<MethodBasedExpressionResolver<String>> exprResolverSupplier,
            @Nullable Supplier<SyncMethodFailureHandler> defaultHandlerSupplier) {
        this.advisor = new RedisSyncAnnotationAdvisor(synchronizerProvider, exprResolverSupplier,
                defaultHandlerSupplier);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        if (beanFactory instanceof ConfigurableBeanFactory) {
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }
        if (advisor instanceof BeanFactoryAware) {
            ((BeanFactoryAware) advisor).setBeanFactory(beanFactory);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Object object = super.postProcessAfterInitialization(bean, beanName);
        postProcessSyncAnnotation(bean);
        return object;
    }

    /**
     * 校验 bean 中的同步注解属性占位符等信息, 实质是通过 Spring 的属性解析器尝试解析占位符,
     * 默认缺少配置信息会出现异常, 可提前发现配置缺失问题.
     * 但是依赖于方法执行上下文实时信息的情况无法提前校验, 如果有问题需要触发调用才能发现.
     * @param bean
     */
    private void postProcessSyncAnnotation(Object bean) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        if (isEligible(targetClass)) {
            if (hasAnnotation(targetClass)) {
                postProcessSyncAnnotatedElement(targetClass);
            }
            MethodIntrospector.selectMethods(targetClass, (MethodFilter) (method) -> hasAnnotation(method))
                    .forEach(this::postProcessSyncAnnotatedElement);
        }
    }

    /**
     * 校验被注解标记的具体元素对象, 对象可能是方法或者类.
     * @param element
     */
    private void postProcessSyncAnnotatedElement(AnnotatedElement element) {
        RedisSync redisSync = findAnnotation(element);
        try {
            if (redisSync != null) {
                // 校验键值表达式占位符
                String name = redisSync.name();
                if (StringUtils.hasText(name)) {
                    name = resolveEmbeddedValue(name);
                }

                // 校验过期时间表达式占位符
                String leaseTime = redisSync.leaseTime();
                if (StringUtils.hasText(leaseTime)) {
                    leaseTime = resolveEmbeddedValue(leaseTime);
                    Long.valueOf(leaseTime);
                }

                // 校验超时时间表达式占位符
                String waitTime = redisSync.waitTime();
                if (StringUtils.hasText(waitTime)) {
                    waitTime = resolveEmbeddedValue(waitTime);
                    Long.valueOf(waitTime);
                }

                String qualifier = redisSync.handlerQualifier();
                if (StringUtils.hasText(qualifier)) {
                    // 校验上下文中是否有配置相应的处理器
                    RedisSyncUtils.getQualifierBean(beanFactory, qualifier, SyncMethodFailureHandler.class);
                }
            }

        } catch (Exception ex) {
            throw new BeanCreationException(getClass().getName(),
                    "Encountered invalid sync attribute '" + redisSync + "' of element '" + element + "'", ex);
        }
    }

    private boolean hasAnnotation(AnnotatedElement element) {
        return AnnotatedElementUtils.hasAnnotation(element, RedisSync.class);
    }

    private RedisSync findAnnotation(AnnotatedElement element) {
        return AnnotatedElementUtils.findMergedAnnotation(element, RedisSync.class);
    }

    private String resolveEmbeddedValue(String value) {
        if (beanFactory != null) {
            // 此处依赖于上下文配置的解析器, 默认缺少配置会抛出异常.
            return beanFactory.resolveEmbeddedValue(value);
        }
        return value;
    }

}

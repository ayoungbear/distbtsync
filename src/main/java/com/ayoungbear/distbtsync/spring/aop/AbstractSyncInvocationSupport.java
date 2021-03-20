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
package com.ayoungbear.distbtsync.spring.aop;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.util.StringUtils;

import com.ayoungbear.distbtsync.spring.MethodInvoker;
import com.ayoungbear.distbtsync.spring.SyncMethodInvocationHandler;

/**
 * 同步方法调用拦截器支持类, 提供了一些便利的方法. 
 * 支持从 Spring 上下文中获取 {@link #getHandlerQualifier(Method)} 所指定的处理器, 并实现了缓存方便下次快速获取, 
 * 该功能需要 {@link org.springframework.beans.factory.BeanFactory} 的支持.
 * 
 * @author yangzexiong
 * @see org.springframework.beans.factory.BeanFactoryAware
 */
public abstract class AbstractSyncInvocationSupport extends AbstractSyncMethodInterceptor implements BeanFactoryAware {

    private BeanFactory beanFactory;

    private Map<Method, SyncMethodInvocationHandler> handlerCache = new ConcurrentHashMap<>(256);

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    protected SyncMethodInvocationHandler determineSyncInvocationHandler(MethodInvoker methodInvoker) {
        Method method = methodInvoker.getMethod();
        String qualifier = getHandlerQualifier(method);
        if (!StringUtils.hasText(qualifier)) {
            return null;
        }
        SyncMethodInvocationHandler handler = handlerCache.get(method);
        if (handler == null) {
            handler = getQualifierInvocationHandler(qualifier);
            handlerCache.put(method, handler);
        }
        return handler;
    }

    /**
     * 根据给定方法, 返回要使用的处理器的限定符或 bean 名.
     * @param method
     * @return
     */
    protected String getHandlerQualifier(Method method) {
        return null;
    }

    /**
     * 根据给定的限定符由 {@linkplain org.springframework.beans.factory.BeanFactory getBean} 获取指定的处理器.
     * @param qualifier
     * @return
     */
    protected SyncMethodInvocationHandler getQualifierInvocationHandler(String qualifier) {
        return getQualifierBean(qualifier, SyncMethodInvocationHandler.class);
    }

    /**
     * 根据给定的限定符由 {@linkplain org.springframework.beans.factory.BeanFactory getBean} 获取指定的 bean.
     * @param qualifier
     * @return
     */
    protected <T> T getQualifierBean(String qualifier, Class<T> beanClass) {
        if (StringUtils.hasText(qualifier)) {
            if (beanFactory == null) {
                throw new IllegalStateException("BeanFactory must be set on " + getClass().getSimpleName()
                        + " to access qualified bean '" + qualifier + "' of type '" + beanClass.getSimpleName() + "'");
            }
            return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, beanClass, qualifier);
        }
        return null;
    }

}

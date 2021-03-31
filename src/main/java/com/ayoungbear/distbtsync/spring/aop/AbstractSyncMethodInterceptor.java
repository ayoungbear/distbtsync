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

import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.function.SingletonSupplier;

import com.ayoungbear.distbtsync.spring.MethodInvoker;
import com.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;
import com.ayoungbear.distbtsync.spring.Synchronizer;
import com.ayoungbear.distbtsync.spring.support.CachedMethodInvoker;
import com.ayoungbear.distbtsync.spring.support.DefaultSyncFailureHandler;

/**
 * 方法同步调用拦截器基础类. 通过 {@linkplain Synchronizer acquire} 在方法调用前同步获取资源, 
 * 获取成功后才会调用方法, 并在方法调用结束后 通过 {@linkplain Synchronizer release} 释放资源.
 * 如果同步操作执行失败, 会通过 {@link SyncMethodFailureHandler} 来进行相应处理.
 * 
 * @author yangzexiong
 * @see CachedMethodInvoker
 * @see DefaultSyncFailureHandler
 */
public abstract class AbstractSyncMethodInterceptor implements MethodInterceptor, Ordered {

    private SingletonSupplier<SyncMethodFailureHandler> defaultHandlerSupplier;

    protected AbstractSyncMethodInterceptor() {
        this.defaultHandlerSupplier = SingletonSupplier.of(DefaultSyncFailureHandler::new);
    }

    protected AbstractSyncMethodInterceptor(@Nullable SyncMethodFailureHandler defaultHandler) {
        this.defaultHandlerSupplier = new SingletonSupplier<SyncMethodFailureHandler>(defaultHandler,
                DefaultSyncFailureHandler::new);
    }

    protected AbstractSyncMethodInterceptor(@Nullable Supplier<SyncMethodFailureHandler> defaultHandlerSupplier) {
        this.defaultHandlerSupplier = new SingletonSupplier<SyncMethodFailureHandler>(defaultHandlerSupplier,
                DefaultSyncFailureHandler::new);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        MethodInvoker methodInvoker = getMethodInvoker(invocation);
        return syncInvoke(methodInvoker);
    }

    /**
     * 同步方法调用, 在调用前根据指定的 {@link Synchronizer} 同步器进行同步操作,
     * 如果同步失败则由指定的  {@link SyncMethodFailureHandler} 处理器进行相应处理.
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private final Object syncInvoke(MethodInvoker methodInvoker) throws Throwable {
        Synchronizer sync = determineSynchronizer(methodInvoker);
        if (sync == null) {
            throw new IllegalStateException("Synchronizer must be specified");
        }

        SingletonSupplier<SyncMethodFailureHandler> handlerSupplier = SingletonSupplier
                .of(() -> getSyncFailureHandler(methodInvoker));

        try{
            // 方法调用前执行同步
            if (!acquire(sync)) {
                // 同步执行失败后续处理
                handlerSupplier.obtain().handleAcquireFailure(sync, methodInvoker);
            }
    
            try {
                // 执行调用, 如果同步失败时处理器没有抛异常终止, 那么将会直接执行方法调用
                return methodInvoker.invoke();
    
            } finally {
                if (sync.isHeld()) {
                    // 方法调用后执行释放操作
                    if (!release(sync)) {
                        handlerSupplier.obtain().handleReleaseFailure(sync, methodInvoker);
                    }
                } else {
                    // 方法调用结束后发现未持有互斥资源
                    handlerSupplier.obtain().handleHeldFailure(sync, methodInvoker);
                }
            }

        } catch (Throwable t) {
            SyncMethodFailureHandler handler = handlerSupplier.get();
            if (handler != null) {
                handler.handleError(t, methodInvoker);
            }
            throw t;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 获取方法调用执行者.
     * @param invocation
     * @return
     */
    protected MethodInvoker getMethodInvoker(MethodInvocation invocation) {
        return new CachedMethodInvoker(invocation);
    }

    /**
     * 根据方法调用的相关信息获取特定的同步方法调用异常处理器, 如果没有则使用默认的处理器.
     * @param methodInvoker
     * @return
     */
    protected final SyncMethodFailureHandler getSyncFailureHandler(MethodInvoker methodInvoker) {
        SyncMethodFailureHandler handler = determineSyncFailureHandler(methodInvoker);
        if (handler == null) {
            handler = defaultHandlerSupplier.obtain();
        }
        return handler;
    }

    /**
     * 根据方法调用的相关信息获取特定的同步方法调用异常处理器.
     * @param methodInvoker
     * @return
     */
    protected SyncMethodFailureHandler determineSyncFailureHandler(MethodInvoker methodInvoker) {
        return null;
    }

    /**
     * 根据方法调用的相关信息决定特定的同步器.
     * @param methodInvoker
     * @return
     */
    protected abstract Synchronizer determineSynchronizer(MethodInvoker methodInvoker);

    /**
     * 在方法调用之前获取同步资源.
     * @param synchronizer
     * @return
     */
    private final boolean acquire(Synchronizer synchronizer) {
        try {
            return synchronizer.acquire();
        } catch (Exception ae) {
            throw ae;
        }
    }

    /**
     * 在方法调用之后释放同步资源.
     * @param synchronizer
     * @return
     */
    private final boolean release(Synchronizer synchronizer) {
        try {
            return synchronizer.release();
        } catch (Exception re) {
            throw re;
        }
    }

}

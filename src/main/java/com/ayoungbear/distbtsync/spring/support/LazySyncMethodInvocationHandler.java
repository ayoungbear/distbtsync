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
package com.ayoungbear.distbtsync.spring.support;

import java.util.function.Supplier;

import com.ayoungbear.distbtsync.spring.MethodInvoker;
import com.ayoungbear.distbtsync.spring.SyncMethodInvocationHandler;
import com.ayoungbear.distbtsync.spring.Synchronizer;

/**
 * {@link SyncMethodInvocationHandler} 懒加载型包装类, 
 * Spring 高版本可考虑替换成 {@link org.springframework.util.function.SingletonSupplier}.
 * 
 * @author yangzexiong
 */
public class LazySyncMethodInvocationHandler implements SyncMethodInvocationHandler {

    private Supplier<SyncMethodInvocationHandler> handlerSupplier;
    private volatile SyncMethodInvocationHandler handler;

    public LazySyncMethodInvocationHandler(Supplier<SyncMethodInvocationHandler> handlerSupplier) {
        this.handlerSupplier = handlerSupplier;
    }

    @Override
    public void handleAcquireFailure(Synchronizer synchronizer, MethodInvoker methodInvoker) {
        getHandler().handleAcquireFailure(synchronizer, methodInvoker);
    }

    @Override
    public void handleReleaseFailure(Synchronizer synchronizer, MethodInvoker methodInvoker) {
        getHandler().handleReleaseFailure(synchronizer, methodInvoker);
    }

    public SyncMethodInvocationHandler getHandler() {

        if (handler == null) {
            synchronized (this) {
                if (handler == null) {
                    handler = handlerSupplier.get();
                }
            }
        }
        return handler;
    }

}

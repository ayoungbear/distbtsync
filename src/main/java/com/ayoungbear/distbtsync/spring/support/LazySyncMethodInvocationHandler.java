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

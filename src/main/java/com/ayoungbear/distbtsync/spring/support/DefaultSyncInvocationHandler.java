package com.ayoungbear.distbtsync.spring.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ayoungbear.distbtsync.spring.MethodInvoker;
import com.ayoungbear.distbtsync.spring.SyncFailureException;
import com.ayoungbear.distbtsync.spring.SyncMethodInvocationHandler;
import com.ayoungbear.distbtsync.spring.Synchronizer;

/**
 * 默认的同步方法调用处理器, 同步失败时抛异常处理.
 * 
 * @author yangzexiong
 */
@SuppressWarnings("rawtypes")
public class DefaultSyncInvocationHandler implements SyncMethodInvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSyncInvocationHandler.class);

    @Override
    public void handleAcquireFailure(Synchronizer synchronizer, MethodInvoker methodInvoker) {
        String msg = "Failed to acquire synchronization for method '" + methodInvoker.getMethodDescription() + "'";
        if (logger.isErrorEnabled()) {
            logger.error(msg);
        }
        throw new SyncFailureException(msg);
    }

    @Override
    public void handleReleaseFailure(Synchronizer synchronizer, MethodInvoker methodInvoker) {
        if (logger.isErrorEnabled()) {
            String msg = "Failed to release synchronization for method '" + methodInvoker.getMethodDescription() + "'";
            logger.error(msg);
        }
    }

}

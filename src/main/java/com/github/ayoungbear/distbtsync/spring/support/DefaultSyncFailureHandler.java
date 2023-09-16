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

package com.github.ayoungbear.distbtsync.spring.support;

import com.github.ayoungbear.distbtsync.spring.MethodInvoker;
import com.github.ayoungbear.distbtsync.spring.SyncFailureException;
import com.github.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;
import com.github.ayoungbear.distbtsync.spring.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认的同步方法调用处理器, 同步失败时抛异常处理.
 *
 * @author yangzexiong
 */
public class DefaultSyncFailureHandler implements SyncMethodFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSyncFailureHandler.class);

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

    @Override
    public void handleHeldFailure(Synchronizer synchronizer, MethodInvoker methodInvoker) {
        if (logger.isErrorEnabled()) {
            String msg = "Not held sync after invoked for method '" + methodInvoker.getMethodDescription() + "'";
            logger.error(msg);
        }
    }

    @Override
    public void handleError(Throwable t, MethodInvoker methodInvoker) {
        String msg = "Sync invoke encountered an error for method '" + methodInvoker.getMethodDescription() + "'";
        if (logger.isErrorEnabled()) {
            logger.error(msg, t);
        }
        throw new SyncFailureException(msg, t);
    }

}

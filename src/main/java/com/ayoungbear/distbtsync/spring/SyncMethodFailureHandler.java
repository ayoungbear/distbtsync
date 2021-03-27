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
package com.ayoungbear.distbtsync.spring;

/**
 * 同步方法调用异常处理器, 用于处理同步失败异常场景.
 * 
 * @author yangzexiong
 * @see Synchronizer
 * @see MethodInvoker
 */
public interface SyncMethodFailureHandler {

    /**
     * 在 {@linkplain Synchronizer acquire} 同步获取资源失败时, 根据给定的同步器和方法调用对象进行后续处理.
     * 可以通过同步器再次尝试进行同步操作, 也可以通过抛出异常来终止.
     * 如果有必要，可以在不同步的情况下使用 {@linkplain MethodInvoker invoke} 直接调用方法.
     * @param synchronizer
     * @param methodInvoker
     */
    void handleAcquireFailure(Synchronizer synchronizer, MethodInvoker methodInvoker);

    /**
     * 在 {@linkplain Synchronizer release} 同步释放资源失败时, 根据给定的同步器和方法调用对象进行后续处理.
     * 可以通过同步器再次尝试进行释放操作, 也可以记录日志或者抛出异常.
     * @param synchronizer
     * @param methodInvoker
     */
    void handleReleaseFailure(Synchronizer synchronizer, MethodInvoker methodInvoker);

}

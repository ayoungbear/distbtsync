package com.ayoungbear.distbtsync.spring;

/**
 * 同步方法调用处理器.
 * 
 * @author yangzexiong
 * @see Synchronizer
 * @see MethodInvoker
 */
public interface SyncMethodInvocationHandler {

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

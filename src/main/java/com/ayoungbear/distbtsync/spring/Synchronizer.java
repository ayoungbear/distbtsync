package com.ayoungbear.distbtsync.spring;

/**
 * 定义同步执行器接口.
 * 
 * @author yangzexiong
 */
public interface Synchronizer {

    /**
     * 获取资源.
     * @return
     */
    boolean acquire();

    /**
     * 释放资源.
     * @return
     */
    boolean release();

    /**
     * 当前线程是否持有资源.
     * @return
     */
    boolean isHeld();

}

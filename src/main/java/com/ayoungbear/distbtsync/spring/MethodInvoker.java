package com.ayoungbear.distbtsync.spring;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 定义方法调用者接口.
 * 
 * @author yangzexiong
 */
public interface MethodInvoker {

    /**
     * 调用方法并返回结果
     * @return
     * @throws Throwable
     */
    Object invoke() throws Throwable;

    /**
     * 获取目标对象.
     * @return
     */
    Object getTarget();

    /**
     * 获取方法参数
     * @return
     */
    Object[] getArguments();

    /**
     * 获取目标对象类型
     * @return
     */
    Class<?> getTargetClass();

    /**
     * 获取对应方法
     * @return
     */
    Method getMethod();

    /**
     * 获取方法描述
     * @return
     */
    default String getMethodDescription() {
        return Optional.ofNullable(getMethod()).map(Method::toString).orElse(null);
    }

}

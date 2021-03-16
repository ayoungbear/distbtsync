package com.ayoungbear.distbtsync.spring.support;

import java.lang.reflect.Method;

/**
 * 基于特定方法上下文的表达式解析器.
 * 
 * @author yangzexiong
 */
public interface MethodBasedExpressionResolver<T> {

    /**
     * 基于方法相关信息, 对给定的表达式进行解析并返回结果.
     * @param expr 表达式
     * @param method 方法
     * @param target 对象
     * @param arguments 参数
     * @return 表达式解析结果
     */
    T evaluate(String expr, Method method, Object target, Object[] arguments);

}

package com.ayoungbear.distbtsync.spring.aop;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.ayoungbear.distbtsync.spring.MethodInvoker;

/**
 * 依赖于 {@link org.aopalliance.intercept.MethodInvocation} 来实现, 会缓存方法的执行结果, 
 * 重复调用会返回之前执行的结果, 而不会触发再次调用.
 * 
 * @author yangzexiong
 * @see org.aopalliance.intercept.MethodInvocation
 */
public class CachedMethodInvoker implements MethodInvoker {

    private static final Object NULL = new Object();

    private MethodInvocation invocation;

    private Method specificMethod;

    private volatile Object result;

    public CachedMethodInvoker(MethodInvocation invocation) {
        Assert.notNull(invocation, () -> "MethodInvocation must be provided");
        this.invocation = invocation; 
    }

    @Override
    public Object getTarget() {
        return invocation.getThis();
    }

    @Override
    public Object[] getArguments() {
        return invocation.getArguments();
    }
    
    @Override
    public Class<?> getTargetClass() {
        Object target = getTarget();
        return (target != null ? AopUtils.getTargetClass(target) : null);
    }

    @Override
    public Method getMethod() {
        return getSpecificMethod();
    }

    /**
     * 执行方法调用, 如果已经执行过了, 则直接返回结果.
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke() throws Throwable {
        if (result == null) {
            synchronized (this) {
                if (result == null) {
                    Object resultTemp = invocation.proceed();
                    result = resultTemp == null ? NULL : resultTemp;
                }
            }
        }
        return getResult();
    }

    /**
     * 获取方法调用执行结果.
     * @return
     */
    protected Object getResult() {
        return result == NULL ? null : result;
    }

    protected Method getSpecificMethod() {
        if (specificMethod == null) {
            specificMethod = BridgeMethodResolver
                    .findBridgedMethod(ClassUtils.getMostSpecificMethod(invocation.getMethod(), getTargetClass()));
        }
        return specificMethod;
    }

}

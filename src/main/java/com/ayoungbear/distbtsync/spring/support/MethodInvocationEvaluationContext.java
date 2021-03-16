package com.ayoungbear.distbtsync.spring.support;

import java.lang.reflect.Method;
import java.util.Objects;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.BeanExpressionContextAccessor;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;

/**
 * 基于 {@link org.springframework.context.expression.MethodBasedEvaluationContext} 的扩展实现, 提供了针对方法调用的解析上下文.
 * 提供了方法相关的信息, 还可以结合 {@link org.springframework.beans.factory.BeanFactory}, 为解析提供更多样的支持.
 * 
 * @author yangzexiong
 * @see org.springframework.context.expression.MethodBasedEvaluationContext
 */
public class MethodInvocationEvaluationContext extends MethodBasedEvaluationContext implements BeanFactoryAware {

    private Object[] args;
    private Method method;
    private String methodName;
    private Object target;
    private Class<?> targetClass;

    public MethodInvocationEvaluationContext(Method method, Object target, Object[] arguments) {
        super(null, method, arguments, new DefaultParameterNameDiscoverer());
        this.method = Objects.requireNonNull(method, "Method must not be null");
        this.args = arguments;
        this.methodName = method.getName();
        if (target != null) {
            this.target = target;
            this.targetClass = target.getClass();
        }
        init();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory != null) {
            // 设置根节点为beanFactory
            setRootObject(beanFactory);
            setBeanResolver(new BeanFactoryResolver(beanFactory));
            addPropertyAccessor(new EnvironmentAccessor());
            addPropertyAccessor(new BeanFactoryAccessor());
            addPropertyAccessor(new BeanExpressionContextAccessor());
        }
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return args;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getTarget() {
        return target;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    private void init() {
        setVariable("args", args);
        setVariable("method", method);
        setVariable("methodName", methodName);
        setVariable("target", target);
        setVariable("targetClass", targetClass);
        addPropertyAccessor(new MapAccessor());
    }

}

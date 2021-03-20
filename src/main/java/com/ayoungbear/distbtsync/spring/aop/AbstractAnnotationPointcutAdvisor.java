package com.ayoungbear.distbtsync.spring.aop;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.core.GenericTypeResolver;

/**
 * 基于注解的切面基础类, 根据指定的注解类型来定义切入点.
 * 
 * @author yangzexiong
 * @param <A>
 */
@SuppressWarnings({ "serial", "unchecked" })
public abstract class AbstractAnnotationPointcutAdvisor<A extends Annotation> extends AbstractPointcutAdvisor {

    protected final Class<A> annotationType;

    private boolean checkInherited = true;

    private Pointcut pointcut;

    protected AbstractAnnotationPointcutAdvisor() {
        this.annotationType = (Class<A>) Objects.requireNonNull(
                GenericTypeResolver.resolveTypeArgument(getClass(), AbstractAnnotationPointcutAdvisor.class),
                "Annotation type must be specific");
    }

    @Override
    public Pointcut getPointcut() {
        if (pointcut == null) {
            pointcut = buildPointcut();
        }
        return pointcut;
    }

    protected Pointcut buildPointcut() {
        Pointcut methodPointcut = new AnnotationMatchingPointcut(null, annotationType, checkInherited);
        Pointcut classPointcut = new AnnotationMatchingPointcut(annotationType, checkInherited);
        return new ComposablePointcut(methodPointcut).union(classPointcut);
    }

    /**
     * 设置是否也要检查超类和接口，以及注解类型的元注解.
     * @param checkInherited the checkInherited to set
     */
    public void setCheckInherited(boolean checkInherited) {
        this.checkInherited = checkInherited;
    }

}

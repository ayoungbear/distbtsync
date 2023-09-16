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

package com.github.ayoungbear.distbtsync.spring.aop;

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
 * @param <A> 指定的标记注解
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class AbstractAnnotationPointcutAdvisor<A extends Annotation> extends AbstractPointcutAdvisor {

    protected final Class<A> annotationType;

    private boolean checkInherited = true;

    private Pointcut pointcut;

    protected AbstractAnnotationPointcutAdvisor() {
        this(true);
    }

    protected AbstractAnnotationPointcutAdvisor(boolean checkInherited) {
        this.checkInherited = checkInherited;
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

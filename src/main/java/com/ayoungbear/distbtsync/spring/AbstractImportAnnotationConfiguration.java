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

import java.lang.annotation.Annotation;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 用于解析由 @{@link Import} 注解注入配置类的基础类, 解析并获取相关的配置注解属性.
 * 
 * @author yangzexiong
 * @param <A>
 */
public abstract class AbstractImportAnnotationConfiguration<A extends Annotation> implements ImportAware {

    protected AnnotationAttributes annotationAttributes;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Class<?> annotationType = GenericTypeResolver.resolveTypeArgument(getClass(),
                AbstractImportAnnotationConfiguration.class);
        this.annotationAttributes = AnnotationAttributes
                .fromMap(importMetadata.getAnnotationAttributes(annotationType.getName(), false));
        if (this.annotationAttributes == null) {
            throw new IllegalArgumentException("@" + annotationType.getSimpleName()
                    + " is not present on importing class " + importMetadata.getClassName());
        }
    }

}

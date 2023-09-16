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

package com.github.ayoungbear.distbtsync.spring;

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

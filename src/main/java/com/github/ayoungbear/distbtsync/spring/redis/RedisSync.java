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

package com.github.ayoungbear.distbtsync.spring.redis;

import com.github.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;
import com.github.ayoungbear.distbtsync.spring.support.DefaultSyncFailureHandler;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import org.springframework.core.annotation.AliasFor;

/**
 * 基于 Redis 实现的分布式同步注解, 类似于分布式场景下的 <i>synchronized</i> 功能, 
 * 实质是利用分布式锁来控制同步.
 * 可用于标记需要同步调用的方法, 也可以在类或者接口级别上标记, 在这种情况下类下的所有
 * 方法或者接口方法都会同步调用.
 *
 * @author yangzexiong
 * @see RedisSyncAnnotationPostProcessor
 * @see RedisSyncAnnotationAdvisor
 * @see RedisSyncMethodInterceptor
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisSync {

    /**
     * 标识, 在同步中作为分布式锁使用的键值.
     * 同时也是 {@link #name()} 属性的别名.
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 标识, 在同步中作为分布式锁使用的键值.
     * 值的设置可以是直接的字符串, 属性占位符, 或者是 SpEL 表达式.
     * 同时也是 {@link #value()} 属性的别名.
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 租约时间, 表示获取到锁后的最大持有时间, 也就是键值的过期时间, 值解析后的结果必须是数字.
     * 值的设置可以是直接的字符串, 属性占位符, 或者是 SpEL 表达式.
     * 租约时间 <i><0</i> 表示永不过期, 一直持有直到解锁.
     */
    String leaseTime() default "";

    /**
     * 等待时间, 表示同步执行过程中最大的阻塞时间, 指定时间过后仍没获取到锁则超时, 值解析后的结果必须是数字.
     * 值的设置可以是直接的字符串, 属性占位符, 或者是 SpEL 表达式.
     * 等待时间 <i>>0</i> 表示同步执行处于等待超时模式, 将阻塞直到成功或者超时;
     * 等待时间 <i>=0</i> 表示只会尝试加锁一次;
     * 等待时间 <i><0</i> 表示将一直阻塞，直到成功.
     * @return
     */
    String waitTime() default "";

    /**
     * 时间单位, 默认为毫秒(ms).
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 上下文中的特定的 {@link SyncMethodFailureHandler} 实例限定符，可以用于获取指定的处理器来处理异常情况.
     * 不设置表示将使用默认的处理器来处理同步异常情况.
     * @see DefaultSyncFailureHandler
     */
    String handlerQualifier() default "";

}

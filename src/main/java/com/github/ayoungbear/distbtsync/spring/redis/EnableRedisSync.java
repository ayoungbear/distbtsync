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
import com.github.ayoungbear.distbtsync.spring.support.StringMethodExpressionResolver;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用基于 Redis 的同步方法调用功能, 标记了 @{@link RedisSync} 的特定方法将被同步调用,
 * 需要用在 @{@link Configuration} 标记的配置类上, 例如:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableRedisSync
 * public class MyRedisSyncConfiguration {
 *     // various &#064;Bean definitions
 * }</pre>
 *
 * 开启功能后会自动检测容器中任何 spring 托管的 bean 上的 @{@link RedisSync} 注解并为其添加代理,
 * 自动添加同步操作切面. 例如, 注册一个 bean 并标记需要同步执行的方法:
 *
 * <pre class="code">
 * &#064;Service
 * public class MySyncService {
 *
 *     &#064;RedisSync
 *     public void syncInvoke() {
 *         // do something in synchronization
 *     }
 * }</pre>
 *
 * 同步操作依赖于 {@link RedisSynchronizer#acquire()} 同步器来实现, 而具体的同步器则由
 * {@link RedisSynchronizerProvider} 根据方法上下文(如请求参数等)获取.
 * 同步操作所需的相关信息需要用表达式在标记注解 @{@link RedisSync} 里指明, 如操作的键值与过期时间等,
 * 而表达式的解析默认使用 {@link StringMethodExpressionResolver} 来实现, 该表达式解析器支持 SpEL 表达式.
 * 在同步操作出现异常情况时, 可通过 {@link SyncMethodFailureHandler} 处理器来进行相应的后续处理.
 *
 * 如果有需要使用自定义的相关功能, 比如自定义同步器提供者或者表达式解析器等, 可通过 {@link RedisSyncConfigurer}
 * 来配置, 只需要实现需自定义的功能接口即可, 未实现的将使用默认的配置. 对于自定义同步器提供者 
 * {@link RedisSynchronizerProvider} 实现类, 可通过上述方式配置, 也可以通过在 Spring 上下文中注册指定对象来实现.
 * 例如:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableRedisSync
 * public class MyRedisSyncConfiguration {
 *     &#064;Bean
 *     public RedisSynchronizerProvider redisSynchronizerProvider() {
 *         return new MyRedisSynchronizerProvider();
 *     }
 * }</pre>
 *
 * @author yangzexiong
 * @see RedisSync
 * @see RedisSyncConfigurer
 * @see RedisSyncConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RedisSyncConfiguration.class)
public @interface EnableRedisSync {

    /**
     * 设置是否创建基于子类(CGLIB)的代理，而不是标准的基于接口的 Java 代理.
     * 默认为 true 使用 CGLIB.
     */
    boolean proxyTargetClass() default true;

    /**
     * 设置是否应该将代理对象保存在 {@link ThreadLocal} 中, 如果被代理对象需要调用自己的另一个被代理的方法,
     * 可通过 {@link AopContext#currentProxy()} 来获取代理对象并调用.
     * 默认值为 false 以避免不必要的额外拦截.
     */
    boolean exposeProxy() default false;

    /**
     * 设置 {@link RedisSyncAnnotationPostProcessor} 后置处理器的执行顺序.
     * 默认值是 {@link Ordered#LOWEST_PRECEDENCE}, 以便在所有其他后处理器之后运行.
     */
    int order() default Ordered.LOWEST_PRECEDENCE;

}

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
package com.ayoungbear.distbtsync.spring.redis;

import org.springframework.lang.Nullable;

import com.ayoungbear.distbtsync.spring.MethodBasedExpressionResolver;
import com.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;

/**
 * 基于 Redis 的同步相关配置类接口, 可用于指定自定义的表达式解析器或者默认的同步失败处理器等.
 * 例如，给定一个配置类:
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableRedisSync
 * public class AppConfig implements RedisSyncConfigurer {
 *
 *     &#064;Override
 *     public RedisSynchronizerProvider getRedisSynchronizerProvider() {
 *         return new MyRedisSynchronizerProvider();
 *     }
 *     
 *     &#064;Override
 *     public SyncMethodFailureHandler getSyncMethodFailureHandler() {
 *         return new MySyncMethodFailureHandler();
 *     }
 * }</pre>
 * 
 * 可仅设置所需的部分, 返回 {@code null} 则保持默认设置.
 * 
 * @author yangzexiong
 */
public interface RedisSyncConfigurer {

    /**
     * 自定义的 {@link RedisSynchronizerProvider} 同步器提供者实例，
     * 提供在基于 Redis 的方法同步调用中使用的特定同步器 {@link RedisSynchronizer}.
     * @return
     */
    @Nullable
    default RedisSynchronizerProvider getRedisSynchronizerProvider() {
        return null;
    }

    /**
     * 自定义的用于解析同步方法的表达式解析器 {@link MethodBasedExpressionResolver} 实例.
     * @return
     */
    @Nullable
    default MethodBasedExpressionResolver<String> getMethodBasedExpressionResolver() {
        return null;
    }

    /**
     * 自定义的在基于方法的同步调用异常场景中使用的默认处理器 {@link SyncMethodFailureHandler} 实例.
     * @return
     */
    @Nullable
    default SyncMethodFailureHandler getSyncMethodFailureHandler() {
        return null;
    }

}

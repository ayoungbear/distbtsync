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

import java.util.Collection;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.function.SingletonSupplier;

import com.ayoungbear.distbtsync.redis.lock.RedisLockCommands;
import com.ayoungbear.distbtsync.redis.lock.support.JedisClusterCommandsAdapter;
import com.ayoungbear.distbtsync.redis.lock.support.RedisConnectionCommandsAdapter;
import com.ayoungbear.distbtsync.spring.AbstractImportAnnotationConfiguration;
import com.ayoungbear.distbtsync.spring.MethodBasedExpressionResolver;
import com.ayoungbear.distbtsync.spring.SyncMethodFailureHandler;

import redis.clients.jedis.JedisCluster;

/**
 * 基于 {@link RedisSync} 注解式同步方法调用 {@code @Configuration} 配置类, 注册了功能所需的相关 bean.
 * 
 * @author yangzexiong
 * @see EnableRedisSync
 * @see RedisSyncConfigurer
 */
@Configuration
@EnableConfigurationProperties(RedisSyncProperties.class)
public class RedisSyncConfiguration extends AbstractImportAnnotationConfiguration<EnableRedisSync> {

    private RedisSyncProperties properties;

    private RedisConnectionFactory redisConnectionFactory;

    @Nullable
    private RedisSynchronizerProvider customizedSynchronizer;
    @Nullable
    private Supplier<MethodBasedExpressionResolver<String>> resolverSupplier;
    @Nullable
    private Supplier<SyncMethodFailureHandler> defaultHandlerSupplier;

    public RedisSyncConfiguration(RedisSyncProperties properties) {
        this.properties = properties;
    }

    @Bean(RedisSyncUtils.REDIS_SYNC_POST_PROCESSOR_NAME)
    public RedisSyncAnnotationPostProcessor redisSyncAnnotationPostProcessor(
            @Autowired(required = false) @Qualifier(RedisSyncUtils.REDIS_SYNC_SYNCHRONIZER_PROVIDER_BEAN_NAME) RedisSynchronizerProvider redisSynchronizerProvider) {
        Supplier<RedisSynchronizerProvider> defaultSupplier = new SingletonSupplier<RedisSynchronizerProvider>(
                redisSynchronizerProvider, () -> defaultRedisSynchronizerProvider(redisConnectionFactory));
        Supplier<RedisSynchronizerProvider> synchronizerProviderSupplier = new SingletonSupplier<RedisSynchronizerProvider>(
                customizedSynchronizer, defaultSupplier);

        RedisSynchronizerProvider synchronizerProvider = synchronizerProviderSupplier.get();
        if (synchronizerProvider == null) {
            throw new BeanCreationException(RedisSyncUtils.REDIS_SYNC_POST_PROCESSOR_NAME,
                    "The RedisSynchronizerProvider must be provided");
        }

        RedisSyncAnnotationAdvisor advisor = new RedisSyncAnnotationAdvisor(synchronizerProviderSupplier.get(),
                resolverSupplier, defaultHandlerSupplier);
        if (properties.getDefaultLeaseTime() != null) {
            advisor.setDefaultLeaseTimeMillis(properties.getDefaultLeaseTime());
        }
        if (properties.getDefaultWaitTime() != null) {
            advisor.setDefaultWaitTimeMillis(properties.getDefaultWaitTime());
        }

        RedisSyncAnnotationPostProcessor postProcessor = new RedisSyncAnnotationPostProcessor(advisor);
        postProcessor.setProxyTargetClass(annotationAttributes.getBoolean("proxyTargetClass"));
        postProcessor.setExposeProxy(annotationAttributes.getBoolean("exposeProxy"));
        postProcessor.setOrder(annotationAttributes.<Integer> getNumber("order"));

        return postProcessor;
    }

    @Autowired(required = false)
    void setConfigurers(Collection<RedisSyncConfigurer> configurers) {
        if (CollectionUtils.isEmpty(configurers)) {
            return;
        }
        if (configurers.size() > 1) {
            throw new IllegalStateException("Only one RedisSyncConfigurer may exist");
        }
        RedisSyncConfigurer configurer = configurers.iterator().next();
        this.customizedSynchronizer = configurer.getRedisSynchronizerProvider();
        this.resolverSupplier = configurer::getMethodBasedExpressionResolver;
        this.defaultHandlerSupplier = configurer::getSyncMethodFailureHandler;
    }

    @Autowired(required = false)
    void setRedisConnectionFactory(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    protected RedisSynchronizerProvider defaultRedisSynchronizerProvider(
            RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            return null;
        }
        RedisLockCommands commands = determineRedisLockCommands(redisConnectionFactory);
        return new RedisLockSynchronizerProvider(commands);
    }

    protected RedisLockCommands determineRedisLockCommands(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory instanceof JedisConnectionFactory) {
            try {
                RedisClusterConnection clusterConnection = redisConnectionFactory.getClusterConnection();
                if (clusterConnection instanceof JedisClusterConnection) {
                    JedisCluster jedisCluster = ((JedisClusterConnection) clusterConnection).getNativeConnection();
                    return new JedisClusterCommandsAdapter(jedisCluster);
                }
            } catch (Exception e) {
                // ignore and use RedisConnection adapter
            }
        }
        return new RedisConnectionCommandsAdapter(redisConnectionFactory);
    }

}

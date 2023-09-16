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

package com.github.ayoungbear.distbtsync.demo.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import javax.servlet.http.HttpServletRequest;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 拦截 controller GET 请求并自动广播至其他端口 本地服务 的 AOP 配置类.
 * 其他相同服务的端口通过 spring.application.ports 来配置,
 * 为了启动多个服务模拟分布式的场景, 触发测试请求时可自动转发到其他服务节点上,
 * 这样就可以只调用一次便将请求转发到各个服务去, 方便测试.
 * 接口方法需要标记 @{@link Publish} 才会被拦截并广播.
 *
 * @author yangzexiong
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class GetRequestPublishConfiguration {

    @Bean
    public GetRequestPublishAdvisor getRequestPublishAdvisor() {
        return new GetRequestPublishAdvisor();
    }

    @Bean
    public PublishService publishService() {
        return new PublishService();
    }

    /**
     * 用于标记请求是否需要广播到其他本地服务(localhost).
     * 各本地服务端口通过 {@code spring.application.publish.ports} 来配置.
     *
     * @author yangzexiong
     */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public static @interface Publish {

    }

    @SuppressWarnings("serial")
    public static class GetRequestPublishAdvisor extends AbstractPointcutAdvisor
            implements Pointcut, MethodInterceptor {

        private PublishService publishService;

        @Override
        public Object invoke(MethodInvocation arg0) throws Throwable {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            publishService.publishGet(request);
            return arg0.proceed();
        }

        @Override
        public ClassFilter getClassFilter() {
            return ClassFilter.TRUE;
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new StaticMethodMatcher() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    if (AnnotatedElementUtils.hasAnnotation(method, Publish.class)
                            && AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method,
                                RequestMapping.class);
                        RequestMethod[] methods = requestMapping.method();
                        return methods.length == 1 && methods[0] == RequestMethod.GET;
                    }
                    return false;
                }
            };
        }

        @Override
        public Pointcut getPointcut() {
            Pointcut methodPointcut = new AnnotationMatchingPointcut(null, RequestMapping.class, true);
            Pointcut classPointcut = new AnnotationMatchingPointcut(Controller.class, true);
            return new ComposablePointcut(methodPointcut).union(classPointcut).intersection(this);
        }

        @Override
        public Advice getAdvice() {
            return this;
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Autowired
        public void setPublishService(PublishService publishService) {
            this.publishService = publishService;
        }

    }

    /**
     * 异步调用广播服务.
     *
     * @author yangzexiong
     */
    public static class PublishService implements ApplicationListener<WebServerInitializedEvent> {

        private static final Logger logger = LoggerFactory.getLogger(PublishService.class);

        private String localPort;

        @Value("${spring.application.publish.ports:}")
        private String[] ports;

        @Value("${server.servlet.context-path}")
        private String contextPath;

        private RestTemplate restTemplate = new RestTemplate();

        @Async
        public void publishGet(HttpServletRequest request) {
            // 避免再次广播
            boolean isPublish = "1".equals(request.getParameter("isPublish"));
            if (!isPublish && !StringUtils.isAllEmpty(ports)) {
                String servletPath = request.getServletPath();
                Map<String, String> params = new HashMap<String, String>();
                StringJoiner stringJoiner = new StringJoiner("&");
                request.getParameterMap().keySet().stream().forEach((key) -> {
                    String parameter = request.getParameter(key);
                    stringJoiner.add(key + "=" + parameter);
                    params.put(key, parameter);
                });
                // 转发请求给各个端口
                logger.info("GET request publish servletPath={} params={}", servletPath, params);
                for (String port : ports) {
                    if (!StringUtils.equals(localPort, port)) {
                        String uri = "http://localhost:" + port + contextPath + servletPath + "?isPublish=1&"
                                + stringJoiner.toString();
                        logger.info("GET URI={}", uri);
                        try {
                            restTemplate.getForObject(uri, Object.class);
                        } catch (Exception e) {
                            logger.warn("publishGet error", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onApplicationEvent(WebServerInitializedEvent event) {
            this.localPort = String.valueOf(event.getWebServer().getPort());
            logger.info("local server port={}", localPort);
        }

    }

}

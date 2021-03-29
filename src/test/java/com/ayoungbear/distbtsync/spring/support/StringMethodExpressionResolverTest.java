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
package com.ayoungbear.distbtsync.spring.support;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import com.ayoungbear.distbtsync.BaseSpringTest;
import com.ayoungbear.distbtsync.spring.support.StringMethodExpressionResolverTest.ExpressionTestConfiguration.ExpressionService;

/**
 * 表达式解析器单元测试.
 * 
 * @author yangzexiong
 */
@TestPropertySource(properties = { "key=is", "name=bear", "expr=#{#methodName}" })
public class StringMethodExpressionResolverTest extends BaseSpringTest {

    @Autowired
    private BeanFactory beanFactory;

    @Test
    public void testResolveLiteralExpression() throws Exception {
        StringMethodExpressionResolver resolver = new StringMethodExpressionResolver();
        resolver.setBeanFactory(beanFactory);

        String expr = "hi! ${nameKey:name} ${key} ${name}.";
        String key = resolver.evaluate(expr, getMethod(), this, null);
        key = resolver.evaluate(expr, null, null, null);
        String actualKey = "hi! name is bear.";
        Assert.assertEquals(actualKey, key);
    }

    @Test
    public void testResolveMethodExpression() throws Exception {
        StringMethodExpressionResolver resolver = new StringMethodExpressionResolver();
        Map<String, String> map = new HashMap<>();
        map.put("key1", "mapValue1");
        map.put("key2", "mapValue2");
        Object[] args = new Object[] { map, BigDecimal.TEN, Integer.SIZE };

        String expr = "methodName=#{#methodName} targetClass=#{#targetClass.getSimpleName()} map.key1=#{#args[0].key1} #{(#a1.intValue()>0?1:0)+#p2+1}=34";
        String key = resolver.evaluate(expr, getMethod(), this, args);
        String actualKey = "methodName=specificMethod targetClass=StringMethodExpressionResolverTest map.key1=mapValue1 34=34";
        Assert.assertEquals(actualKey, key);
    }

    @Test
    public void testResolveMethodExpressionWithBeanFactory() throws Exception {
        StringMethodExpressionResolver resolver = new StringMethodExpressionResolver();
        resolver.setBeanFactory(beanFactory);
        Object[] args = new Object[] { new HashMap<>(), BigDecimal.ONE, Integer.SIZE };

        String expr = "methodName=#{#methodName} targetClass=#{#targetClass.getSimpleName()} map.size=#{#args[0].size()} "
                + "result=#{expressionServiceName.specificMethod(#map,#p1,#integer)} ${nameKey:name} ${key} ${name}. ${expr}";
        String key = resolver.evaluate(expr, getMethod(), this, args);
        String actualKey = "methodName=specificMethod targetClass=StringMethodExpressionResolverTest map.size=0 "
                + "result=specificMethodInvocation dicimal=1.0032 name is bear. #{#methodName}";
        Assert.assertEquals(actualKey, key);
    }

    @Test(expected = IllegalStateException.class)
    public void testResolveExcetion() throws Exception {
        StringMethodExpressionResolver resolver = new StringMethodExpressionResolver();
        resolver.setBeanFactory(beanFactory);
        String expr = "${none}";
        resolver.evaluate(expr, getMethod(), this, null);
    }

    @Configuration
    public static class ExpressionTestConfiguration {
        @Bean("expressionServiceName")
        public ExpressionService myExpressionService() {
            return new ExpressionService();
        }

        public static class ExpressionService {
            public String specificMethod(Map<String, String> map, BigDecimal dicimal, Integer integer) {
                return "specificMethodInvocation dicimal=" + dicimal.setScale(2, BigDecimal.ROUND_HALF_UP) + integer;
            }
        }
    }

    private Method getMethod() throws Exception {
        return ExpressionService.class.getDeclaredMethod("specificMethod", Map.class, BigDecimal.class, Integer.class);
    }

}

package com.ayoungbear.distbtsync.spring.support;

import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * 基于特定方法的 SpEl 表达式解析器, 将表达式根据方法上下文解析成字符串并返回.
 * 
 * @author yangzexiong
 * @see MethodInvocationEvaluationContext
 * @see SpelExpressionSupport
 */
public class StringMethodExpressionResolver extends SpelExpressionSupport
        implements MethodBasedExpressionResolver<String>, BeanFactoryAware, StringValueResolver {

    private ConfigurableBeanFactory beanFactory;

    public StringMethodExpressionResolver() {
        super(ParserContext.TEMPLATE_EXPRESSION);
    }

    @Override
    public String evaluate(String expr, Method method, Object target, Object[] arguments) {
        if (!StringUtils.hasText(expr)) {
            return expr;
        }
        try {
            Expression expression = parseExpression(expr);

            if (expression instanceof LiteralExpression) {
                // 文字表达式不需要解析
                return resolveStringValue(expression.getExpressionString());
            }

            EvaluationContext context = getEvaluationContext(method, target, arguments);

            Object result = expression.getValue(context);
            String stringResult = convertResult(result);
            return resolveStringValue(stringResult);

        } catch (Exception ex) {
            throw new IllegalStateException("Expression of expr '" + expr + "' parsing failed", ex);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableBeanFactory) {
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }
    }

    @Override
    public String resolveStringValue(String strVal) {
        // 解析占位符
        return beanFactory == null ? strVal : beanFactory.resolveEmbeddedValue(strVal);
    }

    /**
     * 获取用于解析方法表达式的求值上下文.
     * @param method
     * @param target
     * @param arguments
     * @return
     */
    protected EvaluationContext getEvaluationContext(Method method, Object target, Object[] arguments) {
        MethodInvocationEvaluationContext context = new MethodInvocationEvaluationContext(method, target, arguments);
        if (beanFactory != null) {
            context.setBeanFactory(beanFactory);
        }
        return context;
    }

    /**
     * 将求值结果转换为字符串值.
     * @param result
     * @return
     */
    protected String convertResult(Object result) {
        return String.valueOf(result);
    }

}

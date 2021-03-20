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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 基于 SpEl 的表达式解析器.
 * 
 * @author yangzexiong
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 */
public class SpelExpressionSupport implements ExpressionParser {

    private Map<String, Expression> expressionCache = new ConcurrentHashMap<String, Expression>(256);

    private ParserContext parserContext;

    private ExpressionParser expressionParser;

    public SpelExpressionSupport() {
        this.expressionParser = new SpelExpressionParser();
        this.parserContext = ParserContext.TEMPLATE_EXPRESSION;
    }

    public SpelExpressionSupport(ParserContext parserContext) {
        this.expressionParser = new SpelExpressionParser();
        this.parserContext = parserContext;
    }

    public SpelExpressionSupport(ClassLoader beanClassLoader) {
        this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
        this.parserContext = ParserContext.TEMPLATE_EXPRESSION;
    }

    public SpelExpressionSupport(ParserContext parserContext, ClassLoader beanClassLoader) {
        this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
        this.parserContext = parserContext;
    }

    @Override
    public Expression parseExpression(String expressionString, ParserContext context) throws ParseException {
        if (expressionString == null) {
            throw new NullPointerException();
        }
        Expression expression = this.expressionCache.get(expressionString);
        if (expression == null) {
            expression = this.expressionParser.parseExpression(expressionString, context);
            this.expressionCache.put(expressionString, expression);
        }
        return expression;
    }

    @Override
    public final Expression parseExpression(String expressionString) {
        return parseExpression(expressionString, parserContext);
    }

    public void setParserContext(ParserContext parserContext) {
        this.parserContext = parserContext;
    }

    public void setParserContext(String expressionPrefix, String expressionSuffix) {
        this.parserContext = new ParserContext() {
            @Override
            public boolean isTemplate() {
                return true;
            }

            @Override
            public String getExpressionPrefix() {
                return expressionPrefix;
            }

            @Override
            public String getExpressionSuffix() {
                return expressionSuffix;
            }
        };
    }

}

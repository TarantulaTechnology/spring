/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class ExpressionEvaluatingCorrelationStrategyTests {

	private ExpressionEvaluatingCorrelationStrategy strategy;


	@Test(expected = IllegalArgumentException.class)
	public void testCreateInstanceWithEmptyExpressionFails() throws Exception {
		strategy = new ExpressionEvaluatingCorrelationStrategy("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateInstanceWithNullExpressionFails() throws Exception {
		Expression nullExpression = null;
		strategy = new ExpressionEvaluatingCorrelationStrategy(nullExpression);
	}

	@Test
	public void testCorrelationKeyWithMethodInvokingExpression() throws Exception {
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression expression = parser.parseExpression("payload.substring(0,1)");
		strategy = new ExpressionEvaluatingCorrelationStrategy(expression);
		strategy.setBeanFactory(mock(BeanFactory.class));
		strategy.afterPropertiesSet();
		Object correlationKey = strategy.getCorrelationKey(new GenericMessage<String>("bla"));
		assertThat(correlationKey, is(instanceOf(String.class)));
		assertThat((String) correlationKey, is("b"));
	}

	@Test
	public void testCorrelationStrategyWithAtBeanExpression() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("expression-evaluating-correlation-with-bf.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		Message<?> message = MessageBuilder.withPayload("foo").setSequenceNumber(1).setSequenceSize(1).build();
		inputChannel.send(message);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
	}

	public static class CustomCorrelator {
		public Object correlate(Object o){
			return o;
		}
	}
}

/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class MethodInvokingMessageSourceTests {

	@Test
	public void testValidMethod() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setObject(new TestBean());
		source.setMethodName("validMethod");
		Message<?> result = source.receive();
		assertNotNull(result);
		assertNotNull(result.getPayload());
		assertEquals("valid", result.getPayload());
	}

	@Test
	public void testHeaderExpressions() {
		Map<String, Expression> headerExpressions = new HashMap<String, Expression>();
		headerExpressions.put("foo", new LiteralExpression("abc"));
		headerExpressions.put("bar", new SpelExpressionParser().parseExpression("new Integer(123)"));
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setBeanFactory(mock(BeanFactory.class));
		source.setObject(new TestBean());
		source.setMethodName("validMethod");
		source.setHeaderExpressions(headerExpressions);
		Message<?> result = source.receive();
		assertNotNull(result);
		assertNotNull(result.getPayload());
		assertEquals("valid", result.getPayload());
		assertEquals("abc", result.getHeaders().get("foo"));
		assertEquals(123, result.getHeaders().get("bar"));
	}

	@Test(expected=MessagingException.class)
	public void testNoMatchingMethodName() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setObject(new TestBean());
		source.setMethodName("noMatchingMethod");
		source.receive();
	}

	@Test(expected=MessagingException.class)
	public void testInvalidMethodWithArg() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setObject(new TestBean());
		source.setMethodName("invalidMethodWithArg");
		source.receive();
	}

	@Test(expected=MessagingException.class)
	public void testInvalidMethodWithNoReturnValue() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setObject(new TestBean());
		source.setMethodName("invalidMethodWithNoReturnValue");
		source.receive();
	}

	@Test
	public void testNullReturningMethodReturnsNullMessage() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setObject(new TestBean());
		source.setMethodName("nullReturningMethod");
		Message<?> message = source.receive();
		assertNull(message);
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		public String validMethod() {
			return "valid";
		}

		public String invalidMethodWithArg(String arg) {
			return "invalid";
		}

		public void invalidMethodWithNoReturnValue() {
		}

		public Object nullReturningMethod() {
			return null;
		}
	}

}

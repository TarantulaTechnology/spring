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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class PublisherExpressionTests {

	private final StaticApplicationContext context = new StaticApplicationContext();


	@Before
	public void setup() throws Exception {
		context.registerSingleton("testChannel", QueueChannel.class);
		IntegrationEvaluationContextFactoryBean factory = new IntegrationEvaluationContextFactoryBean();
		factory.setBeanFactory(context);
		factory.afterPropertiesSet();
		EvaluationContext ec = factory.getObject();
		context.getBeanFactory().registerSingleton(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME, ec);
		context.getBeanFactory().registerSingleton("foo", "foo");
	}


	@Test // INT-1139
	public void returnValue() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		advisor.setDefaultChannel(testChannel);
		ProxyFactory pf = new ProxyFactory(new TestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test("123");
		Message<?> message = testChannel.receive(0);
		assertNotNull(message);
		assertEquals("hellofoo", message.getPayload());
		assertEquals("123", message.getHeaders().get("foo"));
	}


	static interface TestBean {
		String test(String sku);
	}


	static class TestBeanImpl implements TestBean {

		@Publisher
		@Payload("#return + @foo")
		public String test(@Header("foo") String foo) {
			return "hello";
		}
	}

}

/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicExpressionRouterIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel even;

	@Autowired
	private PollableChannel odd;


	@Test
	public void dynamicExpressionBasedRouter() {
		TestBean testBean1 = new TestBean(1);
		TestBean testBean2 = new TestBean(2);
		TestBean testBean3 = new TestBean(3);
		TestBean testBean4 = new TestBean(4);
		Message<?> message1 = MessageBuilder.withPayload(testBean1).build();
		Message<?> message2 = MessageBuilder.withPayload(testBean2).build();
		Message<?> message3 = MessageBuilder.withPayload(testBean3).build();
		Message<?> message4 = MessageBuilder.withPayload(testBean4).build();
		this.input.send(message1);
		this.input.send(message2);
		this.input.send(message3);
		this.input.send(message4);
		assertEquals(testBean1, odd.receive(0).getPayload());
		assertEquals(testBean2, even.receive(0).getPayload());
		assertEquals(testBean3, odd.receive(0).getPayload());
		assertEquals(testBean4, even.receive(0).getPayload());
		assertNull(odd.receive(0));
		assertNull(even.receive(0));
	}


	static class TestBean {

		private final int number;

		public TestBean(int number) {
			this.number = number;
		}

		public int getNumber() {
			return this.number;
		}
	}

}

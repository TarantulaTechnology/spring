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

package org.springframework.integration.channel.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 */
public class MessageSelectingInterceptorTests {

	@Test
	public void testSingleSelectorAccepts() {
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector = new TestMessageSelector(true, counter);
		MessageSelectingInterceptor interceptor = new MessageSelectingInterceptor(selector);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		assertTrue(channel.send(new GenericMessage<String>("test1")));
	}

	@Test(expected=MessageDeliveryException.class)
	public void testSingleSelectorRejects() {
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector = new TestMessageSelector(false, counter);
		MessageSelectingInterceptor interceptor = new MessageSelectingInterceptor(selector);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		channel.send(new GenericMessage<String>("test1"));
	}

	@Test
	public void testMultipleSelectorsAccept() {
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector1 = new TestMessageSelector(true, counter);
		MessageSelector selector2 = new TestMessageSelector(true, counter);
		MessageSelectingInterceptor interceptor = new MessageSelectingInterceptor(selector1, selector2);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		assertTrue(channel.send(new GenericMessage<String>("test1")));
		assertEquals(2, counter.get());
	}

	@Test
	public void testMultipleSelectorsReject() {
		boolean exceptionThrown = false;
		final AtomicInteger counter = new AtomicInteger();
		MessageSelector selector1 = new TestMessageSelector(true, counter);
		MessageSelector selector2 = new TestMessageSelector(false, counter);
		MessageSelector selector3 = new TestMessageSelector(false, counter);
		MessageSelector selector4 = new TestMessageSelector(true, counter);
		MessageSelectingInterceptor interceptor = new MessageSelectingInterceptor(selector1, selector2, selector3, selector4);
		QueueChannel channel = new QueueChannel();
		channel.addInterceptor(interceptor);
		try {
			channel.send(new GenericMessage<String>("test1"));
		}
		catch (MessageDeliveryException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		assertEquals(2, counter.get());
	}


	private static class TestMessageSelector implements MessageSelector {

		private final boolean shouldAccept;

		private final AtomicInteger counter;


		public TestMessageSelector(boolean shouldAccept, AtomicInteger counter) {
			this.shouldAccept = shouldAccept;
			this.counter = counter;
		}


		public boolean accept(Message<?> message) {
			this.counter.incrementAndGet();
			return this.shouldAccept;
		}
	}

}

/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.mongodb.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class DelayerHandlerRescheduleIntegrationTests extends MongoDbAvailableTests {

	public static final String DELAYER_ID = "delayerWithMongoMS";

	@Test
	@MongoDbAvailable
	@SuppressWarnings("unchecked")
	public void testDelayerHandlerRescheduleWithMongoDbMessageStore() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"DelayerHandlerRescheduleIntegrationTests-context.xml", this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		String delayerMessageGroupId = DELAYER_ID + ".messageGroupId";

		messageStore.removeMessageGroup(delayerMessageGroupId);

		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		input.send(message1);
		input.send(MessageBuilder.withPayload("test2").build());

		// Emulate restart and check DB state before next start
		context.destroy();

		Thread.sleep(100);

		try {
			context.getBean("input", MessageChannel.class);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			assertTrue(e.getMessage().contains("BeanFactory not initialized or already closed - call 'refresh'"));
		}

		assertEquals(2, messageStore.messageGroupSize(delayerMessageGroupId));

		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		Iterator<Message<?>> iterator = messageGroup.getMessages().iterator();
		Message<?> messageInStore = iterator.next();
		Object payload = messageInStore.getPayload();

		// INT-3049
		assertTrue(payload instanceof DelayHandler.DelayedMessageWrapper);

		Message<String> original1 = (Message<String>) ((DelayHandler.DelayedMessageWrapper) payload).getOriginal();
		messageInStore = iterator.next();
		Message<String> original2 = (Message<String>) ((DelayHandler.DelayedMessageWrapper) messageInStore.getPayload()).getOriginal();
		assertThat(message1, Matchers.anyOf(Matchers.is(original1), Matchers.is(original2)));

		context.refresh();

		PollableChannel output = context.getBean("output", PollableChannel.class);

		Message<?> message = output.receive(10000);
		assertNotNull(message);

		Object payload1 = message.getPayload();

		message = output.receive(10000);
		assertNotNull(message);
		Object payload2 = message.getPayload();
		assertNotSame(payload1, payload2);

		assertEquals(0, messageStore.messageGroupSize(delayerMessageGroupId));

	}

}

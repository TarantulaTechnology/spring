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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChannelWithMessageStoreParserTests {
	
	private static final String BASE_PACKAGE = "org.springframework.integration";

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	private TestHandler handler;

	@Autowired
	private MessageGroupStore messageGroupStore;

	@Test
	@DirtiesContext
	public void testActivatorSendsToPersistentQueue() throws Exception {

		input.send(createMessage("123", "id1", 3, 1, null));
		handler.getLatch().await(100, TimeUnit.MILLISECONDS);
		assertEquals("The message payload is not correct", "123", handler.getMessageString());
		// The group id for buffered messages is the channel name
		assertEquals(1, messageGroupStore.getMessageGroup("messageStore:output").size());
		
		Message<?> result = output.receive(100);
		assertEquals("hello", result.getPayload());
		assertEquals(0, messageGroupStore.getMessageGroup(BASE_PACKAGE+".store:output").size());

	}

	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(outputChannel).build();
	}

}

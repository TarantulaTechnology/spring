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

package org.springframework.integration.aggregator.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
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
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatorReplyChannelTests {

	@Autowired
	private volatile MessageChannel input;

	@Autowired
	private volatile PollableChannel output;

	private final List<String> list = new ArrayList<String>();


	@Before
	public void setupList() {
		list.add("foo");
		list.add("bar");
	}


	@Test
	public void replyChannelHeader() {
		this.verifyReply(MessageBuilder.withPayload(list).setReplyChannel(output).build());
	}

	@Test // INT-1095
	public void replyChannelNameHeader() {
		this.verifyReply(MessageBuilder.withPayload(list).setReplyChannelName("output").build());
	}

	private void verifyReply(Message<?> message) {
		assertNull(output.receive(0));
		input.send(message);
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof List);
		List<?> resultList = (List<?>) result.getPayload();
		assertEquals(2, resultList.size());
		assertTrue(resultList.contains("foo"));
		assertTrue(resultList.contains("bar"));
	}

}

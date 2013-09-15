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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderEnricherMethodInvokingTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void replyChannelExplicitOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("input", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
		assertEquals(123, result.getHeaders().get("foo"));
		assertEquals("ABC", result.getHeaders().get("bar"));
		assertEquals("zzz", result.getHeaders().get("other"));
	}


	public static class TestBean {

		public String echo(String text) {
			return text.toUpperCase();
		}

		public Map<String, Object> enrich() {
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("foo", 123);
			headers.put("bar", "ABC");
			return headers;
		}
	}

}

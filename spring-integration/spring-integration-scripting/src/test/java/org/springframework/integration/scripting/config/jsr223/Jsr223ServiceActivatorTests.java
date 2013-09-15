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

package org.springframework.integration.scripting.config.jsr223;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Jsr223ServiceActivatorTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Autowired
	private MessageChannel withScriptVariableGenerator;

	@Test
	public void referencedScript() throws Exception {

		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.referencedScriptInput.send(message);
			Thread.sleep(1000);
		}
		String value1 = (String) replyChannel.receive(0).getPayload();
		String value2 = (String) replyChannel.receive(0).getPayload();
		String value3 = (String) replyChannel.receive(0).getPayload();
		System.out.println(value1 + "\n" + value2 + "\n" + value3);
		assertTrue(value1.startsWith("python-test-1-foo - bar"));
		assertTrue(value2.startsWith("python-test-2-foo - bar"));
		assertTrue(value3.startsWith("python-test-3-foo - bar"));

		// because we are using 'prototype bean the suffix date will be
		// different
		assertFalse(value1.substring(26).equals(value2.substring(26)));
		assertFalse(value2.substring(26).equals(value3.substring(26)));

		assertNull(replyChannel.receive(0));
	}

	@Test
	public void withScriptVariableGenerator() throws Exception {

		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.withScriptVariableGenerator.send(message);
			Thread.sleep(1000);
		}
		String value1 = (String) replyChannel.receive(0).getPayload();
		String value2 = (String) replyChannel.receive(0).getPayload();
		String value3 = (String) replyChannel.receive(0).getPayload();
		assertTrue(value1.startsWith("ruby-test-1-foo - bar"));
		assertTrue(value2.startsWith("ruby-test-2-foo - bar"));
		assertTrue(value3.startsWith("ruby-test-3-foo - bar"));
		// because we are using 'prototype bean the suffix date will be
		// different

		assertFalse(value1.substring(26).equals(value2.substring(26)));
		assertFalse(value2.substring(26).equals(value3.substring(26)));

		assertNull(replyChannel.receive(0));
	}

	@Test
	public void inlineScript() throws Exception {

		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.inlineScriptInput.send(message);
		}
		assertEquals("inline-test-1", replyChannel.receive(0).getPayload());
		assertEquals("inline-test-2", replyChannel.receive(0).getPayload());
		assertEquals("inline-test-3", replyChannel.receive(0).getPayload());
		assertNull(replyChannel.receive(0));

	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void inlineScriptAndVariables() throws Exception {
		new ClassPathXmlApplicationContext("Jsr223ServiceActivatorTests-fail-context.xml", this.getClass());
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void variablesAndScriptVariableGenerator() throws Exception {
		new ClassPathXmlApplicationContext("Jsr223ServiceActivatorTests-fail-withgenerator-context.xml",
				this.getClass());
	}

	public static class SampleScriptVariSource implements ScriptVariableGenerator {
		public Map<String, Object> generateScriptVariables(Message<?> message) {
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("foo", "foo");
			variables.put("bar", "bar");
			variables.put("date", new Date());
			variables.put("payload", message.getPayload());
			variables.put("headers", message.getHeaders());
			return variables;
		}
	}

}

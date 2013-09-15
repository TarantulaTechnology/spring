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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ControlBusTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Test
	public void testDefaultEvaluationContext() {
		Message<?> message = MessageBuilder.withPayload("@service.convert('aardvark')+headers.foo").setHeader("foo", "bar").build();
		this.input.send(message);
		assertEquals("catbar", output.receive(0).getPayload());
		assertNull(output.receive(0));
	}

	@Test
	public void testLifecycleMethods() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ControlBusLifecycleTests-context.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		PollableChannel outputChannel = context.getBean("outputChannel", PollableChannel.class);
		assertNull(outputChannel.receive(1000));
		Message<?> message = MessageBuilder.withPayload("@adapter.start()").build();
		inputChannel.send(message);
		assertNotNull(outputChannel.receive(1000));
	}


	public static class Service {

		@ManagedOperation
		public String convert(String input) {
			return "cat";
		}
	}

	public static class AdapterService {
		public Message<String> receive() {
			return new GenericMessage<String>(new Date().toString());
		}
	}

}

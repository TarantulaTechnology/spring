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

package org.springframework.integration.groovy.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.hamcrest.Matchers;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GroovyHeaderEnricherTests {

	@Autowired
	private MessageChannel inputA;

	@Autowired
	private QueueChannel outputA;

	@Autowired
	private MessageChannel inputB;

	@Autowired
	private QueueChannel outputB;

	@Autowired
	private EventDrivenConsumer headerEnricherWithInlineGroovyScript;

	@Test
	public void referencedScript() throws Exception{
		inputA.send(new GenericMessage<String>("Hello"));
		assertEquals("groovy", outputA.receive(1000).getHeaders().get("TEST_HEADER"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void inlineScript() throws Exception{
		Map<String, HeaderEnricher.HeaderValueMessageProcessor<?>> headers =
				TestUtils.getPropertyValue(headerEnricherWithInlineGroovyScript, "handler.transformer.headersToAdd", Map.class);
		assertEquals(1, headers.size());
		HeaderEnricher.HeaderValueMessageProcessor<?> headerValueMessageProcessor = headers.get("TEST_HEADER");
		assertThat(headerValueMessageProcessor.getClass().getName(), Matchers.containsString("HeaderEnricher$MessageProcessingHeaderValueMessageProcessor"));
		Object targetProcessor = TestUtils.getPropertyValue(headerValueMessageProcessor, "targetProcessor");
		assertEquals(GroovyScriptExecutingMessageProcessor.class, targetProcessor.getClass());

		inputB.send(new GenericMessage<String>("Hello"));
		assertEquals("groovy", outputB.receive(1000).getHeaders().get("TEST_HEADER"));
	}
}

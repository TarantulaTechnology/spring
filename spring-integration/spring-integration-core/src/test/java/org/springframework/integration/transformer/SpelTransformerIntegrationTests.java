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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelTransformerIntegrationTests {

	@Autowired
	private MessageChannel simpleInput;

	@Autowired
	private MessageChannel beanResolvingInput;

	@Autowired @Qualifier("output")
	private PollableChannel output;

	@Autowired
	private MessageChannel transformerChainInput;

	@Autowired @Qualifier("foo.handler")
	private AbstractReplyProducingMessageHandler fooHandler;

	@Autowired @Qualifier("bar.handler")
	private AbstractReplyProducingMessageHandler barHandler;

	@Autowired
	private MessageChannel spelFunctionInput;


	@Test
	public void simple() {
		Message<?> message = MessageBuilder.withPayload(new TestBean()).setHeader("bar", 123).build();
		this.simpleInput.send(message);
		Message<?> result = output.receive(0);
		assertEquals("test123", result.getPayload());
	}

	@Test
	public void beanResolving() {
		Message<?> message = MessageBuilder.withPayload("foo").build();
		this.beanResolvingInput.send(message);
		Message<?> result = output.receive(0);
		assertEquals("testFOO", result.getPayload());
	}

	@Test
	public void testInt2755ChainChildIdWithinExceptionMessage() {
		try {
			this.transformerChainInput.send(new GenericMessage<String>("foo"));
		}
		catch (ReplyRequiredException e) {
			assertThat(e.getMessage(), Matchers.containsString("No reply produced by handler 'transformerChain$child#0'"));
		}
	}

	@Test
	public void testCustomAccessor() {
		QueueChannel outputChannel = new QueueChannel();
		fooHandler.setOutputChannel(outputChannel);
		Foo foo = new Foo("baz");
		fooHandler.handleMessage(new GenericMessage<Foo>(foo));
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertTrue(reply.getPayload() instanceof String);
		assertEquals("baz", reply.getPayload());
	}

	@Test
	public void testCustomFunction() {
		QueueChannel outputChannel = new QueueChannel();
		barHandler.setOutputChannel(outputChannel);
		barHandler.handleMessage(new GenericMessage<String>("foo"));
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertEquals("bar", reply.getPayload());
	}

	@Test
	public void testInt1639SpelFunction() {
		Message<?> message = MessageBuilder.withPayload("  foo   ").build();
		this.spelFunctionInput.send(message);
		Message<?> result = output.receive(0);
		assertEquals("foo", result.getPayload());
	}

	static class TestBean {

		public String getFoo() {
			return "test";
		}

	}

	public static class Foo {
		private String bar;

		public Foo(String bar) {
			this.bar = bar;
		}

		public String obtainBar() {
			return bar;
		}

		public void updateBar(String bar) {
			this.bar = bar;
		}

	}

	public static class FooAccessor implements PropertyAccessor {

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] {Foo.class};
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return "bar".equals(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Assert.isInstanceOf(Foo.class, target);
			return  new TypedValue(((Foo) target).obtainBar(), TypeDescriptor.valueOf(String.class));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return "bar".equals(name);
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			Assert.isInstanceOf(Foo.class, target);
			Assert.isInstanceOf(String.class, newValue);
			((Foo) target).updateBar((String) newValue);
		}

	}

	public static class BarFunction {

		public static String bar(Message<?> message) {
			return "bar";
		}
	}
}

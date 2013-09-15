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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageMatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Dave Turanski
 * @author Artem Bilan
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChainParserTests {

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	@Qualifier("filterInput")
	private MessageChannel filterInput;

	@Autowired
	@Qualifier("pollableInput1")
	private MessageChannel pollableInput1;

	@Autowired
	@Qualifier("pollableInput2")
	private MessageChannel pollableInput2;

	@Autowired
	@Qualifier("headerEnricherInput")
	private MessageChannel headerEnricherInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	@Qualifier("replyOutput")
	private PollableChannel replyOutput;

	@Autowired
	@Qualifier("beanInput")
	private MessageChannel beanInput;

	@Autowired
	@Qualifier("aggregatorInput")
	private MessageChannel aggregatorInput;

	@Autowired
	private MessageChannel payloadTypeRouterInput;

	@Autowired
	private MessageChannel headerValueRouterInput;

	@Autowired
	private MessageChannel headerValueRouterWithMappingInput;

	@Autowired
	private MessageChannel loggingChannelAdapterChannel;

	@Autowired @Qualifier("logChain.handler")
	private MessageHandlerChain logChain;

	@Autowired
	private MessageChannel outboundChannelAdapterChannel;

	@Autowired
	private TestConsumer testConsumer;

	@Autowired
	@Qualifier("chainWithSendTimeout.handler")
	private MessageHandlerChain chainWithSendTimeout;

	@Autowired
	@Qualifier("claimCheckInput")
	private MessageChannel claimCheckInput;

	@Autowired
	@Qualifier("claimCheckOutput")
	private PollableChannel claimCheckOutput;

	@Autowired
	private PollableChannel strings;

	@Autowired
	private PollableChannel numbers;

	@Autowired
	private MessageChannel chainReplayRequiredChannel;

	@Autowired
	private MessageChannel chainMessageRejectedExceptionChannel;

	public static Message<?> successMessage = MessageBuilder.withPayload("success").build();

	@Factory
	public static Matcher<Message<?>> sameExceptImmutableHeaders(Message<?> expected) {
		return new MessageMatcher(expected);
	}

	@Test
	public void chainWithAcceptingFilter() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.filterInput.send(message);
		Message<?> reply = this.output.receive(0);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

	@Test
	public void chainWithRejectingFilter() {
		Message<?> message = MessageBuilder.withPayload(123).build();
		this.filterInput.send(message);
		Message<?> reply = this.output.receive(0);
		assertNull(reply);
	}

	@Test
	public void chainWithHeaderEnricher() {
		Message<?> message = MessageBuilder.withPayload(123).build();
		this.headerEnricherInput.send(message);
		Message<?> reply = this.replyOutput.receive(0);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
		assertEquals("ABC", reply.getHeaders().getCorrelationId());
		assertEquals("XYZ", reply.getHeaders().get("testValue"));
		assertEquals(123, reply.getHeaders().get("testRef"));
	}

	@Test
	public void chainWithPollableInput() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.pollableInput1.send(message);
		Message<?> reply = this.output.receive(3000);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

	@Test
	public void chainWithPollerReference() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.pollableInput2.send(message);
		Message<?> reply = this.output.receive(3000);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

	@Test
	public void chainHandlerBean() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.beanInput.send(message);
		Message<?> reply = this.output.receive(3000);
		assertNotNull(reply);
		assertThat(reply, sameExceptImmutableHeaders(successMessage));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void chainNestingAndAggregation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").setCorrelationId(1).setSequenceSize(1).build();
		this.aggregatorInput.send(message);
		Message reply = this.output.receive(3000);
		assertNotNull(reply);
		assertEquals("foo", reply.getPayload());
	}

	@Test
	public void chainWithPayloadTypeRouter() throws Exception {
		Message<?> message1 = MessageBuilder.withPayload("test").build();
		Message<?> message2 = MessageBuilder.withPayload(123).build();
		this.payloadTypeRouterInput.send(message1);
		this.payloadTypeRouterInput.send(message2);
		Message<?> reply1 = this.strings.receive(0);
		Message<?> reply2 = this.numbers.receive(0);
		assertNotNull(reply1);
		assertNotNull(reply2);
		assertEquals("test", reply1.getPayload());
		assertEquals(123, reply2.getPayload());
	}

	@Test // INT-2315
	public void chainWithHeaderValueRouter() throws Exception {
		Message<?> message1 = MessageBuilder.withPayload("test").setHeader("routingHeader", "strings").build();
		Message<?> message2 = MessageBuilder.withPayload(123).setHeader("routingHeader", "numbers").build();
		this.headerValueRouterInput.send(message1);
		this.headerValueRouterInput.send(message2);
		Message<?> reply1 = this.strings.receive(0);
		Message<?> reply2 = this.numbers.receive(0);
		assertNotNull(reply1);
		assertNotNull(reply2);
		assertEquals("test", reply1.getPayload());
		assertEquals(123, reply2.getPayload());
	}

	@Test // INT-2315
	public void chainWithHeaderValueRouterWithMapping() throws Exception {
		Message<?> message1 = MessageBuilder.withPayload("test").setHeader("routingHeader", "isString").build();
		Message<?> message2 = MessageBuilder.withPayload(123).setHeader("routingHeader", "isNumber").build();
		this.headerValueRouterWithMappingInput.send(message1);
		this.headerValueRouterWithMappingInput.send(message2);
		Message<?> reply1 = this.strings.receive(0);
		Message<?> reply2 = this.numbers.receive(0);
		assertNotNull(reply1);
		assertNotNull(reply2);
		assertEquals("test", reply1.getPayload());
		assertEquals(123, reply2.getPayload());
	}

	@Test // INT-1165
	public void chainWithSendTimeout() {
		long sendTimeout = TestUtils.getPropertyValue(this.chainWithSendTimeout, "sendTimeout", Long.class);
		assertEquals(9876, sendTimeout);
	}

	@Test //INT-1622
	public void chainWithClaimChecks() {
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.claimCheckInput.send(message);
		Message<?> reply = this.claimCheckOutput.receive(0);
		assertEquals(message.getPayload(), reply.getPayload());
	}

	@Test //INT-2275
	public void chainWithOutboundChannelAdapter() {
		this.outboundChannelAdapterChannel.send(successMessage);
		assertSame(successMessage, testConsumer.getLastMessage());
	}

	@Test //INT-2275, INT-2958
	public void chainWithLoggingChannelAdapter() {
		Log logger = mock(Log.class);
		final AtomicReference<String> log = new AtomicReference<String>();
		when(logger.isWarnEnabled()).thenReturn(true);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				log.set((String) invocation.getArguments()[0]);
				return null;
			}
		}).when(logger).warn(any());

		@SuppressWarnings("unchecked")
		List<MessageHandler> handlers = TestUtils.getPropertyValue(this.logChain, "handlers", List.class);
		MessageHandler handler = handlers.get(2);
		assertTrue(handler instanceof LoggingHandler);
		DirectFieldAccessor dfa = new DirectFieldAccessor(handler);
		dfa.setPropertyValue("messageLogger", logger);

		this.loggingChannelAdapterChannel.send(MessageBuilder.withPayload(new byte[] {116, 101, 115, 116}).build());
		assertNotNull(log.get());
		assertEquals("TEST", log.get());
	}

	@Test(expected = BeanCreationException.class) //INT-2275
	public void invalidNestedChainWithLoggingChannelAdapter() {
		try {
			new ClassPathXmlApplicationContext("invalidNestedChainWithOutboundChannelAdapter-context.xml", this.getClass());
			fail("BeanCreationException is expected!");
		}
		catch (BeansException e) {
			assertEquals(IllegalArgumentException.class, e.getCause().getClass());
			assertTrue(e.getMessage().contains("output channel was provided"));
			assertTrue(e.getMessage().contains("does not implement the MessageProducer"));
			throw e;
		}
	}

	@Test //INT-2605
	public void checkSmartLifecycleConfig() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("ChainParserSmartLifecycleAttributesTest.xml", this.getClass());
		MessageHandlerChain handlerChain = ctx.getBean(MessageHandlerChain.class);
		assertEquals(false, handlerChain.isAutoStartup());
		assertEquals(256, handlerChain.getPhase());
		assertEquals(3000L, TestUtils.getPropertyValue(handlerChain, "sendTimeout"));
		assertEquals(false, TestUtils.getPropertyValue(handlerChain, "running"));
		//INT-3108
		MessageHandler serviceActivator = ctx.getBean("chain$child.sa-within-chain.handler", MessageHandler.class);
		assertTrue(TestUtils.getPropertyValue(serviceActivator, "requiresReply", Boolean.class));
	}

	@Test
	public void testInt2755SubComponentsIdSupport() {
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1.handler"));
		assertTrue(this.beanFactory.containsBean("filterChain$child.filterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("filterChain$child.serviceActivatorWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain$child.aggregatorWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain$child.nestedChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain$child.nestedChain$child.filterWithinNestedChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain$child.nestedChain$child.doubleNestedChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain$child.nestedChain$child.doubleNestedChain$child.filterWithinDoubleNestedChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain2.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain2$child.aggregatorWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain2$child.nestedChain.handler"));
		assertTrue(this.beanFactory.containsBean("aggregatorChain2$child.nestedChain$child.filterWithinNestedChain.handler"));
		assertTrue(this.beanFactory.containsBean("payloadTypeRouterChain$child.payloadTypeRouterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("headerValueRouterChain$child.headerValueRouterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("chainWithClaimChecks$child.claimCheckInWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("chainWithClaimChecks$child.claimCheckOutWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("outboundChain$child.outboundChannelAdapterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("logChain$child.transformerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("logChain$child.loggingChannelAdapterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.splitterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.resequencerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.enricherWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.headerFilterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.payloadSerializingTransformerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.payloadDeserializingTransformerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.gatewayWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.objectToStringTransformerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.objectToMapTransformerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.mapToObjectTransformerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.controlBusWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("subComponentsIdSupport1$child.routerWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("exceptionTypeRouterChain$child.exceptionTypeRouterWithinChain.handler"));
		assertTrue(this.beanFactory.containsBean("recipientListRouterChain$child.recipientListRouterWithinChain.handler"));

		MessageHandlerChain chain = this.beanFactory.getBean("headerEnricherChain.handler", MessageHandlerChain.class);
		List handlers = TestUtils.getPropertyValue(chain, "handlers", List.class);

		assertTrue(handlers.get(0) instanceof MessageTransformingHandler);
		assertEquals("headerEnricherChain$child.headerEnricherWithinChain", TestUtils.getPropertyValue(handlers.get(0), "componentName"));
		assertEquals("headerEnricherChain$child.headerEnricherWithinChain.handler", TestUtils.getPropertyValue(handlers.get(0), "beanName"));
		assertTrue(this.beanFactory.containsBean("headerEnricherChain$child.headerEnricherWithinChain.handler"));

		assertTrue(handlers.get(1) instanceof ServiceActivatingHandler);
		assertEquals("headerEnricherChain$child#1", TestUtils.getPropertyValue(handlers.get(1), "componentName"));
		assertNull(TestUtils.getPropertyValue(handlers.get(1), "beanName"));
		assertFalse(this.beanFactory.containsBean("headerEnricherChain$child#1.handler"));

	}

	@Test
	public void testInt2755SubComponentException() {
		GenericMessage<String> testMessage = new GenericMessage<String>("test");
		try {
			this.chainReplayRequiredChannel.send(testMessage);
			fail("Expected ReplyRequiredException");
		}
		catch (Exception e) {
			assertTrue(e instanceof ReplyRequiredException);
			assertTrue(e.getMessage().contains("'chainReplayRequired$child.transformerReplayRequired'"));
		}

		try {
			this.chainMessageRejectedExceptionChannel.send(testMessage);
			fail("Expected MessageRejectedException");
		}
		catch (Exception e) {
			assertTrue(e instanceof MessageRejectedException);
			assertTrue(e.getMessage().contains("chainMessageRejectedException$child.filterMessageRejectedException"));
		}

	}

	public static class StubHandler extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return successMessage;
		}

	}

	public static class StubAggregator {

		public String aggregate(List<String> strings) {
			return StringUtils.collectionToCommaDelimitedString(strings);
		}
	}

	public static class FooPojo {


		private String bar;

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

	}

}

/*
 * Copyright 2002-2011 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.ChannelResolutionException;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ChannelAdapterParserTests {

	private AbstractApplicationContext applicationContext;
	private AbstractApplicationContext applicationContextInner;


	@Before
	public void setUp() {
		this.applicationContext = new ClassPathXmlApplicationContext(
				"ChannelAdapterParserTests-context.xml", this.getClass());
		this.applicationContextInner = new ClassPathXmlApplicationContext(
				"ChannelAdapterParserTests-inner-context.xml", this.getClass());
	}

	@After
	public void tearDown() {
		this.applicationContext.close();
	}


	@Test
	public void methodInvokingSourceStoppedByApplicationContext() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		this.applicationContext.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		this.applicationContext.stop();
		message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void methodInvokingSourceStoppedByApplicationContextInner() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContextInner.getBean("queueChannel");
//		TestBean testBean = (TestBean) this.applicationContextInner.getBean("testBean");
//		testBean.store("source test");
		Object adapter = this.applicationContextInner.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		this.applicationContextInner.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		//assertEquals("source test", testBean.getMessage());
		this.applicationContextInner.stop();
		message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void targetOnly() {
		String beanName = "outboundWithImplicitChannel";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveChannelName(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		TestConsumer consumer = (TestConsumer) this.applicationContext.getBean("consumer");
		assertNull(consumer.getLastMessage());
		Message<?> message = new GenericMessage<String>("test");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(consumer.getLastMessage());
		assertEquals(message, consumer.getLastMessage());
	}

	@Test
	public void methodInvokingConsumer() {
		String beanName = "methodInvokingConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveChannelName(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertNull(testBean.getMessage());
		Message<?> message = new GenericMessage<String>("consumer test");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(testBean.getMessage());
		assertEquals("consumer test", testBean.getMessage());
	}

	@Test
	/**
	 * @since 2.1
	 */
	public void expressionConsumer() {
		String beanName = "expressionConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveChannelName(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertNull(testBean.getMessage());
		Message<?> message = new GenericMessage<String>("consumer test expression");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(testBean.getMessage());
		assertEquals("consumer test expression", testBean.getMessage());
	}

	@Test
	public void methodInvokingSource() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(100);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		((SourcePollingChannelAdapter) adapter).stop();
	}

	@Test
	public void methodInvokingSourceWithHeaders() {
		String beanName = "methodInvokingSourceWithHeaders";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannelForHeadersTest");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(100);
		((SourcePollingChannelAdapter) adapter).stop();
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		assertEquals("source test", message.getPayload());
		assertEquals("ABC", message.getHeaders().get("foo"));
		assertEquals(123, message.getHeaders().get("bar"));
	}

	@Test
	public void methodInvokingSourceNotStarted() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		Message<?> message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void methodInvokingSourceStopped() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		((SourcePollingChannelAdapter) adapter).stop();
		message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void methodInvokingSourceStartedByApplicationContext() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		this.applicationContext.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		this.applicationContext.stop();
	}

	@Test(expected = ChannelResolutionException.class)
	public void methodInvokingSourceAdapterIsNotChannel() {
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		channelResolver.resolveChannelName("methodInvokingSource");
	}

	@Test
	public void methodInvokingSourceWithSendTimeout() throws Exception {
		String beanName = "methodInvokingSourceWithTimeout";

		SourcePollingChannelAdapter adapter =
				this.applicationContext.getBean(beanName, SourcePollingChannelAdapter.class);
		assertNotNull(adapter);
		long sendTimeout = TestUtils.getPropertyValue(adapter, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(999, sendTimeout);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void innerBeanAndExpressionFail() throws Exception {
		new ClassPathXmlApplicationContext("InboundChannelAdapterInnerBeanWithExpression-fail-context.xml", this.getClass());
	}

	public static class SampleBean {
		private String message = "hello";

		String getMessage() {
			return message;
		}
	}
}


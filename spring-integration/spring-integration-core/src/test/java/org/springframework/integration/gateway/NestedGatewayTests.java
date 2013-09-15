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

package org.springframework.integration.gateway;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class NestedGatewayTests {

	@Test
	public void nestedWithinHandler() {
		DirectChannel innerChannel = new DirectChannel();
		DirectChannel outerChannel = new DirectChannel();
		innerChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload() + "-reply";
			}
		});
		final MessagingGatewaySupport innerGateway = new MessagingGatewaySupport() {};
		innerGateway.setRequestChannel(innerChannel);
		innerGateway.afterPropertiesSet();
		outerChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return innerGateway.sendAndReceiveMessage(
						"pre-" + requestMessage.getPayload()).getPayload() + "-post";
			}
		});
		MessagingGatewaySupport outerGateway = new MessagingGatewaySupport() {};
		outerGateway.setRequestChannel(outerChannel);
		outerGateway.afterPropertiesSet();
		Message<?> reply = outerGateway.sendAndReceiveMessage("test");
		assertEquals("pre-test-reply-post", reply.getPayload());
	}

	@Test
	public void replyChannelRetained() {
		DirectChannel requestChannel = new DirectChannel();
		DirectChannel replyChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload() + "-reply";
			}
		});
		MessagingGatewaySupport gateway = new MessagingGatewaySupport() {};
		gateway.setRequestChannel(requestChannel);
		gateway.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel).build();
		Message<?> reply = gateway.sendAndReceiveMessage(message);
		assertEquals("test-reply", reply.getPayload());
		assertEquals(replyChannel, reply.getHeaders().getReplyChannel());
	}

	@Test
	public void errorChannelRetained() {
		DirectChannel requestChannel = new DirectChannel();
		DirectChannel errorChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload() + "-reply";
			}
		});
		MessagingGatewaySupport gateway = new MessagingGatewaySupport() {};
		gateway.setRequestChannel(requestChannel);
		gateway.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
				.setErrorChannel(errorChannel).build();
		Message<?> reply = gateway.sendAndReceiveMessage(message);
		assertEquals("test-reply", reply.getPayload());
		assertEquals(errorChannel, reply.getHeaders().getErrorChannel());
	}

}

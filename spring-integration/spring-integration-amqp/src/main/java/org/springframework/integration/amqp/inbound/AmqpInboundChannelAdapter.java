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

package org.springframework.integration.amqp.inbound;

import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Adapter that receives Messages from an AMQP Queue, converts them into
 * Spring Integration Messages, and sends the results to a Message Channel.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class AmqpInboundChannelAdapter extends MessageProducerSupport implements
		OrderlyShutdownCapable {

	private final AbstractMessageListenerContainer messageListenerContainer;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile AmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper();


	public AmqpInboundChannelAdapter(AbstractMessageListenerContainer listenerContainer) {
		Assert.notNull(listenerContainer, "listenerContainer must not be null");
		Assert.isNull(listenerContainer.getMessageListener(), "The listenerContainer provided to an AMQP inbound Channel Adapter " +
				"must not have a MessageListener configured since the adapter needs to configure its own listener implementation.");
		this.messageListenerContainer = listenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
	}


	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	@Override
	protected void onInit() {
		this.messageListenerContainer.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				Object payload = messageConverter.fromMessage(message);
				Map<String, ?> headers = headerMapper.toHeadersFromRequest(message.getMessageProperties());
				sendMessage(MessageBuilder.withPayload(payload).copyHeaders(headers).build());
			}
		});
		this.messageListenerContainer.afterPropertiesSet();
		super.onInit();
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}


	/**
	 * {@inheritDoc}
	 * <p>
	 * Shuts down the listener container.
	 */
	public int beforeShutdown() {
		this.stop();
		return 0;
	}


	/**
	 * {@inheritDoc}
	 * <p>No-op
	 */
	public int afterShutdown() {
		return 0;
	}

}

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

package org.springframework.integration.jms;

import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.Assert;

/**
 * JMS MessageListener that converts a JMS Message into a Spring Integration
 * Message and sends that Message to a channel. If the 'expectReply' value is
 * <code>true</code>, it will also wait for a Spring Integration reply Message
 * and convert that into a JMS reply.
 * 
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Oleg Zhurakousky
 */
public class ChannelPublishingJmsMessageListener 
		implements SessionAwareMessageListener<javax.jms.Message>, InitializingBean, TrackableComponent {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	private volatile boolean expectReply;
	
	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile boolean extractRequestPayload = true;

	private volatile boolean extractReplyPayload = true;

	private volatile Object defaultReplyDestination;

	private volatile String correlationKey;

	private volatile long replyTimeToLive = javax.jms.Message.DEFAULT_TIME_TO_LIVE;

	private volatile int replyPriority = javax.jms.Message.DEFAULT_PRIORITY;

	private volatile int replyDeliveryMode = javax.jms.Message.DEFAULT_DELIVERY_MODE;

	private volatile boolean explicitQosEnabledForReplies;

	private volatile DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private volatile JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
	
	private final GatewayDelegate gatewayDelegate = new GatewayDelegate();

	/**
	 * Specify whether a JMS reply Message is expected.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	public void setComponentName(String componentName){
		this.gatewayDelegate.setComponentName(componentName);
	}
	
	public void setRequestChannel(MessageChannel requestChannel){
		this.gatewayDelegate.setRequestChannel(requestChannel);
	}
	
	public void setReplyChannel(MessageChannel replyChannel){
		this.gatewayDelegate.setReplyChannel(replyChannel);
	}
	
	public void setErrorChannel(MessageChannel errorChannel){
		this.gatewayDelegate.setErrorChannel(errorChannel);
	}
	
	public void setRequestTimeout(long requestTimeout){
		this.gatewayDelegate.setRequestTimeout(requestTimeout);
	}
	
	public void setReplyTimeout(long replyTimeout){
		this.gatewayDelegate.setReplyTimeout(replyTimeout);
	}
	
	public void setShouldTrack(boolean shouldTrack) {
		this.gatewayDelegate.setShouldTrack(shouldTrack);
	}

	public String getComponentName() {
		return this.gatewayDelegate.getComponentName();
	}

	public String getComponentType() {
		return this.gatewayDelegate.getComponentType();
	}
	
	/**
	 * Set the default reply destination to send reply messages to. This will
	 * be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 */
	public void setDefaultReplyDestination(Destination defaultReplyDestination) {
		this.defaultReplyDestination = defaultReplyDestination;
	}

	/**
	 * Set the name of the default reply queue to send reply messages to.
	 * This will be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 * <p>Alternatively, specify a JMS Destination object as "defaultReplyDestination".
	 * @see #setDestinationResolver
	 * @see #setDefaultReplyDestination(javax.jms.Destination)
	 */
	public void setDefaultReplyQueueName(String destinationName) {
		this.defaultReplyDestination = new DestinationNameHolder(destinationName, false);
	}

	/**
	 * Set the name of the default reply topic to send reply messages to.
	 * This will be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 * <p>Alternatively, specify a JMS Destination object as "defaultReplyDestination".
	 * @see #setDestinationResolver
	 * @see #setDefaultReplyDestination(javax.jms.Destination)
	 */
	public void setDefaultReplyTopicName(String destinationName) {
		this.defaultReplyDestination = new DestinationNameHolder(destinationName, true);
	}

	/**
	 * Specify the time-to-live property for JMS reply Messages.
	 * @see javax.jms.MessageProducer#setTimeToLive(long)
	 */
	public void setReplyTimeToLive(long replyTimeToLive) {
		this.replyTimeToLive = replyTimeToLive;
	}

	/**
	 * Specify the priority value for JMS reply Messages.
	 * @see javax.jms.MessageProducer#setPriority(int)
	 */
	public void setReplyPriority(int replyPriority) {
		this.replyPriority = replyPriority;
	}

	/**
	 * Specify the delivery mode for JMS reply Messages.
	 * @see javax.jms.MessageProducer#setDeliveryMode(int)
	 */
	public void setReplyDeliveryPersistent(boolean replyDeliveryPersistent) {
		this.replyDeliveryMode = replyDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
	}

	/**
	 * Provide the name of a JMS property that should be copied from the request
	 * Message to the reply Message. If this value is NULL (the default) then the
	 * JMSMessageID from the request will be copied into the JMSCorrelationID of the reply
	 * unless there is already a value in the JMSCorrelationID property of the newly created
	 * reply Message in which case nothing will be copied. If the JMSCorrelationID of the
	 * request Message should be copied into the JMSCorrelationID of the reply Message 
	 * instead, then this value should be set to "JMSCorrelationID".
	 * Any other value will be treated as a JMS String Property to be copied as-is
	 * from the request Message into the reply Message with the same property name.
	 */
	public void setCorrelationKey(String correlationKey) {
		this.correlationKey = correlationKey;
	}

	/**
	 * Specify whether explicit QoS should be enabled for replies
	 * (for timeToLive, priority, and deliveryMode settings). 
	 */
	public void setExplicitQosEnabledForReplies(boolean explicitQosEnabledForReplies) {
		this.explicitQosEnabledForReplies = explicitQosEnabledForReplies;
	}

	/**
	 * Set the DestinationResolver that should be used to resolve reply
	 * destination names for this listener.
	 * <p>The default resolver is a DynamicDestinationResolver. Specify a
	 * JndiDestinationResolver for resolving destination names as JNDI locations.
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "destinationResolver must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Provide a {@link MessageConverter} implementation to use when
	 * converting between JMS Messages and Spring Integration Messages.
	 * If none is provided, a {@link SimpleMessageConverter} will
	 * be used.
	 * 
	 * @param messageConverter
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Provide a {@link JmsHeaderMapper} implementation to use when
	 * converting between JMS Messages and Spring Integration Messages.
	 * If none is provided, a {@link DefaultJmsHeaderMapper} will be used.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify whether the JMS request Message's body should be extracted prior
	 * to converting into a Spring Integration Message. This value is set to
	 * <code>true</code> by default. To send the JMS Message itself as a
	 * Spring Integration Message payload, set this to <code>false</code>.
	 */
	public void setExtractRequestPayload(boolean extractRequestPayload) {
		this.extractRequestPayload = extractRequestPayload;
	}

	/**
	 * Specify whether the Spring Integration reply Message's payload should be
	 * extracted prior to converting into a JMS Message. This value is set to
	 * <code>true</code> by default. To send the Spring Integration Message
	 * itself as the JMS Message's body, set this to <code>false</code>.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	public void onMessage(javax.jms.Message jmsMessage, Session session) throws JMSException {
		Object result = jmsMessage;
		if (this.extractRequestPayload) {
			result = this.messageConverter.fromMessage(jmsMessage);
			if (logger.isDebugEnabled()) {
				logger.debug("converted JMS Message [" + jmsMessage + "] to integration Message payload [" + result + "]");
			}
		}
	
		Map<String, Object> headers = headerMapper.toHeaders(jmsMessage);
		Message<?> requestMessage = (result instanceof Message<?>) ?
				MessageBuilder.fromMessage((Message<?>) result).copyHeaders(headers).build() : 
				MessageBuilder.withPayload(result).copyHeaders(headers).build();
		if (!this.expectReply) {
			this.gatewayDelegate.send(requestMessage);
		}
		else {
			Message<?> replyMessage = this.gatewayDelegate.sendAndReceiveMessage(requestMessage);
			if (replyMessage != null) {
				Destination destination = this.getReplyDestination(jmsMessage, session);
				if (destination != null) {
					// convert SI Message to JMS Message
					Object replyResult = replyMessage;
					if (this.extractReplyPayload) {
						replyResult = replyMessage.getPayload();
					}
					try {
						javax.jms.Message jmsReply = this.messageConverter.toMessage(replyResult, session);
						// map SI Message Headers to JMS Message Properties/Headers
						headerMapper.fromHeaders(replyMessage.getHeaders(), jmsReply);
						this.copyCorrelationIdFromRequestToReply(jmsMessage, jmsReply);
						this.sendReply(jmsReply, destination, session);
					}
					catch (RuntimeException e) {
						logger.error("Failed to generate JMS Reply Message from: " + replyResult, e);
						throw e;
					}
				}
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("expected a reply but none was received");
			}
		}
	}

	public void afterPropertiesSet()  {
		this.gatewayDelegate.afterPropertiesSet();
	}
	
	protected void start(){
		this.gatewayDelegate.start();
	}
	
	protected void stop(){
		this.gatewayDelegate.stop();
	}
	
	private void copyCorrelationIdFromRequestToReply(javax.jms.Message requestMessage, javax.jms.Message replyMessage) throws JMSException {
		if (this.correlationKey != null) {
			if (this.correlationKey.equals("JMSCorrelationID")) {
				replyMessage.setJMSCorrelationID(requestMessage.getJMSCorrelationID());
			}
			else {
				String value = requestMessage.getStringProperty(this.correlationKey);
				if (value != null) {
					replyMessage.setStringProperty(this.correlationKey, value);
				}
				else if (logger.isWarnEnabled()) {
					logger.warn("No property value available on request Message for correlationKey '" + this.correlationKey + "'");
				}
			}
		}
		else if (replyMessage.getJMSCorrelationID() == null) {
			replyMessage.setJMSCorrelationID(requestMessage.getJMSMessageID());
		}
	}

	/**
	 * Determine a reply destination for the given message.
	 * <p>
	 * This implementation first checks the boolean 'error' flag which signifies that the reply is an error message. If
	 * reply is not an error it will first check the JMS Reply-To {@link Destination} of the supplied request message;
	 * if that is not <code>null</code> it is returned; if it is <code>null</code>, then the configured
	 * {@link #resolveDefaultReplyDestination default reply destination} is returned; if this too is <code>null</code>,
	 * then an {@link InvalidDestinationException} is thrown.
	 * @param request the original incoming JMS message
	 * @param session the JMS Session to operate on
	 * @return the reply destination (never <code>null</code>)
	 * @throws JMSException if thrown by JMS API methods
	 * @throws InvalidDestinationException if no {@link Destination} can be determined
	 * @see #setDefaultReplyDestination
	 * @see javax.jms.Message#getJMSReplyTo()
	 */
	private Destination getReplyDestination(javax.jms.Message request, Session session) throws JMSException {
		Destination replyTo = request.getJMSReplyTo();
		if (replyTo == null) {
			replyTo = resolveDefaultReplyDestination(session);
			if (replyTo == null) {
				throw new InvalidDestinationException("Cannot determine reply destination: " +
						"Request message does not contain reply-to destination, and no default reply destination set.");
			}
		}
		return replyTo;
	}

	/**
	 * Resolve the default reply destination into a JMS {@link Destination}, using this
	 * listener's {@link DestinationResolver} in case of a destination name.
	 * @return the located {@link Destination}
	 * @throws javax.jms.JMSException if resolution failed
	 * @see #setDefaultReplyDestination
	 * @see #setDefaultReplyQueueName
	 * @see #setDefaultReplyTopicName
	 * @see #setDestinationResolver
	 */
	private Destination resolveDefaultReplyDestination(Session session) throws JMSException {
		if (this.defaultReplyDestination instanceof Destination) {
			return (Destination) this.defaultReplyDestination;
		}
		if (this.defaultReplyDestination instanceof DestinationNameHolder) {
			DestinationNameHolder nameHolder = (DestinationNameHolder) this.defaultReplyDestination;
			return this.destinationResolver.resolveDestinationName(session, nameHolder.name, nameHolder.isTopic);
		}
		return null;
	}

	private void sendReply(javax.jms.Message replyMessage, Destination destination, Session session) throws JMSException {
		MessageProducer producer = session.createProducer(destination);
		try {
			if (this.explicitQosEnabledForReplies) {
				producer.send(replyMessage, this.replyDeliveryMode, this.replyPriority, this.replyTimeToLive);
			}
			else {
				producer.send(replyMessage);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * Internal class combining a destination name
	 * and its target destination type (queue or topic).
	 */
	private static class DestinationNameHolder {

		public final String name;

		public final boolean isTopic;

		public DestinationNameHolder(String name, boolean isTopic) {
			this.name = name;
			this.isTopic = isTopic;
		}
	}
	
	private class GatewayDelegate extends MessagingGatewaySupport {

		protected void send(Object request) {
			super.send(request);
		}
		
		protected Message<?> sendAndReceiveMessage(Object request) {
			return super.sendAndReceiveMessage(request);
		}

		public String getComponentType() {
			if (expectReply) {
				return "jms:inbound-gateway";
			}
			else {
				return "jms:message-driven-channel-adapter";
			}
		}
	}
}

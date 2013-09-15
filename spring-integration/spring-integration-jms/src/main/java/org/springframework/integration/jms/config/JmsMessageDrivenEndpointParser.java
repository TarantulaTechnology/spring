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

package org.springframework.integration.jms.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;message-driven-channel-adapter&gt; element and the
 * &lt;inbound-gateway&gt; element of the 'jms' namespace.
 *
 * @author Mark Fisher
 * @author Michael Bannister
 */
public class JmsMessageDrivenEndpointParser extends AbstractSingleBeanDefinitionParser {

	private static final String DEFAULT_REPLY_DESTINATION_ATTRIB = "default-reply-destination";

	private static final String DEFAULT_REPLY_QUEUE_NAME_ATTRIB = "default-reply-queue-name";

	private static final String DEFAULT_REPLY_TOPIC_NAME_ATTRIB = "default-reply-topic-name";

	private static final String REPLY_TIME_TO_LIVE = "reply-time-to-live";

	private static final String REPLY_PRIORITY = "reply-priority";

	private static final String REPLY_DELIVERY_PERSISTENT = "reply-delivery-persistent";

	private static final String EXPLICIT_QOS_ENABLED_FOR_REPLIES = "explicit-qos-enabled-for-replies";


	private static String[] containerAttributes = new String[] {
		JmsAdapterParserUtils.CONNECTION_FACTORY_PROPERTY,
		JmsAdapterParserUtils.DESTINATION_ATTRIBUTE,
		JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE,
		"destination-resolver", "transaction-manager",
		"concurrent-consumers", "max-concurrent-consumers",
		"max-messages-per-task", "selector",
		"receive-timeout", "recovery-interval",
		"idle-consumer-limit", "idle-task-execution-limit",
		"cache-level", "subscription-durable", "durable-subscription-name",
		"client-id", "task-executor"
	};


	private final boolean expectReply;


	public JmsMessageDrivenEndpointParser(boolean expectReply) {
		this.expectReply = expectReply;
	}


	@Override
	protected String getBeanClassName(Element element) {
		return JmsMessageDrivenEndpoint.class.getName();
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String containerBeanName = this.parseMessageListenerContainer(element, parserContext);
		String listenerBeanName = this.parseMessageListener(element, parserContext);
		builder.addConstructorArgReference(containerBeanName);
		builder.addConstructorArgReference(listenerBeanName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
	}

	private String parseMessageListenerContainer(Element element, ParserContext parserContext) {
		if (element.hasAttribute("container")) {
			for (String containerAttribute : containerAttributes) {
				if (element.hasAttribute(containerAttribute)) {
					parserContext.getReaderContext().error("The '" + containerAttribute +
							"' attribute should not be provided when specifying a 'container' reference.", element);
				}
			}
			return element.getAttribute("container");
		}
		// otherwise, we build a DefaultMessageListenerContainer instance
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.jms.listener.DefaultMessageListenerContainer");
		String destinationAttribute = this.expectReply ? "request-destination" : "destination";
		String destinationNameAttribute = this.expectReply ? "request-destination-name" : "destination-name";
		String pubSubDomainAttribute = this.expectReply ? "request-pub-sub-domain" : "pub-sub-domain";
		String destination = element.getAttribute(destinationAttribute);
		String destinationName = element.getAttribute(destinationNameAttribute);
		boolean hasDestination = StringUtils.hasText(destination);
		boolean hasDestinationName = StringUtils.hasText(destinationName);
		if (!(hasDestination ^ hasDestinationName)) {
			parserContext.getReaderContext().error(
					"Exactly one of '" + destinationAttribute +
					"' or '" + destinationNameAttribute + "' is required.", element);
		}
		builder.addPropertyReference(JmsAdapterParserUtils.CONNECTION_FACTORY_PROPERTY,
				JmsAdapterParserUtils.determineConnectionFactoryBeanName(element, parserContext));
		if (hasDestination) {
			builder.addPropertyReference("destination", destination);
		}
		else {
			builder.addPropertyValue("destinationName", destinationName);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, pubSubDomainAttribute, "pubSubDomain");
		}
		Integer acknowledgeMode = JmsAdapterParserUtils.parseAcknowledgeMode(element, parserContext);
		if (acknowledgeMode != null) {
			if (acknowledgeMode.intValue() == JmsAdapterParserUtils.SESSION_TRANSACTED) {
				builder.addPropertyValue("sessionTransacted", Boolean.TRUE);
			}
			else {
				builder.addPropertyValue("sessionAcknowledgeMode", acknowledgeMode);
			}
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "transaction-manager");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "selector", "messageSelector");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-messages-per-task");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "recovery-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-consumer-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-task-execution-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "cache-level");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription-durable");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "durable-subscription-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "client-id");
		builder.addPropertyValue("autoStartup", false);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private String parseMessageListener(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.jms.ChannelPublishingJmsMessageListener");
		builder.addPropertyValue("expectReply", this.expectReply);
		if (this.expectReply) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-request-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "correlation-key");
			int defaults = 0;
			if (StringUtils.hasText(element.getAttribute(DEFAULT_REPLY_DESTINATION_ATTRIB))) {
				defaults++;
			}
			if (StringUtils.hasText(element.getAttribute(DEFAULT_REPLY_QUEUE_NAME_ATTRIB))) {
				defaults++;
			}
			if (StringUtils.hasText(element.getAttribute(DEFAULT_REPLY_TOPIC_NAME_ATTRIB))) {
				defaults++;
			}
			if (defaults > 1) {
				parserContext.getReaderContext().error("At most one of '" + DEFAULT_REPLY_DESTINATION_ATTRIB
						+ "', '" + DEFAULT_REPLY_QUEUE_NAME_ATTRIB + "', or '" + DEFAULT_REPLY_TOPIC_NAME_ATTRIB
						+ "' may be provided.", element);
			}
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DEFAULT_REPLY_DESTINATION_ATTRIB);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, DEFAULT_REPLY_QUEUE_NAME_ATTRIB);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, DEFAULT_REPLY_TOPIC_NAME_ATTRIB);
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_TIME_TO_LIVE);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_PRIORITY);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_DELIVERY_PERSISTENT);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, EXPLICIT_QOS_ENABLED_FOR_REPLIES);
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		}
		else {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "channel", "requestChannel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout", "requestTimeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload", "extractRequestPayload");
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
		BeanDefinition beanDefinition = builder.getBeanDefinition();
		String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, parserContext.getRegistry());
		BeanComponentDefinition component = new BeanComponentDefinition(beanDefinition, beanName);
		parserContext.registerBeanComponent(component);
		return beanName;
	}

}

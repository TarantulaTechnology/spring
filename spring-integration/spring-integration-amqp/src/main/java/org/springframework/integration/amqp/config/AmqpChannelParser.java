/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.amqp.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the 'channel' and 'publish-subscribe-channel' elements of the
 * Spring Integration AMQP namespace.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class AmqpChannelParser extends AbstractChannelParser {

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AmqpChannelFactoryBean.class);
		String messageDriven = element.getAttribute("message-driven");
		if (StringUtils.hasText(messageDriven)) {
			builder.addConstructorArgValue(messageDriven);
		}
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "rabbitConnectionFactory";
		}
		builder.addPropertyReference("connectionFactory", connectionFactory);
		if ("channel".equals(element.getLocalName())) {
			builder.addPropertyValue("pubSub", false);
			this.setMaxSubscribersProperty(parserContext, builder, element, IntegrationNamespaceUtils.DEFAULT_MAX_UNICAST_SUBSCRIBERS_PROPERTY_NAME);
		}
		else if ("publish-subscribe-channel".equals(element.getLocalName())) {
			builder.addPropertyValue("pubSub", true);
			this.setMaxSubscribersProperty(parserContext, builder, element, IntegrationNamespaceUtils.DEFAULT_MAX_BROADCAST_SUBSCRIBERS_PROPERTY_NAME);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "acknowledge-mode");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "advice-chain");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "amqp-admin");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "channel-transacted");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "encoding");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-handler");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "exchange");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expose-listener-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-properties-converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "prefetch-count");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "queue-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "recovery-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "shutdown-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "transaction-attribute");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "transaction-manager");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "tx-size");
		return builder;
	}

}

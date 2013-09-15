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

package org.springframework.integration.jms.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.DynamicJmsTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Utility methods and constants for JMS adapter parsers.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
abstract class JmsAdapterParserUtils {

	static final String JMS_TEMPLATE_ATTRIBUTE = "jms-template";

	static final String JMS_TEMPLATE_PROPERTY = "jmsTemplate";

	static final String CONNECTION_FACTORY_ATTRIBUTE = "connection-factory";

	static final String CONNECTION_FACTORY_PROPERTY = "connectionFactory";

	static final String DESTINATION_ATTRIBUTE = "destination";

	static final String DESTINATION_PROPERTY = "destination";

	static final String DESTINATION_NAME_ATTRIBUTE = "destination-name";

	static final String DESTINATION_NAME_PROPERTY = "destinationName";

	static final String DESTINATION_EXPRESSION_ATTRIBUTE = "destination-expression";

	static final String DESTINATION_EXPRESSION_PROPERTY = "destinationExpression";

	static final String PUB_SUB_DOMAIN_ATTRIBUTE = "pub-sub-domain";

	static final String PUB_SUB_DOMAIN_PROPERTY = "pubSubDomain";

	static final String HEADER_MAPPER_ATTRIBUTE = "header-mapper";

	static final String HEADER_MAPPER_PROPERTY = "headerMapper";

	private static final String[] JMS_TEMPLATE_ATTRIBUTES = {
		"connection-factory", "message-converter", "destination-resolver", "pub-sub-domain",
		"time-to-live", "priority", "delivery-persistent", "explicit-qos-enabled", "acknowledge",
		"receive-timeout"
	};


	/*
	 * The following constants match those of javax.jms.Session.
	 * They are duplicated here to avoid a dependency in tooling.
	 */

	static final int SESSION_TRANSACTED = 0;

	private static final int AUTO_ACKNOWLEDGE = 1;

	private static final int CLIENT_ACKNOWLEDGE = 2;

	private static final int DUPS_OK_ACKNOWLEDGE = 3;


	static String determineConnectionFactoryBeanName(Element element, ParserContext parserContext) {
		String connectionFactoryBeanName = "connectionFactory";
		if (element.hasAttribute(CONNECTION_FACTORY_ATTRIBUTE)) {
			connectionFactoryBeanName = element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
			if (!StringUtils.hasText(connectionFactoryBeanName)) {
				parserContext.getReaderContext().error(
						"JMS adapter 'connection-factory' attribute must not be empty", element);
			}
		}
		return connectionFactoryBeanName;
	}

	static Integer parseAcknowledgeMode(Element element, ParserContext parserContext) {
		String acknowledge = element.getAttribute("acknowledge");
		if (StringUtils.hasText(acknowledge)) {
			int acknowledgeMode = AUTO_ACKNOWLEDGE;
			if ("transacted".equals(acknowledge)) {
				acknowledgeMode = SESSION_TRANSACTED;
			}
			else if ("dups-ok".equals(acknowledge)) {
				acknowledgeMode = DUPS_OK_ACKNOWLEDGE;
			}
			else if ("client".equals(acknowledge)) {
				acknowledgeMode = CLIENT_ACKNOWLEDGE;
			}
			else if (!"auto".equals(acknowledge)) {
				parserContext.getReaderContext().error("Invalid JMS 'acknowledge' setting: " +
						"only \"auto\", \"client\", \"dups-ok\" and \"transacted\" supported.", element);
			}
			return acknowledgeMode;
		}
		else {
			return null;
		}
	}

	static BeanDefinition parseJmsTemplateBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DynamicJmsTemplate.class);
		builder.addPropertyReference(JmsAdapterParserUtils.CONNECTION_FACTORY_PROPERTY,
				JmsAdapterParserUtils.determineConnectionFactoryBeanName(element, parserContext));
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "time-to-live");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "priority");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delivery-persistent");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "explicit-qos-enabled");
		String receiveTimeout = element.getAttribute("receive-timeout");
		if (StringUtils.hasText(receiveTimeout)) {
			builder.addPropertyValue("receiveTimeout", receiveTimeout);
		}
		else {
			builder.addPropertyValue("receiveTimeout", JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
		}
		Integer acknowledgeMode = parseAcknowledgeMode(element, parserContext);
		if (acknowledgeMode != null) {
			builder.addPropertyValue("sessionAcknowledgeMode", acknowledgeMode);
		}

		return builder.getBeanDefinition();
	}

	static void verifyNoJmsTemplateAttributes(Element element, ParserContext parserContext) {
		for (String attributeName : JMS_TEMPLATE_ATTRIBUTES) {
			if (element.hasAttribute(attributeName)) {
				parserContext.getReaderContext().error("When providing a 'jms-template' reference, the '"
						+ attributeName + "' attribute is not allowed", parserContext.extractSource(element));
			}
		}
	}

}

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

package org.springframework.integration.ws.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractOutboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ws.DefaultSoapHeaderMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.integration.ws.MarshallingWebServiceOutboundGateway;
import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;

/**
 * Parser for the &lt;outbound-gateway/&gt; element in the 'ws' namespace.
 *
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class WebServiceOutboundGatewayParser extends AbstractOutboundGatewayParser {

	@Override
	protected String getGatewayClassName(Element element) {
		return ((StringUtils.hasText(element.getAttribute("marshaller"))) ?
				MarshallingWebServiceOutboundGateway.class : SimpleWebServiceOutboundGateway.class).getName();
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getGatewayClassName(element));
		String uri = element.getAttribute("uri");
		String destinationProvider = element.getAttribute("destination-provider");
		List<Element> uriVariableElements = DomUtils.getChildElementsByTagName(element, "uri-variable");
		if (!(StringUtils.hasText(destinationProvider) ^ StringUtils.hasText(uri))) {
			parserContext.getReaderContext().error(
					"Exactly one of 'uri' or 'destination-provider' is required.", element);
		}
		if (StringUtils.hasText(destinationProvider)) {
			if (!CollectionUtils.isEmpty(uriVariableElements)) {
				parserContext.getReaderContext().error("No 'uri-variable' sub-elements are allowed when "
						+ "a 'destination-provider' reference has been provided.", element);
			}
			builder.addConstructorArgReference(destinationProvider);
		}
		else {
			builder.addConstructorArgValue(uri);
			if (!CollectionUtils.isEmpty(uriVariableElements)) {
				ManagedMap<String, Object> uriVariableExpressions = new ManagedMap<String, Object>();
				for (Element uriVariableElement : uriVariableElements) {
					String name = uriVariableElement.getAttribute("name");
					String expression = uriVariableElement.getAttribute("expression");
					BeanDefinitionBuilder factoryBeanBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
					factoryBeanBuilder.addConstructorArgValue(expression);
					uriVariableExpressions.put(name,  factoryBeanBuilder.getBeanDefinition());
				}
				builder.addPropertyValue("uriVariableExpressions", uriVariableExpressions);
			}
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-empty-responses");
		this.postProcessGateway(builder, element, parserContext);

		IntegrationNamespaceUtils.configureHeaderMapper(element, builder, parserContext, DefaultSoapHeaderMapper.class, null);

		return builder;
	}

	@Override
	protected void postProcessGateway(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String marshallerRef = element.getAttribute("marshaller");
		if (StringUtils.hasText(marshallerRef)) {
			builder.addConstructorArgReference(marshallerRef);
			String unmarshallerRef = element.getAttribute("unmarshaller");
			if (StringUtils.hasText(unmarshallerRef)) {
				builder.addConstructorArgReference(unmarshallerRef);
			}
		}
		else {
			String sourceExtractorRef = element.getAttribute("source-extractor");
			if (StringUtils.hasText(sourceExtractorRef)) {
				builder.addConstructorArgReference(sourceExtractorRef);
			}
			else {
				builder.addConstructorArgValue(null);
			}
		}
		String messageFactoryRef = element.getAttribute("message-factory");
		if (StringUtils.hasText(messageFactoryRef)) {
			builder.addConstructorArgReference(messageFactoryRef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-callback");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "fault-message-resolver");

		String messageSenderRef = element.getAttribute("message-sender");
		String messageSenderListRef = element.getAttribute("message-senders");
		if (StringUtils.hasText(messageSenderRef) && StringUtils.hasText(messageSenderListRef)) {
			parserContext.getReaderContext().error(
					"Only one of message-sender or message-senders should be specified.", element);
		}
		if (StringUtils.hasText(messageSenderRef)) {
			builder.addPropertyReference("messageSender", messageSenderRef);
		}
		if (StringUtils.hasText(messageSenderListRef)) {
			builder.addPropertyReference("messageSenders", messageSenderListRef);
		}
		String interceptorRef = element.getAttribute("interceptor");
		String interceptorListRef = element.getAttribute("interceptors");
		if (StringUtils.hasText(interceptorRef) && StringUtils.hasText(interceptorListRef)) {
			parserContext.getReaderContext().error(
					"Only one of interceptor or interceptors should be specified.", element);
		}
		if (StringUtils.hasText(interceptorRef)) {
			builder.addPropertyReference("interceptors", interceptorRef);
		}
		if (StringUtils.hasText(interceptorListRef)) {
			builder.addPropertyReference("interceptors", interceptorListRef);
		}
	}

}

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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;annotation-config&gt; element of the integration namespace.
 * Adds a {@link org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor}
 * and a {@link org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor}
 * to the application context.
 * 
 * @author Mark Fisher
 */
public class AnnotationConfigParser implements BeanDefinitionParser {

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		RootBeanDefinition messagingAnnotationPostProcessorDef = new RootBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".config.annotation.MessagingAnnotationPostProcessor");
		messagingAnnotationPostProcessorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		String messagingAnnotationPostProcessorName = IntegrationNamespaceUtils.BASE_PACKAGE + ".internalMessagingAnnotationPostProcessor";
		parserContext.getRegistry().registerBeanDefinition(messagingAnnotationPostProcessorName, messagingAnnotationPostProcessorDef);
		RootBeanDefinition publisherAnnotationPostProcessorDef = new RootBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".aop.PublisherAnnotationBeanPostProcessor");
		publisherAnnotationPostProcessorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		String defaultPublisherChannel = element.getAttribute("default-publisher-channel");
		if (StringUtils.hasText(defaultPublisherChannel)) {
			publisherAnnotationPostProcessorDef.getPropertyValues().add("defaultChannel", new RuntimeBeanReference(defaultPublisherChannel));
		}
		String publisherAnnotationPostProcessorName = IntegrationNamespaceUtils.BASE_PACKAGE + ".internalPublisherAnnotationBeanPostProcessor";
		parserContext.getRegistry().registerBeanDefinition(publisherAnnotationPostProcessorName, publisherAnnotationPostProcessorDef);
		return null;
	}


}

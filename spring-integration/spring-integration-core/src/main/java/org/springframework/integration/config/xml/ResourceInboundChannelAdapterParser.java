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

package org.springframework.integration.config.xml;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.resource.ResourceRetrievingMessageSource;
import org.springframework.integration.util.AcceptOnceCollectionFilter;
import org.springframework.util.StringUtils;

import org.w3c.dom.Element;

/**
 * Parser for 'resource-inbound-channel-adapter' 
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class ResourceInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	
	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(ResourceRetrievingMessageSource.class);
		sourceBuilder.addConstructorArgValue(element.getAttribute("pattern"));
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(sourceBuilder, element, "pattern-resolver");
		boolean hasFilter = element.hasAttribute("filter");
		if (hasFilter){
			String filterValue = element.getAttribute("filter");
			if (StringUtils.hasText(filterValue)){
				sourceBuilder.addPropertyReference("filter", filterValue);
			}
		}
		else {
			BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(AcceptOnceCollectionFilter.class);
			sourceBuilder.addPropertyValue("filter", filterBuilder.getBeanDefinition());
		}
		return sourceBuilder.getBeanDefinition();
	}

}

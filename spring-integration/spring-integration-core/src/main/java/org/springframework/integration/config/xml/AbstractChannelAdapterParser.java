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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.util.StringUtils;

/**
 * Base parser for Channel Adapters.
 *
 * Includes logic to determine {@link org.springframework.integration.MessageChannel}:
 * if 'channel' attribute is defined - uses its value as 'channelName';
 * if 'id' attribute is defined - creates {@link DirectChannel} at runtime and uses id's value as 'channelName';
 * if current component is defined as nested element inside any other components e.g. &lt;chain&gt;
 * 'id' and 'channel' attributes will be ignored and this component will not be parsed as
 * {@link org.springframework.integration.endpoint.AbstractEndpoint}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public abstract class AbstractChannelAdapterParser extends AbstractBeanDefinitionParser {

	@Override
	protected final String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String id = element.getAttribute(ID_ATTRIBUTE);
		if (!element.hasAttribute("channel")) {
			// the created channel will get the 'id', so the adapter's bean name includes a suffix
			id = id + ".adapter";
		}
		else if (!StringUtils.hasText(id)) {
			id = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry(), parserContext.isNested());
		}
		return id;
	}

	@Override
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String channelName = element.getAttribute("channel");
		if (!StringUtils.hasText(channelName)) {
			channelName = this.createDirectChannel(element, parserContext);
		}
		return doParse(element, parserContext, channelName);
	}

	private String createDirectChannel(Element element, ParserContext parserContext) {
		if (parserContext.isNested()) {
			return null;
		}
		String channelId = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(channelId)) {
			parserContext.getReaderContext().error("The channel-adapter's 'id' attribute is required when no 'channel' "
					+ "reference has been provided, because that 'id' would be used for the created channel.", element);
		}
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
		BeanDefinitionHolder holder = new BeanDefinitionHolder(channelBuilder.getBeanDefinition(), channelId);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		return channelId;
	}

	/**
	 * Subclasses must implement this method to parse the adapter element.
	 * The name of the MessageChannel bean is provided.
	 */
	protected abstract AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName);

}

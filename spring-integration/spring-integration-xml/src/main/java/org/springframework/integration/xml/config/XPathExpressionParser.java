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

package org.springframework.integration.xml.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.xml.xpath.XPathExpressionFactory;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;xpath-expression&gt; element.
 *
 * @author Jonas Partner
 * @author Soby Chacko
 */
public class XPathExpressionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return XPathExpressionFactory.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String expression = element.getAttribute("expression");
		Assert.hasText(expression, "The 'expression' attribute is required.");
		String nsPrefix = element.getAttribute("ns-prefix");
		String nsUri = element.getAttribute("ns-uri");
		String namespaceMapRef = element.getAttribute("namespace-map");

		List<Element> mapElements = DomUtils.getChildElementsByTagName(element, "map");

		boolean prefixProvided = StringUtils.hasText(nsPrefix);
		boolean namespaceProvided = StringUtils.hasText(nsUri);
		boolean namespaceMapProvided = StringUtils.hasText(namespaceMapRef);

		boolean mapSubElementProvided = !mapElements.isEmpty();

		if (prefixProvided || namespaceProvided) {
			Assert.isTrue(prefixProvided && namespaceProvided,
					"Both 'ns-prefix' and 'ns-uri' must be specified if one is specified.");
			Assert.isTrue(!namespaceMapProvided, "It is not valid to specify both, the namespace attributes ('ns-prefix' and 'ns-uri') and the 'namespace-map' attribute.");
			Assert.isTrue(!mapSubElementProvided, "It is not valid to specify both, the namespace attributes ('ns-prefix' and 'ns-uri') and the 'map' sub-element.");
		} else if (mapSubElementProvided) {
			Assert.isTrue(!namespaceMapProvided, "It is not valid to specify both, the 'namespace-map' attribute and the 'map' sub-element.");
		}

		builder.setFactoryMethod("createXPathExpression");
		builder.addConstructorArgValue(expression);

		if (prefixProvided) {
			Map<String, String> namespaceMap = new HashMap<String, String>(1);
			namespaceMap.put(nsPrefix, nsUri);
			builder.addConstructorArgValue(namespaceMap);
		} else if (namespaceMapProvided) {
			builder.addConstructorArgReference(namespaceMapRef);
		} else if (mapSubElementProvided) {

			Element mapElement = mapElements.get(0);

			if (mapElement != null) {
				builder.addConstructorArgValue(this.parseNamespaceMapElement(
						mapElement, parserContext, builder.getBeanDefinition()));
			}
		}
	}

	protected Map<?,?> parseNamespaceMapElement(Element element, ParserContext parserContext, BeanDefinition parentDefinition) {
		BeanDefinitionParserDelegate beanParser = parserContext.getDelegate();
		beanParser.initDefaults(element.getOwnerDocument().getDocumentElement(), beanParser);
		return beanParser.parseMapElement(element, parentDefinition);
	}

}

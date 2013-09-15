/*
 * Copyright 2002-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jmx.config;

import java.util.UUID;

import javax.management.MBeanServerFactory;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'mbean-export' element of the integration JMX namespace.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class MBeanExporterParser extends AbstractSingleBeanDefinitionParser {

	private static final String MBEAN_EXPORTER_NAME = "mbeanExporter";


	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return IntegrationMBeanExporter.class.getName();
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		Object mbeanServer = getMBeanServer(element, parserContext);
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-domain");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "object-name-static-properties");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "managed-components", "componentNamePatterns");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "shutdown-executor");
		
		builder.addPropertyValue("server", mbeanServer);
		this.registerMBeanExporterHelper(parserContext.getRegistry());
	}

	private void registerMBeanExporterHelper(BeanDefinitionRegistry registry){
		BeanDefinitionBuilder mBeanExporterHelperBuilder = BeanDefinitionBuilder.rootBeanDefinition(MBeanExporterHelper.class);
		BeanDefinitionReaderUtils.registerWithGeneratedName(mBeanExporterHelperBuilder.getBeanDefinition(), registry);
	}
	
	private Object getMBeanServer(Element element, ParserContext parserContext) {
		String mbeanServer = element.getAttribute("server");
		if (StringUtils.hasText(mbeanServer)) {
			return new RuntimeBeanReference(mbeanServer);
		}
		else {
			return MBeanServerFactory.createMBeanServer();
		}
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);
		if (MBEAN_EXPORTER_NAME.equals(id)) {
			parserContext.getReaderContext().error(
					"Illegal bean id for <jmx:mbean-export/>: " + MBEAN_EXPORTER_NAME +
					" (clashes with <context:mbean-export/> default).  Please choose another bean id.",
					definition);
		}
		if (id.matches(IntegrationMBeanExporter.class.getName() + "#[0-9]+")) {
			//	Randomize the name in case there are multiple contexts in the same JVM
			id += "#" + UUID.randomUUID();
		}
		return id;
	}

}

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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;

import org.junit.Test;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.0
 */
public class DatatypeChannelTests {

	@Test
	public void supportedType() {
		MessageChannel channel = createChannel(String.class);
		assertTrue(channel.send(new GenericMessage<String>("test")));
	}

	@Test(expected = MessageDeliveryException.class)
	public void unsupportedTypeAndNoConversionService() {
		MessageChannel channel = createChannel(Integer.class);
		channel.send(new GenericMessage<String>("123"));
	}

	@Test
	public void unsupportedTypeButConversionServiceSupports() {
		QueueChannel channel = createChannel(Integer.class);
		ConversionService conversionService = new DefaultConversionService();
		channel.setConversionService(conversionService);
		assertTrue(channel.send(new GenericMessage<String>("123")));
	}

	@Test(expected = MessageDeliveryException.class)
	public void unsupportedTypeAndConversionServiceDoesNotSupport() {
		QueueChannel channel = createChannel(Integer.class);
		ConversionService conversionService = new DefaultConversionService();
		channel.setConversionService(conversionService);
		assertTrue(channel.send(new GenericMessage<Boolean>(Boolean.TRUE)));
	}

	@Test
	public void unsupportedTypeButCustomConversionServiceSupports() {
		QueueChannel channel = createChannel(Integer.class);
		GenericConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<Boolean, Integer>() {
			public Integer convert(Boolean source) {
				return source ? 1 : 0;
			}
		});
		channel.setConversionService(conversionService);
		assertTrue(channel.send(new GenericMessage<Boolean>(Boolean.TRUE)));
		assertEquals(1, channel.receive().getPayload());
	}

	@Test
	public void conversionServiceBeanUsedByDefault() {
		GenericApplicationContext context = new GenericApplicationContext();
		Converter<Boolean, Integer> converter = new Converter<Boolean, Integer>() {
			public Integer convert(Boolean source) {
				return source ? 1 : 0;
			}
		};
		BeanDefinitionBuilder conversionServiceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(ConversionServiceFactoryBean.class);
		conversionServiceBuilder.addPropertyValue("converters", Collections.singleton(converter));
		context.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
				conversionServiceBuilder.getBeanDefinition());
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(QueueChannel.class);
		channelBuilder.addPropertyValue("datatypes", "java.lang.Integer, java.util.Date");
		context.registerBeanDefinition("testChannel", channelBuilder.getBeanDefinition());
		context.refresh();

		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		assertTrue(channel.send(new GenericMessage<Boolean>(Boolean.TRUE)));
		assertEquals(1, channel.receive().getPayload());
	}

	@Test
	public void conversionServiceReferenceOverridesDefault() {
		GenericApplicationContext context = new GenericApplicationContext();
		Converter<Boolean, Integer> defaultConverter = new Converter<Boolean, Integer>() {
			public Integer convert(Boolean source) {
				return source ? 1 : 0;
			}
		};
		GenericConversionService customConversionService = new DefaultConversionService();
		customConversionService.addConverter(new Converter<Boolean, Integer>() {
			public Integer convert(Boolean source) {
				return source ? 99 : -99;
			}
		});
		BeanDefinitionBuilder conversionServiceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(ConversionServiceFactoryBean.class);
		conversionServiceBuilder.addPropertyValue("converters", Collections.singleton(defaultConverter));
		context.registerBeanDefinition("conversionService", conversionServiceBuilder.getBeanDefinition());
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(QueueChannel.class);
		channelBuilder.addPropertyValue("datatypes", "java.lang.Integer, java.util.Date");
		channelBuilder.addPropertyValue("conversionService", customConversionService);
		context.registerBeanDefinition("testChannel", channelBuilder.getBeanDefinition());
		context.refresh();

		QueueChannel channel = context.getBean("testChannel", QueueChannel.class);
		assertTrue(channel.send(new GenericMessage<Boolean>(Boolean.TRUE)));
		assertEquals(99, channel.receive().getPayload());
	}

	@Test
	public void multipleTypes() {
		MessageChannel channel = createChannel(String.class, Integer.class);
		assertTrue(channel.send(new GenericMessage<String>("test1")));
		assertTrue(channel.send(new GenericMessage<Integer>(2)));
		Exception exception = null;
		try {
			channel.send(new GenericMessage<Date>(new Date()));
		}
		catch (MessageDeliveryException e) {
			exception = e;
		}
		assertNotNull(exception);
	}

	@Test
	public void subclassOfAcceptedType() {
		MessageChannel channel = createChannel(RuntimeException.class);
		assertTrue(channel.send(new ErrorMessage(new MessagingException("test"))));
	}

	@Test(expected = MessageDeliveryException.class)
	public void superclassOfAcceptedTypeNotAccepted() {
		MessageChannel channel = createChannel(RuntimeException.class);
		channel.send(new ErrorMessage(new Exception("test")));
	}


	private static QueueChannel createChannel(Class<?> ... datatypes) {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		channel.setDatatypes(datatypes);
		return channel;
	}

}

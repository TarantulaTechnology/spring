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

package org.springframework.integration.aggregator;

import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.store.MessageGroup;

/**
 * A {@link MessageGroupProcessor} implementation that evaluates a SpEL expression. The SpEL context root is the list of
 * all Messages in the group. The evaluation result can be any Object and is send as new Message payload to the output
 * channel.
 * 
 * @author Alex Peters
 * @author Dave Syer
 */
public class ExpressionEvaluatingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor implements BeanFactoryAware {

	private final ExpressionEvaluatingMessageListProcessor processor;


	public ExpressionEvaluatingMessageGroupProcessor(String expression) {
		processor = new ExpressionEvaluatingMessageListProcessor(expression);
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		processor.setBeanFactory(beanFactory);
	}

	public void setConversionService(ConversionService conversionService) {
		processor.setConversionService(conversionService);
	}

	public void setExpectedType(Class<?> expectedType) {
		processor.setExpectedType(expectedType);
	}

	/**
	 * Evaluate the expression provided on the messages (a collection) in the group, and delegate to the
	 * {@link MessagingTemplate} to send downstream.
	 */
	@Override
	protected Object aggregatePayloads(MessageGroup group, Map<String, Object> headers) {
		return processor.process(group.getMessages());
	}

}

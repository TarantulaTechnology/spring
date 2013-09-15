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

package org.springframework.integration.splitter;

import java.util.Collection;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.util.Assert;

/**
 * Base class for Message Splitter implementations that delegate to a
 * {@link MessageProcessor} instance.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
abstract class AbstractMessageProcessingSplitter extends AbstractMessageSplitter {

	private final MessageProcessor<Collection<?>> messageProcessor;


	protected AbstractMessageProcessingSplitter(MessageProcessor<Collection<?>> expressionEvaluatingMessageProcessor) {
		Assert.notNull(expressionEvaluatingMessageProcessor, "messageProcessor must not be null");
		this.messageProcessor = expressionEvaluatingMessageProcessor;
	}

	@Override
	public void onInit() {
		super.onInit();
		ConversionService conversionService = this.getConversionService();
		if (conversionService != null && this.messageProcessor instanceof AbstractMessageProcessor) {
			((AbstractMessageProcessor<?>) this.messageProcessor).setConversionService(conversionService);
		}
		if (this.messageProcessor instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.messageProcessor).setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected final Object splitMessage(Message<?> message) {
		return this.messageProcessor.processMessage(message);
	}

}

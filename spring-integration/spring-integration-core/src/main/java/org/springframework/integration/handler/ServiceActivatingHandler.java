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

package org.springframework.integration.handler;

import java.lang.reflect.Method;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.annotation.ServiceActivator;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class ServiceActivatingHandler extends AbstractReplyProducingMessageHandler {

	private final MessageProcessor<?> processor;


	public ServiceActivatingHandler(final Object object) {
		this(new MethodInvokingMessageProcessor<Object>(object, ServiceActivator.class));
	}

	public ServiceActivatingHandler(Object object, Method method) {
		this(new MethodInvokingMessageProcessor<Object>(object, method));
	}

	public ServiceActivatingHandler(Object object, String methodName) {
		this(new MethodInvokingMessageProcessor<Object>(object, methodName));
	}

	public <T> ServiceActivatingHandler(MessageProcessor<T> processor) {
		this.processor = processor;
	}


	@Override
	public String getComponentType() {
		return "service-activator";
	}

	@Override
	public final void onInit() {
		super.onInit();
		if (processor instanceof AbstractMessageProcessor) {
			((AbstractMessageProcessor<?>) this.processor).setConversionService(this.getConversionService());
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		try {
			return this.processor.processMessage(message);
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new MessageHandlingException(message, "failure occurred in Service Activator '" + this + "'", e);
		}
	}

	@Override
	public String toString() {
		return "ServiceActivator for [" + this.processor + "]";
	}

}

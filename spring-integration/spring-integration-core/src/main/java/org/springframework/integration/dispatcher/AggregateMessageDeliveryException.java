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

package org.springframework.integration.dispatcher;

import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * An Exception that encapsulates an aggregated group of Exceptions for use by dispatchers
 * that may try multiple handler invocations within a single dispatch operation.
 * 
 * @author Mark Fisher
 * @since 1.0.3
 */
@SuppressWarnings("serial")
public class AggregateMessageDeliveryException extends MessageDeliveryException {

	private final List<? extends Exception> aggregatedExceptions;


	public AggregateMessageDeliveryException(Message<?> undeliveredMessage,
			String description, List<? extends Exception> aggregatedExceptions) {
		super(undeliveredMessage, description);
		this.initCause(aggregatedExceptions.get(0));
		this.aggregatedExceptions = aggregatedExceptions;
	}

	public List<? extends Exception> getAggregatedExceptions() {
		return Collections.unmodifiableList(this.aggregatedExceptions);
	}

	@Override
	public String getMessage() {
		String baseMessage = super.getMessage();
		StringBuilder message = new StringBuilder(appendPeriodIfNecessary(baseMessage) + " Multiple causes:\n");
		for (Exception exception : aggregatedExceptions) {
			message.append("    " + exception.getMessage() + "\n");
		}
		message.append("See below for the stacktrace of the first cause.");
		return message.toString();
	}

	private String appendPeriodIfNecessary(String baseMessage) {
		if (!StringUtils.hasText(baseMessage)) {
			baseMessage = "";
		}
		else if (!baseMessage.endsWith(".")) {
			baseMessage = baseMessage + ".";
		}
		return baseMessage;
	}

}

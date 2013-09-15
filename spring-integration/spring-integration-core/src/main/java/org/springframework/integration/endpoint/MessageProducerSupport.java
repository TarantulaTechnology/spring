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

package org.springframework.integration.endpoint;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.util.Assert;

/**
 * A support class for producer endpoints that provides a setter for the
 * output channel and a convenience method for sending Messages.
 * 
 * @author Mark Fisher
 */
public abstract class MessageProducerSupport extends AbstractEndpoint implements MessageProducer, TrackableComponent {

	private volatile MessageChannel outputChannel;

	private volatile MessageChannel errorChannel;

	private volatile boolean shouldTrack = false;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.outputChannel, "outputChannel is required");
	}

	/**
	 * Takes no action by default. Subclasses may override this if they
	 * need lifecycle-managed behavior.
	 */
	@Override
	protected void doStart() {
	}

	/**
	 * Takes no action by default. Subclasses may override this if they
	 * need lifecycle-managed behavior.
	 */
	@Override
	protected void doStop() {
	}

	protected void sendMessage(Message<?> message) {
		if (message == null) {
			throw new MessagingException("cannot send a null message");
		}
		if (this.shouldTrack) {
			message = MessageHistory.write(message, this);
		}
		try {
			this.messagingTemplate.send(this.outputChannel, message);
		}
		catch (Exception e) {
			if (this.errorChannel != null) {
				this.messagingTemplate.send(this.errorChannel, new ErrorMessage(e));
			}
			else if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			else {
				throw new MessageDeliveryException(message, "failed to send message", e);
			}
		}
	}

}

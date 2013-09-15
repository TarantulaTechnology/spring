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

package org.springframework.integration.filter;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.handler.AbstractReplyProducingPostProcessingMessageHandler;
import org.springframework.util.Assert;

/**
 * Message Handler that delegates to a {@link MessageSelector}. If and only if
 * the selector {@link MessageSelector#accept(Message) accepts} the Message, it
 * will be passed to this filter's output channel. Otherwise the message will
 * either be silently dropped (the default) or will trigger the throwing of a
 * {@link MessageRejectedException} depending on the value of its
 * {@link #throwExceptionOnRejection} property. If a discard channel is
 * provided, the rejected Messages will be sent to that channel.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class MessageFilter extends AbstractReplyProducingPostProcessingMessageHandler {

	private final MessageSelector selector;

	private volatile boolean throwExceptionOnRejection;

	private volatile MessageChannel discardChannel;


	/**
	 * Create a MessageFilter that will delegate to the given
	 * {@link MessageSelector}.
	 */
	public MessageFilter(MessageSelector selector) {
		Assert.notNull(selector, "selector must not be null");
		this.selector = selector;
	}


	/**
	 * Specify whether this filter should throw a
	 * {@link MessageRejectedException} when its selector does not accept a
	 * Message. The default value is <code>false</code> meaning that rejected
	 * Messages will be quietly dropped or sent to the discard channel if
	 * available. Typically this value would not be <code>true</code> when
	 * a discard channel is provided, but if so, it will still apply
	 * (in such a case, the Message will be sent to the discard channel,
	 * and <em>then</em> the exception will be thrown).
	 * @see #setDiscardChannel(MessageChannel)
	 */
	public void setThrowExceptionOnRejection(boolean throwExceptionOnRejection) {
		this.throwExceptionOnRejection = throwExceptionOnRejection;
	}

	/**
	 * Specify a channel where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped. However,
	 * the 'throwExceptionOnRejection' flag determines whether rejected Messages
	 * trigger an exception. That value is evaluated regardless of the presence
	 * of a discard channel.
	 * @see #setThrowExceptionOnRejection(boolean)
	 */
	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	/**
	 * Set to 'true' if you wish the discard processing to occur within any
	 * request handler advice applied to this filter. Also applies to
	 * throwing an exception on rejection. Default: true.
	 */
	public void setDiscardWithinAdvice(boolean discardWithinAdvice) {
		this.setPostProcessWithinAdvice(discardWithinAdvice);
	}

	@Override
	public String getComponentType() {
		return "filter";
	}

	@Override
	public final void onInit() {
		super.onInit();
		if (this.selector instanceof AbstractMessageProcessingSelector) {
			((AbstractMessageProcessingSelector) this.selector).setConversionService(this.getConversionService());
		}
		if (this.selector instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.selector).setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object doHandleRequestMessage(Message<?> message) {
		if (this.selector.accept(message)) {
			return message;
		}
		else {
			return null;
		}
	}

	@Override
	public Object postProcess(Message<?> message, Object result) {
		if (result == null) {
			if (this.discardChannel != null) {
				this.getMessagingTemplate().send(this.discardChannel, message);
			}
			if (this.throwExceptionOnRejection) {
				throw new MessageRejectedException(message, "MessageFilter '" + this.getComponentName() + "' rejected Message");
			}
		}
		return result;
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

}

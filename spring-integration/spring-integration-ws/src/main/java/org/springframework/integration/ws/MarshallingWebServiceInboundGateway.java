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

package org.springframework.integration.ws;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.support.MarshallingUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.2
 */
public class MarshallingWebServiceInboundGateway extends AbstractWebServiceInboundGateway {

	private volatile Marshaller marshaller;

	private volatile Unmarshaller unmarshaller;

	/**
	 * Creates a new <code>MarshallingWebServiceInboundGateway</code>.
	 * The {@link Marshaller} and {@link Unmarshaller} must be injected using properties.
	 */
	public MarshallingWebServiceInboundGateway() {
	}

	/**
	 * Creates a new <code>MarshallingWebServiceInboundGateway</code> with the given marshaller.
	 * The Marshaller must also implement {@link Unmarshaller}, since it is used for both marshalling and
	 * unmarshalling.
	 * <p>
	 * Note that all {@link Marshaller} implementations in Spring-OXM also implement the {@link Unmarshaller}
	 * interface, so you can safely use this constructor for any of those implementations.
	 *
	 * @param marshaller object used as marshaller and unmarshaller
	 * @throws IllegalArgumentException when <code>marshaller</code> does not implement {@link Unmarshaller}
	 * @see #MarshallingWebServiceInboundGateway(Marshaller, Unmarshaller)
	 */
	public MarshallingWebServiceInboundGateway(Marshaller marshaller) {
		Assert.notNull(marshaller, "'marshaller' must no be null");
		Assert.isInstanceOf(Unmarshaller.class, marshaller, "When using this constructor the provided " +
				"Marshaller must also implement Unmarshaller");
		this.marshaller = marshaller;
		this.unmarshaller = (Unmarshaller) marshaller;
	}

	/**
	 * Creates a new <code>MarshallingWebServiceInboundGateway</code> with the given marshaller and unmarshaller.
	 */
	public MarshallingWebServiceInboundGateway(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "'marshaller' must no be null");
		Assert.notNull(unmarshaller, "'unmarshaller' must no be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}

	public void setMarshaller(Marshaller marshaller) {
		Assert.notNull(marshaller, "'marshaller' must no be null");
		this.marshaller = marshaller;
	}

	public void setUnmarshaller(Unmarshaller unmarshaller) {
		Assert.notNull(unmarshaller, "'unmarshaller' must no be null");
		this.unmarshaller = unmarshaller;
	}

	protected void onInit() throws Exception {
		super.onInit();
		Assert.notNull(marshaller, "This implementation requires Marshaller");
		Assert.notNull(unmarshaller, "This implementation requires Unmarshaller");
	}

	@Override
	protected void doInvoke(MessageContext messageContext) throws Exception{
		WebServiceMessage request = messageContext.getRequest();
		Assert.notNull(request, "Invalid message context: request was null.");
		Object requestObject = MarshallingUtils.unmarshal(unmarshaller, request);
		MessageBuilder<?> builder = MessageBuilder.withPayload(requestObject);

		this.fromSoapHeaders(messageContext, builder);

		Message<?> replyMessage = this.sendAndReceiveMessage(builder.build());

		if (replyMessage != null) {
			WebServiceMessage response = messageContext.getResponse();
			this.toSoapHeaders(response, replyMessage);

			MarshallingUtils.marshal(marshaller, replyMessage.getPayload(), response);
		}
	}
}

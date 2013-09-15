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

package org.springframework.integration.xmpp.core;

import org.jivesoftware.smack.XMPPConnection;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractXmppConnectionAwareEndpoint extends MessageProducerSupport {

	protected volatile XMPPConnection xmppConnection;

	protected volatile boolean initialized;


	public AbstractXmppConnectionAwareEndpoint() {
	}

	public AbstractXmppConnectionAwareEndpoint(XMPPConnection xmppConnection) {
		Assert.notNull(xmppConnection, "'xmppConnection' must no be null");
		this.xmppConnection = xmppConnection;
	}

	/**
	 * {@link Deprecated} This method will be eligible for removal in 2.1.
	 * Use {@link #setOutputChannel(MessageChannel)} instead.
	 */
	@Deprecated
	public void setRequestChannel(MessageChannel requestChannel) {
		this.setOutputChannel(requestChannel);
	}

	@Override
	protected void onInit() {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (this.xmppConnection == null && beanFactory != null) {
			this.xmppConnection = beanFactory.getBean(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, XMPPConnection.class);
		}
		Assert.notNull(this.xmppConnection, "Failed to resolve XMPPConnection. XMPPConnection must either be set expicitly " +
				"via the 'xmpp-connection' attribute or implicitly by registering a bean with the name 'xmppConnection' and of type " +
				"'org.jivesoftware.smack.XMPPConnection' in the Application Context.");
		this.initialized = true;
	}

}

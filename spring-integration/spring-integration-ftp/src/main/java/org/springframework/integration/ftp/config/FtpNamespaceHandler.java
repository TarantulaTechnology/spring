/*
 * Copyright 2002-2013 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.ftp.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;
import org.springframework.integration.file.config.RemoteFileOutboundChannelAdapterParser;

/**
 * Provides namespace support for using FTP
 * <p>
 * This is *heavily* influenced by the good work done by Iwein before.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FtpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new FtpInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new RemoteFileOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new FtpOutboundGatewayParser());
	}

}

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

package org.springframework.integration.rmi.config;

import java.rmi.registry.Registry;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundGatewayParser;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-gateway/&gt; element of the 'rmi' namespace. 
 * 
 * @author Mark Fisher
 */
public class RmiOutboundGatewayParser extends AbstractOutboundGatewayParser {

	@Override
	protected String getGatewayClassName(Element element) {
		return "org.springframework.integration.rmi.RmiOutboundGateway";
	}

	@Override
	protected String parseUrl(Element element, ParserContext parserContext) {
		String host = element.getAttribute("host");
		String remoteChannel = element.getAttribute("remote-channel");
		if (!StringUtils.hasText(host) || !StringUtils.hasText(remoteChannel)) {
			parserContext.getReaderContext().error(
					"The 'host' and 'remote-channel' attributes are both required", element);
		}
		String portAttribute = element.getAttribute("port");
		String port = StringUtils.hasText(portAttribute) ? portAttribute : "" + Registry.REGISTRY_PORT;
		return "rmi://" + host + ":" + port + "/" + RmiInboundGateway.SERVICE_NAME_PREFIX + remoteChannel;
	}

}

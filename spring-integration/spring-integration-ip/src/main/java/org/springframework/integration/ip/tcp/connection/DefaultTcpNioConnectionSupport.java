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
package org.springframework.integration.ip.tcp.connection;

import java.nio.channels.SocketChannel;

import org.springframework.context.ApplicationEventPublisher;


/**
 * Implementation of {@link TcpNioConnectionSupport} for non-SSL
 * NIO connections.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class DefaultTcpNioConnectionSupport implements TcpNioConnectionSupport {

	public TcpNioConnection createNewConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
			ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) throws Exception {
		return new TcpNioConnection(socketChannel, server, lookupHost, applicationEventPublisher, connectionFactoryName);
	}

}

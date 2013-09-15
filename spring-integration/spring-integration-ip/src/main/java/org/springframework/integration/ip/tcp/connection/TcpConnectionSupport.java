/*
 * Copyright 2001-2013 the original author or authors.
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

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.util.Assert;

/**
 * Base class for TcpConnections. TcpConnections are established by
 * client connection factories (outgoing) or server connection factories
 * (incoming).
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public abstract class TcpConnectionSupport implements TcpConnection {

	protected final Log logger = LogFactory.getLog(this.getClass());

	@SuppressWarnings("rawtypes")
	private volatile Deserializer deserializer;

	@SuppressWarnings("rawtypes")
	private volatile Serializer serializer;

	private volatile TcpMessageMapper mapper;

	private volatile TcpListener listener;

	private volatile TcpListener actualListener;

	private volatile TcpSender sender;

	private volatile boolean singleUse;

	private final boolean server;

	private volatile String connectionId;

	private final AtomicLong sequence = new AtomicLong();

	private volatile int soLinger = -1;

	private volatile String hostName = "unknown";

	private volatile String hostAddress = "unknown";

	private volatile String connectionFactoryName = "unknown";

	private final ApplicationEventPublisher applicationEventPublisher;

	private final AtomicBoolean closePublished = new AtomicBoolean();

	private final AtomicBoolean exceptionSent = new AtomicBoolean();

	public TcpConnectionSupport() {
		this.server = false;
		this.applicationEventPublisher = null;
	}

	/**
	 * Creates a {@link TcpConnectionSupport} object and publishes a
	 * {@link TcpConnectionOpenEvent}, if an event publisher is provided.
	 * @param socket the underlying socket.
	 * @param server true if this connection is a server connection
	 * @param lookupHost true if reverse lookup of the host name should be performed,
	 * otherwise, the ip address will be used for identification purposes.
	 * @param applicationEventPublisher the publisher to which open, close and exception events will
	 * be sent; may be null if event publishing is not required.
	 * @param connectionFactoryName the name of the connection factory creating this connection; used
	 * during event publishing, may be null, in which case "unknown" will be used.
	 */
	public TcpConnectionSupport(Socket socket, boolean server, boolean lookupHost,
			ApplicationEventPublisher applicationEventPublisher,
			String connectionFactoryName) {
		this.server = server;
		InetAddress inetAddress = socket.getInetAddress();
		if (inetAddress != null) {
			this.hostAddress = inetAddress.getHostAddress();
			if (lookupHost) {
				this.hostName = inetAddress.getHostName();
			} else {
				this.hostName = this.hostAddress;
			}
		}
		int port = socket.getPort();
		this.connectionId = this.hostName + ":" + port + ":" + UUID.randomUUID().toString();
		try {
			this.soLinger = socket.getSoLinger();
		} catch (SocketException e) { }
		this.applicationEventPublisher = applicationEventPublisher;
		if (connectionFactoryName != null) {
			this.connectionFactoryName = connectionFactoryName;
		}
		this.publishConnectionOpenEvent();
		if (logger.isDebugEnabled()) {
			logger.debug("New connection " + this.getConnectionId());
		}
	}

	public void afterSend(Message<?> message) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Message sent " + message);
		}
		if (this.singleUse) {
			// if (we're a server socket, or a send-only socket), and soLinger <> 0, close
			if ((this.isServer() || this.actualListener == null) && this.soLinger != 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("Closing single-use connection" + this.getConnectionId());
				}
				this.closeConnection();
			}
		}
	}

	/**
	 * Closes this connection.
	 */
	public void close() {
		if (this.sender != null) {
			this.sender.removeDeadConnection(this);
		}
		// close() may be called multiple times; only publish once
		if (!this.closePublished.getAndSet(true)) {
			this.publishConnectionCloseEvent();
		}
	}

	/**
	 * If we have been intercepted, propagate the close from the outermost interceptor;
	 * otherwise, just call close().
	 */
	protected void closeConnection() {
		if (!(this.listener instanceof TcpConnectionInterceptor)) {
			close();
			return;
		}
		TcpConnectionInterceptor outerInterceptor = (TcpConnectionInterceptor) this.listener;
		while (outerInterceptor.getListener() instanceof TcpConnectionInterceptor) {
			outerInterceptor = (TcpConnectionInterceptor) outerInterceptor.getListener();
		}
		outerInterceptor.close();
	}

	/**
	 * @return the mapper
	 */
	public TcpMessageMapper getMapper() {
		return mapper;
	}

	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(TcpMessageMapper mapper) {
		Assert.notNull(mapper, this.getClass().getName() + " Mapper may not be null");
		this.mapper = mapper;
		if (this.serializer != null &&
			 !(this.serializer instanceof AbstractByteArraySerializer)) {
			mapper.setStringToBytes(false);
		}
	}

	/**
	 *
	 * @return the deserializer
	 */
	public Deserializer<?> getDeserializer() {
		return this.deserializer;
	}

	/**
	 * @param deserializer the deserializer to set
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 *
	 * @return the serializer
	 */
	public Serializer<?> getSerializer() {
		return this.serializer;
	}

	/**
	 * @param serializer the serializer to set
	 */
	public void setSerializer(Serializer<?> serializer) {
		this.serializer = serializer;
		if (!(serializer instanceof AbstractByteArraySerializer)) {
			this.mapper.setStringToBytes(false);
		}
	}

	/**
	 * Sets the listener that will receive incoming Messages.
	 * @param listener The listener.
	 */
	public void registerListener(TcpListener listener) {
		this.listener = listener;
		// Determine the actual listener for this connection
		if (!(this.listener instanceof TcpConnectionInterceptor)) {
			this.actualListener = this.listener;
		} else {
			TcpConnectionInterceptor outerInterceptor = (TcpConnectionInterceptor) this.listener;
			while (outerInterceptor.getListener() instanceof TcpConnectionInterceptor) {
				outerInterceptor = (TcpConnectionInterceptor) outerInterceptor.getListener();
			}
			this.actualListener = outerInterceptor.getListener();
		}
	}

	/**
	 * Registers a sender. Used on server side connections so a
	 * sender can determine which connection to send a reply
	 * to.
	 * @param sender the sender.
	 */
	public void registerSender(TcpSender sender) {
		this.sender = sender;
		if (sender != null) {
			sender.addNewConnection(this);
		}
	}

	/**
	 * @return the listener
	 */
	public TcpListener getListener() {
		return this.listener;
	}

	/**
	 * @return the sender
	 */
	public TcpSender getSender() {
		return sender;
	}

	/**
	 * @param singleUse true if this socket is to used once and
	 * discarded.
	 */
	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	/**
	 *
	 * @return True if connection is used once.
	 */
	public boolean isSingleUse() {
		return this.singleUse;
	}

	public boolean isServer() {
		return server;
	}

	public long incrementAndGetConnectionSequence() {
		return this.sequence.incrementAndGet();
	}

	public String getHostAddress() {
		return this.hostAddress;
	}

	public String getHostName() {
		return this.hostName;
	}

	public String getConnectionId() {
		return this.connectionId;
	}

	protected final void sendExceptionToListener(Exception e) {
		if (!this.exceptionSent.getAndSet(true) && this.getListener() != null) {
			Map<String, Object> headers = Collections.singletonMap(IpHeaders.CONNECTION_ID,
					(Object) this.getConnectionId());
			ErrorMessage errorMessage = new ErrorMessage(e, headers);
			this.getListener().onMessage(errorMessage);
		}
	}

	protected void publishConnectionOpenEvent() {
		TcpConnectionEvent event = new TcpConnectionOpenEvent(this,
				this.connectionFactoryName);
		doPublish(event);
	}

	protected void publishConnectionCloseEvent() {
		TcpConnectionEvent event = new TcpConnectionCloseEvent(this,
				this.connectionFactoryName);
		doPublish(event);
	}

	protected void publishConnectionExceptionEvent(Throwable t) {
		TcpConnectionEvent event = new TcpConnectionExceptionEvent(this,
				this.connectionFactoryName, t);
		doPublish(event);
	}

	/**
	 * Allow interceptors etc to publish events, perhaps subclasses of
	 * TcpConnectionEvent. The event source must be this connection.
	 * @param event the event to publish.
	 */
	public void publishEvent(TcpConnectionEvent event) {
		Assert.isTrue(event.getSource() == this, "Can only publish events with this as the source");
		this.doPublish(event);
	}

	private void doPublish(TcpConnectionEvent event) {
		try {
			if (this.applicationEventPublisher == null) {
				logger.warn("No publisher available to publish " + event);
			}
			else {
				this.applicationEventPublisher.publishEvent(event);
			}
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to publish " + event, e);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn("Failed to publish " + event + ":" + e.getMessage());
			}
		}
	}

}

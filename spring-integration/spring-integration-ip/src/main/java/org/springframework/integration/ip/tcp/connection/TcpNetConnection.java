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

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.Message;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;

/**
 * A TcpConnection that uses and underlying {@link Socket}.
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNetConnection extends TcpConnectionSupport {

	private final Socket socket;

	private boolean noReadErrorOnClose;

	private volatile long lastRead = System.currentTimeMillis();

	private volatile long lastSend;

	/**
	 * Constructs a TcpNetConnection for the socket.
	 * @param socket the socket
	 * @param server if true this connection was created as
	 * a result of an incoming request.
	 * @param lookupHost true if hostname lookup should be performed, otherwise the connection will
	 * be identified using the ip address.
	 * @deprecated Use {@link #TcpNetConnection(Socket, boolean, boolean, ApplicationEventPublisher, String)}
	 * TODO: Remove in 3.1/4.0
	 */
	@Deprecated
	public TcpNetConnection(Socket socket, boolean server, boolean lookupHost) {
		this(socket, server, lookupHost, null, null);
	}

	/**
	 * Constructs a TcpNetConnection for the socket.
	 * @param socket the socket
	 * @param server if true this connection was created as
	 * a result of an incoming request.
	 * @param lookupHost true if hostname lookup should be performed, otherwise the connection will
	 * be identified using the ip address.
	 * @param applicationEventPublisher the publisher to which OPEN, CLOSE and EXCEPTION events will
	 * be sent; may be null if event publishing is not required.
	 * @param connectionFactoryName the name of the connection factory creating this connection; used
	 * during event publishing, may be null, in which case "unknown" will be used.
	 */
	public TcpNetConnection(Socket socket, boolean server, boolean lookupHost,
			ApplicationEventPublisher applicationEventPublisher, String connectionFactoryName) {
		super(socket, server, lookupHost, applicationEventPublisher, connectionFactoryName);
		this.socket = socket;
	}

	/**
	 * Closes this connection.
	 */
	@Override
	public void close() {
		this.noReadErrorOnClose = true;
		try {
			this.socket.close();
		} catch (Exception e) {}
		super.close();
	}

	public boolean isOpen() {
		return !this.socket.isClosed();
	}

	@SuppressWarnings("unchecked")
	public synchronized void send(Message<?> message) throws Exception {
		Object object = this.getMapper().fromMessage(message);
		this.lastSend = System.currentTimeMillis();
		try {
			((Serializer<Object>) this.getSerializer()).serialize(object, this.socket.getOutputStream());
		}
		catch (Exception e) {
			this.publishConnectionExceptionEvent(e);
			throw e;
		}
		this.afterSend(message);
	}

	public Object getPayload() throws Exception {
		return this.getDeserializer().deserialize(this.socket.getInputStream());
	}

	public int getPort() {
		return this.socket.getPort();
	}

	public Object getDeserializerStateKey() {
		try {
			return this.socket.getInputStream();
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * If there is no listener, and this connection is not for single use,
	 * this method exits. When there is a listener, the method runs in a
	 * loop reading input from the connections's stream, data is converted
	 * to an object using the {@link Deserializer} and the listener's
	 * {@link TcpListener#onMessage(Message)} method is called. For single use
	 * connections with no listener, the socket is closed after its timeout
	 * expires. If data is received on a single use socket with no listener,
	 * a warning is logged.
	 */
	public void run() {
		boolean singleUse = this.isSingleUse();
		TcpListener listener = this.getListener();
		if (listener == null && !singleUse) {
			logger.debug("TcpListener exiting - no listener and not single use");
			return;
		}
		boolean okToRun = true;
		if (logger.isDebugEnabled()) {
			logger.debug(this.getConnectionId() + " Reading...");
		}
		boolean intercepted = false;
		while (okToRun) {
			Message<?> message = null;
			try {
				message = this.getMapper().toMessage(this);
				this.lastRead = System.currentTimeMillis();
			}
			catch (Exception e) {
				this.publishConnectionExceptionEvent(e);
				if (handleReadException(e)) {
					okToRun = false;
				}
			}
			if (okToRun && message != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Message received " + message);
				}
				try {
					if (listener == null) {
						logger.warn("Unexpected message - no inbound adapter registered with connection " + message);
						continue;
					}
					intercepted = this.getListener().onMessage(message);
				}
				catch (NoListenerException nle) {
					if (singleUse) {
						logger.debug("Closing single use socket after inbound message " + this.getConnectionId());
						this.closeConnection();
						okToRun = false;
					} else {
						logger.warn("Unexpected message - no inbound adapter registered with connection " + message);
					}
				}
				catch (Exception e2) {
					logger.error("Exception sending message: " + message, e2);
				}
				/*
				 * For single use sockets, we close after receipt if we are on the client
				 * side, and the data was not intercepted,
				 * or the server side has no outbound adapter registered
				 */
				if (singleUse && ((!this.isServer() && !intercepted) || (this.isServer() && this.getSender() == null))) {
					logger.debug("Closing single use socket after inbound message " + this.getConnectionId());
					this.closeConnection();
					okToRun = false;
				}
			}
		}
	}

	protected boolean handleReadException(Exception e) {
		boolean doClose = true;
		/*
		 * For client connections, we have to wait for 2 timeouts if the last
		 * send was within the current timeout.
		 */
		if (!this.isServer() && e instanceof SocketTimeoutException) {
			long now = System.currentTimeMillis();
			try {
				int soTimeout = this.socket.getSoTimeout();
				if (now - this.lastSend < soTimeout && now - this.lastRead < soTimeout * 2) {
					doClose = false;
				}
				if (!doClose && logger.isDebugEnabled()) {
					logger.debug("Skipping a socket timeout because we have a recent send " + this.getConnectionId());
				}
			}
			catch (SocketException e1) {
				logger.error("Error accessing soTimeout", e1);
				doClose = true;
			}
		}
		if (doClose) {
			boolean noReadErrorOnClose = this.noReadErrorOnClose;
			this.closeConnection();
			if (!(e instanceof SoftEndOfStreamException)) {
				if (e instanceof SocketTimeoutException && this.isSingleUse()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Closed single use socket after timeout:" + this.getConnectionId());
					}
				}
				else {
					if (noReadErrorOnClose) {
						if (logger.isTraceEnabled()) {
							logger.trace("Read exception " +
									 this.getConnectionId(), e);
						}
						else if (logger.isDebugEnabled()) {
							logger.debug("Read exception " +
									 this.getConnectionId() + " " +
									 e.getClass().getSimpleName() +
								     ":" + (e.getCause() != null ? e.getCause() + ":" : "") + e.getMessage());
						}
					}
					else if (logger.isTraceEnabled()) {
						logger.error("Read exception " +
								 this.getConnectionId(), e);
					}
					else {
						logger.error("Read exception " +
									 this.getConnectionId() + " " +
									 e.getClass().getSimpleName() +
								     ":" + (e.getCause() != null ? e.getCause() + ":" : "") + e.getMessage());
					}
					this.sendExceptionToListener(e);
				}
			}
		}
		return doClose;
	}

}

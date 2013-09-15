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
package org.springframework.integration.syslog.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.syslog.config.SyslogReceivingChannelAdapterFactoryBean;
import org.springframework.integration.test.util.SocketUtils;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SyslogReceivingChannelAdapterTests {

	@Test
	public void testUdp() throws Exception {
		SyslogReceivingChannelAdapterFactoryBean factory = new SyslogReceivingChannelAdapterFactoryBean(
				SyslogReceivingChannelAdapterFactoryBean.Protocol.udp);
		int port = SocketUtils.findAvailableUdpSocket(1514);
		factory.setPort(port);
		PollableChannel outputChannel = new QueueChannel();
		factory.setOutputChannel(outputChannel);
		factory.afterPropertiesSet();
		factory.start();
		UdpSyslogReceivingChannelAdapter adapter = (UdpSyslogReceivingChannelAdapter) factory.getObject();
		Thread.sleep(1000);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes("UTF-8");
		DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("localhost", port));
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);
		socket.close();
		Message<?> message = outputChannel.receive(10000);
		assertNotNull(message);
		assertEquals("WEBERN", message.getHeaders().get("syslog_HOST"));
		adapter.stop();
	}

	@Test
	public void testTcp() throws Exception {
		SyslogReceivingChannelAdapterFactoryBean factory = new SyslogReceivingChannelAdapterFactoryBean(
				SyslogReceivingChannelAdapterFactoryBean.Protocol.tcp);
		int port = SocketUtils.findAvailableServerSocket(1514);
		factory.setPort(port);
		PollableChannel outputChannel = new QueueChannel();
		factory.setOutputChannel(outputChannel);
		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
		final CountDownLatch latch = new CountDownLatch(2);
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				latch.countDown();
				return null;
			}
		}).when(publisher).publishEvent(any(ApplicationEvent.class));
		factory.setApplicationEventPublisher(publisher);
		factory.afterPropertiesSet();
		factory.start();
		TcpSyslogReceivingChannelAdapter adapter = (TcpSyslogReceivingChannelAdapter) factory.getObject();
		Thread.sleep(1000);
		byte[] buf = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE\n".getBytes("UTF-8");
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write(buf);
		socket.close();
		Message<?> message = outputChannel.receive(10000);
		assertNotNull(message);
		assertEquals("WEBERN", message.getHeaders().get("syslog_HOST"));
		adapter.stop();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

}

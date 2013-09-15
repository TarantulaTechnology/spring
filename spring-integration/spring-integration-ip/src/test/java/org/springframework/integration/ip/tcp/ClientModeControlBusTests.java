/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientModeControlBusTests {

	@Autowired
	ControlBus controlBus;

	@Autowired
	TcpReceivingChannelAdapter tcpIn;

	@Autowired
	TaskScheduler taskScheduler; // default

	@Test
	public void test() throws Exception {
		assertTrue(controlBus.boolResult("@tcpIn.isClientMode()"));
		int n = 0;
		while (!controlBus.boolResult("@tcpIn.isClientModeConnected()")) {
			Thread.sleep(100);
			n += 100;
			if (n > 10000) {
				fail("Connection never established");
			}
		}
		assertTrue(controlBus.boolResult("@tcpIn.isRunning()"));
		assertSame(taskScheduler, TestUtils.getPropertyValue(tcpIn, "taskScheduler"));
		controlBus.voidResult("@tcpIn.retryConnection()");
	}

	public static interface ControlBus {

		boolean boolResult(String command);

		void voidResult(String command);
	}
}

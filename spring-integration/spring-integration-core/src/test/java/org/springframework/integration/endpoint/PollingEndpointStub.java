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

package org.springframework.integration.endpoint;

import org.springframework.integration.Message;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Jonas Partner
 * @author Gary Russell
 */
public class PollingEndpointStub extends AbstractPollingEndpoint {

	public PollingEndpointStub() {
		this.setTrigger(new PeriodicTrigger(500));
	}

	@Override
	protected void handleMessage(Message<?> message) {
	}

	@Override
	protected Message<?> receiveMessage() {
		throw new RuntimeException("intentional test failure");
	}

}

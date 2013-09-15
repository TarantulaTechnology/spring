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

package org.springframework.integration.aggregator.integration;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DefaultMessageAggregatorIntegrationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(timeout = 10000)
	public void testAggregation() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			input.send(new GenericMessage<Integer>(i, headers));
		}
		Object payload = output.receive().getPayload();
		assertThat(payload, is(instanceOf(List.class)));
		assertTrue(payload + " doesn't contain all of {0,1,2,3,4}",
				((List) payload).containsAll(Arrays.asList(0, 1, 2, 3, 4)));
	}


	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correllationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(MessageHeaders.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(MessageHeaders.SEQUENCE_SIZE, sequenceSize);
		headers.put(MessageHeaders.CORRELATION_ID, correllationId);
		return headers;
	}

}

/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.ws;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.xml.transform.StringSource;

/**
 * 
 * @author Iwein Fuld
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MarshallingWebServiceIntegrationTests {

	private static final String input = "<hello/>";

	@Autowired MarshallingWebServiceInboundGateway gateway;
	
	@Mock
	private MessageContext context;

	@Mock
	private WebServiceMessage response;

	@Mock
	private WebServiceMessage request;

	private Source stringSource = new StreamSource(new StringReader(input));
	
	private StringWriter output = new StringWriter();

	private Result stringResult = new StreamResult(output);
	
	@Before public void setupMocks(){
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void configOk() throws Exception {
		// just flag invalid config
	}
	
	@Test
	public void sendString() throws Exception {
		when(context.getResponse()).thenReturn(response);
		when(context.getRequest()).thenReturn(request);
		when(request.getPayloadSource()).thenReturn(stringSource );
		when(response.getPayloadResult()).thenReturn(stringResult);
		gateway.invoke(context);
		assertTrue(output.toString().endsWith(input));
	}

	public static class StubMarshaller implements Marshaller, Unmarshaller {

		public void marshal(Object graph, Result result)
				throws XmlMappingException, IOException {
			Transformer transformer;
			try {
				transformer = TransformerFactory.newInstance().newTransformer();
				StringSource stringSource = new StringSource(graph.toString());
				transformer.transform(stringSource, result);
			}
			catch (Exception e) {
				throw new MarshallingFailureException("Stub failed to marshal",
						e);
			}
		}


		@SuppressWarnings("rawtypes")
		public boolean supports(Class clazz) {
			return true;
		}

		public Object unmarshal(Source source) throws XmlMappingException,
				IOException {
			//this is a hack, but we're not here to test marshalling
			return input;
		}
	}

	public static class StubEndpoint {
		@ServiceActivator(inputChannel="requests")
		public Object handle(Object o) {
			return o;
		}
	}
}

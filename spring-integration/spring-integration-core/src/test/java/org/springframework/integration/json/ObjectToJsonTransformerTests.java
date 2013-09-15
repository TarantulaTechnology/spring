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

package org.springframework.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JacksonJsonObjectMapper;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class ObjectToJsonTransformerTests {

	@Test
	public void simpleStringPayload() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		String result = (String) transformer.transform(new GenericMessage<String>("foo")).getPayload();
		assertEquals("\"foo\"", result);
	}

	@Test
	public void withDefaultContentType() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		Message<?> result = transformer.transform(new GenericMessage<String>("foo"));
		assertEquals(ObjectToJsonTransformer.JSON_CONTENT_TYPE, result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentType() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "text/xml").build();
		Message<?> result = transformer.transform(message);
		assertEquals("text/xml", result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentTypeWithOverride() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		transformer.setContentType(ObjectToJsonTransformer.JSON_CONTENT_TYPE);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "text/xml").build();
		Message<?> result = transformer.transform(message);
		assertEquals(ObjectToJsonTransformer.JSON_CONTENT_TYPE, result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentTypeAsEmptyString() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		transformer.setContentType("");
		Message<?> message = MessageBuilder.withPayload("foo").build();
		Message<?> result = transformer.transform(message);
		assertFalse(result.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentTypeAsEmptyStringDoesNotOverride() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		transformer.setContentType("");
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "text/xml").build();
		Message<?> result = transformer.transform(message);
		assertEquals("text/xml", result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test(expected=IllegalArgumentException.class)
	public void withProvidedContentTypeAsNull() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		transformer.setContentType(null);
	}

	@Test
	public void simpleIntegerPayload() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		String result = (String) transformer.transform(new GenericMessage<Integer>(123)).getPayload();
		assertEquals("123", result);
	}

	@Test
	public void objectPayload() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		TestAddress address = new TestAddress(123, "Main Street");
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(address);
		String result = (String) transformer.transform(new GenericMessage<TestPerson>(person)).getPayload();
		assertTrue(result.contains("\"firstName\":\"John\""));
		assertTrue(result.contains("\"lastName\":\"Doe\""));
		assertTrue(result.contains("\"age\":42"));
		Pattern addressPattern = Pattern.compile("(\"address\":\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(result);
		assertTrue(matcher.find());
		String addressResult = matcher.group(1);
		assertTrue(addressResult.contains("\"number\":123"));
		assertTrue(addressResult.contains("\"street\":\"Main Street\""));
	}

	@Test
	public void objectPayloadWithCustomObjectMapper() throws Exception {
		ObjectMapper customMapper = new ObjectMapper();
		customMapper.configure(Feature.QUOTE_FIELD_NAMES, Boolean.FALSE);
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer(new JacksonJsonObjectMapper(customMapper));
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));
		String result = (String) transformer.transform(new GenericMessage<TestPerson>(person)).getPayload();
		assertTrue(result.contains("firstName:\"John\""));
		assertTrue(result.contains("lastName:\"Doe\""));
		assertTrue(result.contains("age:42"));
		Pattern addressPattern = Pattern.compile("(address:\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(result);
		assertTrue(matcher.find());
		String addressResult = matcher.group(1);
		assertTrue(addressResult.contains("number:123"));
		assertTrue(addressResult.contains("street:\"Main Street\""));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IllegalArgumentException.class)
	public void testInt2831IllegalArgument() throws Exception {
		new ObjectToJsonTransformer(new Object());
	}

	@SuppressWarnings("unused")
	private static class TestPerson {

		private final String firstName;

		private final String lastName;

		private final int age;

		private TestAddress address;


		public TestPerson(String firstName, String lastName, int age) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.age = age;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public int getAge() {
			return age;
		}

		public TestAddress getAddress() {
			return address;
		}

		public void setAddress(TestAddress address) {
			this.address = address;
		}
	}


	@SuppressWarnings("unused")
	private static class TestAddress {

		private final int number;

		private final String street;


		public TestAddress(int number, String street) {
			this.number = number;
			this.street = street;
		}

		public int getNumber() {
			return number;
		}

		public String getStreet() {
			return street;
		}
	}

}

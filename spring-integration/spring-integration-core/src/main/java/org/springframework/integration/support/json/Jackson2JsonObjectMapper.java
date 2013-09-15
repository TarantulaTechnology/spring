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

package org.springframework.integration.support.json;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.util.Assert;

/**
 * Jackson 2 JSON-processor (@link https://github.com/FasterXML) {@linkplain JsonObjectMapper} implementation.
 * Delegates <code>toJson</code> and <code>fromJson</code>
 * to the {@linkplain com.fasterxml.jackson.databind.ObjectMapper}
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class Jackson2JsonObjectMapper implements JsonObjectMapper<JsonParser> {

	private final ObjectMapper objectMapper;

	public Jackson2JsonObjectMapper() {
		this.objectMapper = new ObjectMapper();
	}

	public Jackson2JsonObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public String toJson(Object value) throws Exception {
		return this.objectMapper.writeValueAsString(value);
	}

	@Override
	public void toJson(Object value, Writer writer) throws Exception {
		this.objectMapper.writeValue(writer, value);
	}

	public <T> T fromJson(String json, Class<T> valueType) throws Exception {
		return this.objectMapper.readValue(json, valueType);
	}

	@Override
	public <T> T fromJson(Reader json, Class<T> valueType) throws Exception {
		return this.objectMapper.readValue(json, valueType);
	}

	@Override
	public <T> T fromJson(JsonParser parser, Type valueType) throws Exception {
		return this.objectMapper.readValue(parser, this.objectMapper.constructType(valueType));
	}

}

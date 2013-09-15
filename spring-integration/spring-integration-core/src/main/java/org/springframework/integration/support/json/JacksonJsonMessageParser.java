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

import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * {@link JsonInboundMessageMapper.JsonMessageParser} implementation that parses JSON messages
 * and builds a {@link Message} with the specified payload type from provided {@link JsonInboundMessageMapper}.
 * Uses Jackson JSON-processor (@link http://jackson.codehaus.org).
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class JacksonJsonMessageParser extends AbstractJacksonJsonMessageParser<JsonParser> {

	public JacksonJsonMessageParser() {
		super(new JacksonJsonObjectMapper());
	}

	@Override
	protected JsonParser createJsonParser(String jsonMessage) throws Exception {
		return new JsonFactory().createJsonParser(jsonMessage);
	}

	@Override
	protected Message<?> parseWithHeaders(JsonParser parser, String jsonMessage) throws Exception {
		String error = AbstractJsonInboundMessageMapper.MESSAGE_FORMAT_ERROR + jsonMessage;
		Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
		Map<String, Object> headers = null;
		Object payload = null;
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			Assert.isTrue(parser.getCurrentToken() == JsonToken.FIELD_NAME, error);
			boolean isHeadersToken = "headers".equals(parser.getCurrentName());
			boolean isPayloadToken = "payload".equals(parser.getCurrentName());
			Assert.isTrue(isHeadersToken || isPayloadToken, error);
			if (isHeadersToken) {
				Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
				headers = readHeaders(parser, jsonMessage);
			}
			else if (isPayloadToken) {
				parser.nextToken();
				payload = this.readPayload(parser, jsonMessage);
			}
		}
		Assert.notNull(headers, error);
		return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
	}

	private Map<String, Object> readHeaders(JsonParser parser, String jsonMessage) throws Exception {
		Map<String, Object> headers = new LinkedHashMap<String, Object>();
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String headerName = parser.getCurrentName();
			parser.nextToken();
			Object headerValue = this.readHeader(parser, headerName, jsonMessage);
			headers.put(headerName, headerValue);
		}
		return headers;
	}

}

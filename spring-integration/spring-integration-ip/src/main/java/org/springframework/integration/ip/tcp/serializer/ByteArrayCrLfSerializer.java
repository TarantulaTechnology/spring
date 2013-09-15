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

package org.springframework.integration.ip.tcp.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reads data in an InputStream to a byte[]; data must be terminated by \r\n
 * (not included in resulting byte[]).
 * Writes a byte[] to an OutputStream and adds \r\n.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class ByteArrayCrLfSerializer extends AbstractByteArraySerializer {

	private static final byte[] CRLF = "\r\n".getBytes();

	/**
	 * Reads the data in the inputstream to a byte[]. Data must be terminated
	 * by CRLF (\r\n). Throws a {@link SoftEndOfStreamException} if the stream
	 * is closed immediately after the \r\n (i.e. no data is in the process of
	 * being read).
	 */
	public byte[] deserialize(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[this.maxMessageSize];
		int n = this.fillToCrLf(inputStream, buffer);
		byte[] assembledData = this.copyToSizedArray(buffer, n);
		return assembledData;
	}

	public int fillToCrLf(InputStream inputStream, byte[] buffer)
			throws IOException, SoftEndOfStreamException {
		int n = 0;
		int bite;
		if (logger.isDebugEnabled()) {
			logger.debug("Available to read:" + inputStream.available());
		}
		while (true) {
			bite = inputStream.read();
//			logger.debug("Read:" + (char) bite);
			if (bite < 0 && n == 0) {
				throw new SoftEndOfStreamException("Stream closed between payloads");
			}
			checkClosure(bite);
			if (n > 0 && bite == '\n' && buffer[n-1] == '\r') {
				break;
			}
			buffer[n++] = (byte) bite;
			if (n >= this.maxMessageSize) {
				throw new IOException("CRLF not found before max message length: "
						+ this.maxMessageSize);
			}
		};
		return n-1; // trim \r
	}

	/**
	 * Writes the byte[] to the stream and appends \r\n.
	 */
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		outputStream.write(bytes);
		outputStream.write(CRLF);
		outputStream.flush();
	}

}

/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.xml.source;

import java.io.File;
import java.io.FileReader;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.integration.MessagingException;
import org.springframework.util.FileCopyUtils;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

/**
 * {@link SourceFactory} implementation which supports creation of a {@link StringSource}
 * from a {@link Document}, {@link File} or {@link String} payload
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class StringSourceFactory implements SourceFactory {

	private final TransformerFactory transformerFactory;


	public StringSourceFactory() {
		this(TransformerFactory.newInstance());
	}

	public StringSourceFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}


	public Source createSource(Object payload) {
		Source source = null;
		if (payload instanceof String) {
			source = new StringSource((String) payload);
		}
		else if (payload instanceof Document) {
			source = createStringSourceForDocument((Document) payload);
		}
		else if (payload instanceof File) {
			source = createStringSourceForFile((File) payload);
		}
		if (source == null) {
			throw new MessagingException("Failed to create Source for payload type ["
					+ payload.getClass().getName() + "]");
		}
		return source;
	}

	private StringSource createStringSourceForDocument(Document document) {
		try {
			StringResult result = new StringResult();
			Transformer transformer = getTransformer();
			transformer.transform(new DOMSource(document), result);
			return new StringSource(result.toString());
		}
		catch (Exception e) {
			throw new MessagingException("failed to create StringSource from document", e);
		}
	}

	private StringSource createStringSourceForFile(File file) {
		try {
			String content = FileCopyUtils.copyToString(new FileReader(file));
			return new StringSource(content);
		}
		catch (Exception e) {
			throw new MessagingException("failed to create StringSource from file", e);
		}
	}

	private synchronized Transformer getTransformer() {
		try {
			return transformerFactory.newTransformer();
		}
		catch (Exception e) {
			throw new MessagingException("Exception creating transformer", e);
		}
	}

}

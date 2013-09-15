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

package org.springframework.integration.xml.transformer;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

/**
 * Thread safe XSLT transformer implementation which returns a transformed
 * {@link Source}, {@link Document}, or {@link String}. If
 * alwaysUseSourceResultFactories is false (default) the following logic occurs
 * <p>
 * {@link String} payload in results in {@link String} payload out
 * <p>
 * {@link Document} payload in results in {@link Document} payload out
 * <p>
 * {@link Source} payload in results in {@link Result} payload out, type will be
 * determined by the {@link ResultFactory}, {@link DomResultFactory} by default.
 * If an instance of {@link ResultTransformer} is registered this will be used
 * to convert the result.
 * <p>
 * If alwaysUseSourceResultFactories is true then the ResultFactory and
 * {@link SourceFactory} will be used to create the {@link Source} from the
 * payload and the {@link Result} to pass into the transformer. An instance of
 * {@link ResultTransformer} can also be provided to convert the Result prior to
 * returning.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class XsltPayloadTransformer extends AbstractTransformer {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Templates templates;

	private volatile StandardEvaluationContext evaluationContext;

	private Map<String, Expression> xslParameterMappings;

	private final ResultTransformer resultTransformer;

	private volatile SourceFactory sourceFactory = new DomSourceFactory();

	private volatile ResultFactory resultFactory = new DomResultFactory();

	private volatile boolean resultFactoryExplicitlySet;

	private volatile boolean alwaysUseSourceFactory = false;

	private volatile boolean alwaysUseResultFactory = false;

	private volatile String[] xsltParamHeaders;


	public XsltPayloadTransformer(Templates templates) throws ParserConfigurationException {
		this(templates, null);
	}

	public XsltPayloadTransformer(Resource xslResource) throws Exception {
		this(TransformerFactory.newInstance().newTemplates(
				createStreamSourceOnResource(xslResource)), null);
	}

	public XsltPayloadTransformer(Resource xslResource, ResultTransformer resultTransformer) throws Exception {
		this(TransformerFactory.newInstance().newTemplates(
				createStreamSourceOnResource(xslResource)), resultTransformer);
	}

	public XsltPayloadTransformer(Templates templates, ResultTransformer resultTransformer) throws ParserConfigurationException {
		this.templates = templates;
		this.resultTransformer = resultTransformer;
	}


	/**
	 * Sets the SourceFactory.
	 */
	public void setSourceFactory(SourceFactory sourceFactory) {
		Assert.notNull(sourceFactory, "SourceFactory must not be null");
		this.sourceFactory = sourceFactory;
	}

	/**
	 * Sets the ResultFactory
	 */
	public void setResultFactory(ResultFactory resultFactory) {
		Assert.notNull(sourceFactory, "ResultFactory must not be null");
		this.resultFactory = resultFactory;
		this.resultFactoryExplicitlySet = true;
	}

	/**
	 * Specify whether to always use source factory even for directly supported payload types.
	 */
	public void setAlwaysUseSourceFactory(boolean alwaysUseSourceFactory) {
		this.alwaysUseSourceFactory = alwaysUseSourceFactory;
	}

	/**
	 * Specify whether to always use result factory even for directly supported payload types
	 */
	public void setAlwaysUseResultFactory(boolean alwaysUseResultFactory) {
		this.alwaysUseResultFactory = alwaysUseResultFactory;
	}

	public void setXslParameterMappings(Map<String, Expression> xslParameterMappings) {
		this.xslParameterMappings = xslParameterMappings;
	}

	public void setXsltParamHeaders(String[] xsltParamHeaders) {
		this.xsltParamHeaders = xsltParamHeaders;
	}

	@Override
	public String getComponentType() {
		return "xml:xslt-transformer";
	}


	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		Transformer transformer = buildTransformer(message);
		Object payload;
		if (this.alwaysUseSourceFactory) {
			payload = sourceFactory.createSource(message.getPayload());
		}
		else {
			payload = message.getPayload();
		}
		Object transformedPayload = null;
		if (this.alwaysUseResultFactory) {
			transformedPayload = transformUsingResultFactory(payload, transformer);
		}
		else if (payload instanceof String) {
			transformedPayload = transformString((String) payload, transformer);
		}
		else if (payload instanceof Document) {
			transformedPayload = transformDocument((Document) payload, transformer);
		}
		else if (payload instanceof Source) {
			transformedPayload = transformSource((Source) payload, payload, transformer);
		}
		else {
			// fall back to trying factories
			transformedPayload = transformUsingResultFactory(payload, transformer);
		}
		return transformedPayload;
	}

	private Object transformUsingResultFactory(Object payload, Transformer transformer) throws TransformerException {
		Source source;
		if (this.alwaysUseSourceFactory) {
			source = this.sourceFactory.createSource(payload);
		}
		else if (payload instanceof String) {
			source = new StringSource((String) payload);
		}
		else if (payload instanceof Document) {
			source = new DOMSource((Document) payload);
		}
		else if (payload instanceof Source) {
			source = (Source) payload;
		}
		else {
			source = this.sourceFactory.createSource(payload);
		}
		return transformSource(source, payload, transformer);
	}

	private Object transformSource(Source source, Object payload, Transformer transformer) throws TransformerException {
		Result result;
		if (!this.resultFactoryExplicitlySet && "text".equals(transformer.getOutputProperties().getProperty("method"))) {
			result = new StringResult();
		}
		else {
			result = this.resultFactory.createResult(payload);
		}
		transformer.transform(source, result);
		if (this.resultTransformer != null) {
			return this.resultTransformer.transformResult(result);
		}
		return result;
	}

	private String transformString(String stringPayload, Transformer transformer) throws TransformerException {
		StringResult result = new StringResult();
		Source source;
		if (this.alwaysUseSourceFactory) {
			source = this.sourceFactory.createSource(stringPayload);
		}
		else {
			source = new StringSource(stringPayload);
		}
		transformer.transform(source, result);
		return result.toString();
	}

	private Document transformDocument(Document documentPayload, Transformer transformer) throws TransformerException {
		Source source;
		if (this.alwaysUseSourceFactory) {
			source = this.sourceFactory.createSource(documentPayload);
		}
		else {
			source = new DOMSource(documentPayload);
		}
		Result result = this.resultFactory.createResult(documentPayload);
		if (!DOMResult.class.isAssignableFrom(result.getClass())) {
			throw new MessagingException(
					"Document to Document conversion requires a DOMResult-producing ResultFactory implementation.");
		}
		DOMResult domResult = (DOMResult) result;
		transformer.transform(source, domResult);
		return (Document) domResult.getNode();
	}

	private Transformer buildTransformer(Message<?> message) throws TransformerException {
		// process individual mappings
		Transformer transformer = this.templates.newTransformer();
		if (this.xslParameterMappings != null) {
			for (String parameterName : this.xslParameterMappings.keySet()) {
				Expression expression = this.xslParameterMappings.get(parameterName);
				try {
					Object value = expression.getValue(this.evaluationContext, message);
					transformer.setParameter(parameterName, value);
				}
				catch (Exception e) {
					if (logger.isWarnEnabled()) {
						logger.warn("Evaluation of header expression '"
								+ expression.getExpressionString()
								+ "' failed. The XSLT parameter '"
								+ parameterName + "' will be skipped.");
					}
				}
			}
		}
		// process xslt-parameter-headers
		MessageHeaders headers = message.getHeaders();
		if (!ObjectUtils.isEmpty(this.xsltParamHeaders)) {
			for (String headerName : headers.keySet()) {
				if (PatternMatchUtils.simpleMatch(this.xsltParamHeaders, headerName)) {
					transformer.setParameter(headerName, headers.get(headerName));
				}
			}
		}
		return transformer;
	}


	/**
	 * Compensate for the fact that a Resource <i>may</i> not be a File or even
	 * addressable through a URI. If it is, we want the created StreamSource to
	 * read other resources relative to the provided one. If it isn't, it loads
	 * from the default path.
	 */
	private static StreamSource createStreamSourceOnResource(Resource xslResource) throws IOException {
		try {
			String systemId = xslResource.getURI().toString();
			return new StreamSource(xslResource.getInputStream(), systemId);
		}
		catch (IOException e) {
			return new StreamSource(xslResource.getInputStream());
		}
	}

}

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

package org.springframework.integration.jdbc.config;

import java.sql.Types;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.1
 */
public final class StoredProcParserUtils {

	private static final Log LOGGER = LogFactory.getLog(StoredProcParserUtils.class);

	/** Prevent instantiation. */
	private StoredProcParserUtils() {
		throw new AssertionError();
	}

	/**
	 * @param storedProcComponent
	 * @param parserContext
	 */
	public static ManagedList<BeanDefinition> getSqlParameterDefinitionBeanDefinitions(
			Element storedProcComponent, ParserContext parserContext) {
		List<Element> sqlParameterDefinitionChildElements = DomUtils.getChildElementsByTagName(storedProcComponent, "sql-parameter-definition");
		ManagedList<BeanDefinition> sqlParameterList = new ManagedList<BeanDefinition>();

		for (Element childElement : sqlParameterDefinitionChildElements) {

			String name        = childElement.getAttribute("name");
			String sqlType     = childElement.getAttribute("type");
			String direction   = childElement.getAttribute("direction");
			String scale       = childElement.getAttribute("scale");
			String typeName    = childElement.getAttribute("type-name");
			String returnType  = childElement.getAttribute("return-type");

			if (StringUtils.hasText(typeName) && StringUtils.hasText(scale)) {
				parserContext.getReaderContext().error("'type-name' and 'scale' attributes are mutually exclusive " +
						"for 'sql-parameter-definition' element.", storedProcComponent);
			}

			if (StringUtils.hasText(returnType) && StringUtils.hasText(scale)) {
				parserContext.getReaderContext().error("'returnType' and 'scale' attributes are mutually exclusive " +
						"for 'sql-parameter-definition' element.", storedProcComponent);
			}

			final BeanDefinitionBuilder parameterBuilder;

			if ("OUT".equalsIgnoreCase(direction)) {
				parameterBuilder = BeanDefinitionBuilder.genericBeanDefinition(SqlOutParameter.class);
			}
			else if ("INOUT".equalsIgnoreCase(direction)) {
				parameterBuilder = BeanDefinitionBuilder.genericBeanDefinition(SqlInOutParameter.class);
			}
			else {
				parameterBuilder = BeanDefinitionBuilder.genericBeanDefinition(SqlParameter.class);
				if (StringUtils.hasText(returnType)) {
					parserContext.getReaderContext().error("'return-type' attribute can't be provided " +
							"for IN 'sql-parameter-definition' element.", storedProcComponent);
				}
			}

			if (StringUtils.hasText(name)) {
				parameterBuilder.addConstructorArgValue(name);
			}
			else {
					parserContext.getReaderContext().error(
							"The 'name' attribute must be set for the Sql parameter element.", storedProcComponent);
			}

			if (StringUtils.hasText(sqlType)) {

				JdbcTypesEnum jdbcTypeEnum = JdbcTypesEnum.convertToJdbcTypesEnum(sqlType);

				if (jdbcTypeEnum != null) {
					parameterBuilder.addConstructorArgValue(jdbcTypeEnum.getCode());
				}
				else {
					parameterBuilder.addConstructorArgValue(sqlType);
				}

			}
			else {
				parameterBuilder.addConstructorArgValue(Types.VARCHAR);
			}

			if (StringUtils.hasText(typeName)) {
				parameterBuilder.addConstructorArgValue(typeName);
			}
			else if (StringUtils.hasText(scale)) {
				parameterBuilder.addConstructorArgValue(new TypedStringValue(scale, Integer.class));
			}
			else {
				parameterBuilder.addConstructorArgValue(null);
			}

			if (StringUtils.hasText(returnType)) {
				parameterBuilder.addConstructorArgReference(returnType);
			}

			sqlParameterList.add(parameterBuilder.getBeanDefinition());
		}
		return sqlParameterList;
	}

	/**
	 * @param storedProcComponent
	 * @param parserContext
	 */
	public static ManagedList<BeanDefinition> getProcedureParameterBeanDefinitions(
			Element storedProcComponent, ParserContext parserContext) {

		ManagedList<BeanDefinition> procedureParameterList = new ManagedList<BeanDefinition>();

		List<Element> parameterChildElements = DomUtils
				.getChildElementsByTagName(storedProcComponent, "parameter");

		for (Element childElement : parameterChildElements) {

			BeanDefinitionBuilder parameterBuilder = BeanDefinitionBuilder.genericBeanDefinition(ProcedureParameter.class);

			String name = childElement.getAttribute("name");
			String expression = childElement.getAttribute("expression");
			String value = childElement.getAttribute("value");
			String type = childElement.getAttribute("type");

			if (StringUtils.hasText(name)) {
				parameterBuilder.addPropertyValue("name", name);
			}

			if (StringUtils.hasText(expression)) {
				parameterBuilder.addPropertyValue("expression", expression);
			}

			if (StringUtils.hasText(value)) {

				if (!StringUtils.hasText(type)) {

					if (LOGGER.isInfoEnabled()) {
						LOGGER.info(String
								.format("Type attribute not set for Store "
									+ "Procedure parameter '%s'. Defaulting to "
									+ "'java.lang.String'.", value));
					}

					parameterBuilder.addPropertyValue("value",
							new TypedStringValue(value, String.class));

				}
				else {
					parameterBuilder.addPropertyValue("value",
							new TypedStringValue(value, type));
				}

			}

			procedureParameterList.add(parameterBuilder.getBeanDefinition());
		}

		return procedureParameterList;

	}

	/**
	 * @param storedProcComponent
	 * @param parserContext
	 */
	public static ManagedMap<String, BeanDefinition> getReturningResultsetBeanDefinitions(
			Element storedProcComponent, ParserContext parserContext) {

		List<Element> returningResultsetChildElements = DomUtils.getChildElementsByTagName(storedProcComponent, "returning-resultset");

		ManagedMap<String, BeanDefinition> returningResultsetMap = new ManagedMap<String, BeanDefinition>();

		for (Element childElement : returningResultsetChildElements) {

			String name       = childElement.getAttribute("name");
			String rowMapperAsString = childElement.getAttribute("row-mapper");

			BeanDefinitionBuilder rowMapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(rowMapperAsString);

			returningResultsetMap.put(name, rowMapperBuilder.getBeanDefinition());
		}

		return returningResultsetMap;

	}

	/**
	 * Create a new {@link BeanDefinitionBuilder} for the class {@link StoredProcExecutor}.
	 * Initialize the wrapped {@link StoredProcExecutor} with common properties.
	 *
	 * @param element Must not be Null
	 * @param parserContext Must not be Null
	 * @return The {@link BeanDefinitionBuilder} for the {@link StoredProcExecutor}
	 */
	public static BeanDefinitionBuilder getStoredProcExecutorBuilder(final Element element,
														final ParserContext parserContext) {

		Assert.notNull(element,       "The provided element must not be Null.");
		Assert.notNull(parserContext, "The provided parserContext must not be Null.");

		final String dataSourceRef = element.getAttribute("data-source");

		final BeanDefinitionBuilder storedProcExecutorBuilder = BeanDefinitionBuilder.genericBeanDefinition(StoredProcExecutor.class);
		storedProcExecutorBuilder.addConstructorArgReference(dataSourceRef);

		final String storedProcedureName = element.getAttribute("stored-procedure-name");
		final String storedProcedureNameExpression = element.getAttribute("stored-procedure-name-expression");
		boolean hasStoredProcedureName = StringUtils.hasText(storedProcedureName);
		boolean hasStoredProcedureNameExpression = StringUtils.hasText(storedProcedureNameExpression);

		if (!(hasStoredProcedureName ^ hasStoredProcedureNameExpression)) {
			parserContext.getReaderContext()
					.error("Exactly one of 'stored-procedure-name' or 'stored-procedure-name-expression' is required",
							element);
		}

		BeanDefinitionBuilder expressionBuilder;
		if (hasStoredProcedureNameExpression) {
			expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(storedProcedureNameExpression);
		}
		else {
			expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(LiteralExpression.class);
			expressionBuilder.addConstructorArgValue(storedProcedureName);
		}
		storedProcExecutorBuilder.addPropertyValue("storedProcedureNameExpression", expressionBuilder.getBeanDefinition());

		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "ignore-column-meta-data");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "jdbc-call-operations-cache-size");

		final ManagedList<BeanDefinition> procedureParameterList       = StoredProcParserUtils.getProcedureParameterBeanDefinitions(element, parserContext);
		final ManagedList<BeanDefinition> sqlParameterDefinitionList   = StoredProcParserUtils.getSqlParameterDefinitionBeanDefinitions(element, parserContext);

		if (!procedureParameterList.isEmpty()) {
			storedProcExecutorBuilder.addPropertyValue("procedureParameters", procedureParameterList);
		}
		if (!sqlParameterDefinitionList.isEmpty()) {
			storedProcExecutorBuilder.addPropertyValue("sqlParameters", sqlParameterDefinitionList);
		}

		return storedProcExecutorBuilder;

	}

}

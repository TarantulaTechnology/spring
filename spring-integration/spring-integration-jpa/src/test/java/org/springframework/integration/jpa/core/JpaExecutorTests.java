/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jpa.core;

import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JpaExecutorTests {

	@Autowired
	protected EntityManager entityManager;

	/**
	 * In this test, the {@link JpaExecutor}'s poll method will be called without
	 * specifying a 'query', 'namedQuery' or 'entityClass' property. This should
	 * result in an {@link IllegalArgumentException}.
	 *
	 * @throws Exception
	 */
	@Test
	public void testExecutePollWithNoEntityClassSpecified() throws Exception {

		final JpaExecutor jpaExecutor = new JpaExecutor(mock(EntityManager.class));

		try {
			jpaExecutor.poll();
		} catch (IllegalStateException e) {
			Assert.assertEquals("Exception Message does not match.",
					"For the polling operation, one of "
					+ "the following properties must be specified: "
					+ "query, namedQuery or entityClass.", e.getMessage());
			return;
		}

		Assert.fail("Was expecting an IllegalStateException to be thrown.");

	}

	/**
	 */
	@Test
	public void testInstatiateJpaExecutorWithNullJpaOperations() {

		JpaOperations jpaOperations = null;

		try {
			new JpaExecutor(jpaOperations);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("jpaOperations must not be null.", e.getMessage());
		}

	}

	@Test
	public void testSetMultipleQueryTypes() {
		JpaExecutor executor = new JpaExecutor(mock(EntityManager.class));
		executor.setJpaQuery("select s from Student s");
		Assert.assertNotNull(TestUtils.getPropertyValue(executor, "jpaQuery", String.class));

		try {
			executor.setNamedQuery("NamedQuery");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("You can define only one of the "
				+ "properties 'jpaQuery', 'nativeQuery', 'namedQuery'", e.getMessage());
		}

		Assert.assertNull(TestUtils.getPropertyValue(executor, "namedQuery"));

		try {
			executor.setNativeQuery("select * from Student");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("You can define only one of the "
					+ "properties 'jpaQuery', 'nativeQuery', 'namedQuery'", e.getMessage());
		}
		Assert.assertNull(TestUtils.getPropertyValue(executor, "nativeQuery"));

		executor = new JpaExecutor(mock(EntityManager.class));
		executor.setNamedQuery("NamedQuery");
		Assert.assertNotNull(TestUtils.getPropertyValue(executor, "namedQuery", String.class));

		try {
			executor.setJpaQuery("select s from Student s");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("You can define only one of the "
					+ "properties 'jpaQuery', 'nativeQuery', 'namedQuery'", e.getMessage());
		}
		Assert.assertNull(TestUtils.getPropertyValue(executor, "jpaQuery"));

	}

	@Test
	@Transactional
	public void selectWithMessageAsParameterSource() {
		String query = "select s from Student s where s.firstName = :firstName";
		Message<Map<String, String>> message =
			MessageBuilder.withPayload(Collections.singletonMap("firstName", "First One")).build();
		JpaExecutor executor = getJpaExecutorForMessageAsParamSource(query);
		StudentDomain student = (StudentDomain) executor.poll(message);
		Assert.assertNotNull(student);
	}

	@Test
	@Transactional
	public void selectWithPayloadAsParameterSource() {
		String query = "select s from Student s where s.firstName = :firstName";
		Message<String> message =
			MessageBuilder.withPayload("First One").build();
		JpaExecutor executor = getJpaExecutorForPayloadAsParamSource(query);
		StudentDomain student = (StudentDomain) executor.poll(message);
		Assert.assertNotNull(student);
	}

	@Test
	@Transactional
	public void updateWithMessageAsParameterSource() {
		String query = "update Student s set s.firstName = :firstName where s.lastName = 'Last One'";
		Message<Map<String, String>> message =
			MessageBuilder.withPayload(Collections.singletonMap("firstName", "First One")).build();
		JpaExecutor executor = getJpaExecutorForMessageAsParamSource(query);
		Integer rowsAffected = (Integer) executor.executeOutboundJpaOperation(message);
		Assert.assertTrue(1 == rowsAffected);
	}

	@Test
	@Transactional
	public void updateWithPayloadAsParameterSource() {
		String query = "update Student s set s.firstName = :firstName where s.lastName = 'Last One'";
		Message<String> message =
			MessageBuilder.withPayload("First One").build();
		JpaExecutor executor = getJpaExecutorForPayloadAsParamSource(query);
		Integer rowsAffected = (Integer) executor.executeOutboundJpaOperation(message);
		Assert.assertTrue(1 == rowsAffected);
	}

	/**
	 * @param query
	 * @return
	 */
	private JpaExecutor getJpaExecutorForMessageAsParamSource(String query) {
		JpaExecutor executor = new JpaExecutor(entityManager);
		ExpressionEvaluatingParameterSourceFactory factory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		factory.setParameters(
				Collections.singletonList(new JpaParameter("firstName", null, "payload['firstName']")));
		executor.setParameterSourceFactory(factory);
		executor.setJpaQuery(query);
		executor.setExpectSingleResult(true);
		executor.setUsePayloadAsParameterSource(false);
		executor.afterPropertiesSet();
		return executor;
	}

	/**
	 * @param query
	 * @return
	 */
	private JpaExecutor getJpaExecutorForPayloadAsParamSource(String query) {
		JpaExecutor executor = new JpaExecutor(entityManager);
		ExpressionEvaluatingParameterSourceFactory factory =
				new ExpressionEvaluatingParameterSourceFactory(mock(BeanFactory.class));
		factory.setParameters(
				Collections.singletonList(new JpaParameter("firstName", null, "#this")));
		executor.setParameterSourceFactory(factory);
		executor.setJpaQuery(query);
		executor.setExpectSingleResult(true);
		executor.setUsePayloadAsParameterSource(true);
		executor.afterPropertiesSet();
		return executor;
	}

	@Test
	public void testNegativeMaxNumberOfResults() throws Exception {

		final JpaExecutor jpaExecutor = new JpaExecutor(mock(EntityManager.class));

		try {
			jpaExecutor.setMaxNumberOfResults(-10);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("maxNumberOfResults must not be negative.", e.getMessage());
			return;
		}

		Assert.fail("Was expecting an IllegalStateException to be thrown.");

	}

}
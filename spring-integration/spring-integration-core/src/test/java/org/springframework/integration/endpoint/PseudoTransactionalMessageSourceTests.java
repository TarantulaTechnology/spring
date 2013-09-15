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
package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.2
 *
 */
public class PseudoTransactionalMessageSourceTests {

	@Test
	public void testCommit() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
				new ExpressionEvaluatingTransactionSynchronizationProcessor();
		syncProcessor.setBeanFactory(mock(BeanFactory.class));
		PollableChannel queueChannel = new QueueChannel();
		syncProcessor.setBeforeCommitExpression(new SpelExpressionParser().parseExpression("#bix"));
		syncProcessor.setBeforeCommitChannel(queueChannel);
		syncProcessor.setAfterCommitChannel(queueChannel);
		syncProcessor.setAfterCommitExpression(new SpelExpressionParser().parseExpression("#baz"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<String>("foo");
				((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("baz", "qux");
				((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("bix", "qox");
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerBeforeCommit(false);
		TransactionSynchronizationUtils.triggerAfterCommit();
		Message<?> beforeCommitMessage = queueChannel.receive(1000);
		assertNotNull(beforeCommitMessage);
		assertEquals("qox", beforeCommitMessage.getPayload());
		Message<?> afterCommitMessage = queueChannel.receive(1000);
		assertNotNull(afterCommitMessage);
		assertEquals("qux", afterCommitMessage.getPayload());
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	public void testRollback() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
				new ExpressionEvaluatingTransactionSynchronizationProcessor();
		syncProcessor.setBeanFactory(mock(BeanFactory.class));
		PollableChannel queueChannel = new QueueChannel();
		syncProcessor.setAfterRollbackChannel(queueChannel);
		syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<String>("foo");
				((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("baz", "qux");
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertNotNull(rollbackMessage);
		assertEquals("qux", rollbackMessage.getPayload());
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	public void testCommitWithManager() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		transactionTemplate.execute(new TransactionCallback<Object>() {

			public Object doInTransaction(TransactionStatus status) {
				SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
				ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
						new ExpressionEvaluatingTransactionSynchronizationProcessor();
				syncProcessor.setBeanFactory(mock(BeanFactory.class));
				syncProcessor.setBeforeCommitExpression(new SpelExpressionParser().parseExpression("#bix"));
				syncProcessor.setBeforeCommitChannel(queueChannel);
				syncProcessor.setAfterCommitChannel(queueChannel);
				syncProcessor.setAfterCommitExpression(new SpelExpressionParser().parseExpression("#baz"));

				DefaultTransactionSynchronizationFactory syncFactory =
						new DefaultTransactionSynchronizationFactory(syncProcessor);

				adapter.setTransactionSynchronizationFactory(syncFactory);

				QueueChannel outputChannel = new QueueChannel();
				adapter.setOutputChannel(outputChannel);
				adapter.setSource(new MessageSource<String>() {

					public Message<String> receive() {
						GenericMessage<String> message = new GenericMessage<String>("foo");
						((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("baz", "qux");
						((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("bix", "qox");
						return message;
					}
				});

				doPoll(adapter);
				return null;
			}
		});
		Message<?> beforeCommitMessage = queueChannel.receive(1000);
		assertNotNull(beforeCommitMessage);
		assertEquals("qox", beforeCommitMessage.getPayload());
		Message<?> afterCommitMessage = queueChannel.receive(1000);
		assertNotNull(afterCommitMessage);
		assertEquals("qux", afterCommitMessage.getPayload());
	}

	@Test
	public void testRollbackWithManager() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		try {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				public Object doInTransaction(TransactionStatus status) {

					SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
					ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor = new ExpressionEvaluatingTransactionSynchronizationProcessor();
					syncProcessor.setBeanFactory(mock(BeanFactory.class));
					syncProcessor.setAfterRollbackChannel(queueChannel);
					syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

					DefaultTransactionSynchronizationFactory syncFactory = new DefaultTransactionSynchronizationFactory(
							syncProcessor);

					adapter.setTransactionSynchronizationFactory(syncFactory);

					QueueChannel outputChannel = new QueueChannel();
					adapter.setOutputChannel(outputChannel);
					adapter.setSource(new MessageSource<String>() {

						public Message<String> receive() {
							GenericMessage<String> message = new GenericMessage<String>("foo");
							((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this))
									.addAttribute("baz", "qux");
							return message;
						}
					});

					doPoll(adapter);
					throw new RuntimeException("Force rollback");
				}
			});
		}
		catch (Exception e) {
			assertEquals("Force rollback", e.getMessage());
		}
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertNotNull(rollbackMessage);
		assertEquals("qux", rollbackMessage.getPayload());
	}

	@Test
	public void testRollbackWithManagerUsingStatus() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		transactionTemplate.execute(new TransactionCallback<Object>() {

			public Object doInTransaction(TransactionStatus status) {

				SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
				ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor = new ExpressionEvaluatingTransactionSynchronizationProcessor();
				syncProcessor.setBeanFactory(mock(BeanFactory.class));
				syncProcessor.setAfterRollbackChannel(queueChannel);
				syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

				DefaultTransactionSynchronizationFactory syncFactory = new DefaultTransactionSynchronizationFactory(
						syncProcessor);

				adapter.setTransactionSynchronizationFactory(syncFactory);

				QueueChannel outputChannel = new QueueChannel();
				adapter.setOutputChannel(outputChannel);
				adapter.setSource(new MessageSource<String>() {

					public Message<String> receive() {
						GenericMessage<String> message = new GenericMessage<String>("foo");
						((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this))
								.addAttribute("baz", "qux");
						return message;
					}
				});

				doPoll(adapter);
				status.setRollbackOnly();
				return null;
			}
		});
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertNotNull(rollbackMessage);
		assertEquals("qux", rollbackMessage.getPayload());
	}

	@Test
	public void testInt2777UnboundResourceAfterTransactionComplete() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();

		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				return null;
			}
		});

		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationManager.setActualTransactionActive(false);

		// Before INT-2777 this test was failed here
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Ignore
	@Test
	public void testInt2777CustomTransactionSynchronizationFactoryWithoutDealWithIntegrationResourceHolder() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();

		final AtomicInteger txSyncCounter = new AtomicInteger();

		TransactionSynchronizationFactory syncFactory = new TransactionSynchronizationFactory() {

			public TransactionSynchronization create(Object key) {
				return new TransactionSynchronizationAdapter() {
					@Override
					public void afterCompletion(int status) {
						txSyncCounter.incrementAndGet();
					}
				};
			}
		};

		adapter.setTransactionSynchronizationFactory(syncFactory);
		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				return null;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
		assertEquals(1, txSyncCounter.get());

		/*TODO: Failed with 'java.lang.IllegalStateException: Already value
		TODO: [org.springframework.integration.transaction.IntegrationResourceHolder@46b8c8e6]
		TODO: for key [org.springframework.integration.endpoint.PseudoTransactionalMessageSourceTests$8@78a1d1f4] bound to thread [main]'*/
		//TODO: Need new JIRA issue to fix it
		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
		assertEquals(2, txSyncCounter.get());
	}

	protected void doPoll(SourcePollingChannelAdapter adapter) {
		try {
			Method method = AbstractPollingEndpoint.class.getDeclaredMethod("doPoll");
			method.setAccessible(true);
			method.invoke(adapter);
		}
		catch (Exception e) {
			fail("Failed to invoke doPoll(): " + e.toString());
		}
	}

	public class Bar {
		public String getValue() {
			return "bar";
		}
	}
}

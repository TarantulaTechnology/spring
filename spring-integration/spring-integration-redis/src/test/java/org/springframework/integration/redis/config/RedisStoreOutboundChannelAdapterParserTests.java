/*
 * Copyright 2007-2012 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.redis.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.redis.outbound.RedisStoreWritingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisStoreOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RedisTemplate<?,?> redisTemplate;

	@Test
	public void validateWithStringTemplate(){
		RedisStoreWritingMessageHandler withStringTemplate =
				TestUtils.getPropertyValue(context.getBean("withStringTemplate.adapter"), "handler", RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys", ((LiteralExpression)TestUtils.getPropertyValue(withStringTemplate, "keyExpression")).getExpressionString());
		assertEquals("PROPERTIES", ((CollectionType)TestUtils.getPropertyValue(withStringTemplate, "collectionType")).toString());
		assertTrue(TestUtils.getPropertyValue(withStringTemplate, "redisTemplate") instanceof StringRedisTemplate);
	}

	@Test
	public void validateWithStringObjectTemplate(){
		RedisStoreWritingMessageHandler withStringObjectTemplate =
				TestUtils.getPropertyValue(context.getBean("withStringObjectTemplate.adapter"), "handler", RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys", ((LiteralExpression)TestUtils.getPropertyValue(withStringObjectTemplate, "keyExpression")).getExpressionString());
		assertEquals("PROPERTIES", ((CollectionType)TestUtils.getPropertyValue(withStringObjectTemplate, "collectionType")).toString());
		assertFalse(TestUtils.getPropertyValue(withStringObjectTemplate, "redisTemplate") instanceof StringRedisTemplate);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate, "redisTemplate.keySerializer") instanceof StringRedisSerializer);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate, "redisTemplate.hashKeySerializer") instanceof StringRedisSerializer);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate, "redisTemplate.valueSerializer") instanceof JdkSerializationRedisSerializer);
		assertTrue(TestUtils.getPropertyValue(withStringObjectTemplate, "redisTemplate.hashValueSerializer") instanceof JdkSerializationRedisSerializer);
	}

	@Test
	public void validateWithExternalTemplate(){
		RedisStoreWritingMessageHandler withExternalTemplate =
				TestUtils.getPropertyValue(context.getBean("withExternalTemplate.adapter"), "handler", RedisStoreWritingMessageHandler.class);
		assertEquals("pepboys", ((LiteralExpression)TestUtils.getPropertyValue(withExternalTemplate, "keyExpression")).getExpressionString());
		assertEquals("PROPERTIES", ((CollectionType)TestUtils.getPropertyValue(withExternalTemplate, "collectionType")).toString());
		assertSame(redisTemplate, TestUtils.getPropertyValue(withExternalTemplate, "redisTemplate"));
	}
}

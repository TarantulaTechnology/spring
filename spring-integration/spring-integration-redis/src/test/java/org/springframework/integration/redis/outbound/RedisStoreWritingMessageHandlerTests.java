/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.redis.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.data.redis.support.collections.DefaultRedisZSet;
import org.springframework.data.redis.support.collections.RedisCollectionFactoryBean.CollectionType;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Mark Fisher
 */
public class RedisStoreWritingMessageHandlerTests extends RedisAvailableTests{

	@Test
	@RedisAvailable
	public void testListWithListPayloadParsedAndProvidedKey() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<String>(key, this.initTemplate(jcf, new StringRedisTemplate()));

		assertEquals(0, redisList.size());

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<List<String>>(list);
		handler.handleMessage(message);

		assertEquals(3, redisList.size());
		assertEquals("Manny", redisList.get(0));
		assertEquals("Moe", redisList.get(1));
		assertEquals("Jack", redisList.get(2));
	}

	@Test
	@RedisAvailable
	public void testListWithListPayloadParsedAndProvidedKeyAsHeader() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<String>(key, this.initTemplate(jcf, new StringRedisTemplate()));

		assertEquals(0, redisList.size());

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);

		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertEquals(3, redisList.size());
		assertEquals("Manny", redisList.get(0));
		assertEquals("Moe", redisList.get(1));
		assertEquals("Jack", redisList.get(2));
	}

	@RedisAvailable
	@Test(expected=MessageHandlingException.class)
	public void testListWithListPayloadParsedAndNoKey() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<String> redisList =
				new DefaultRedisList<String>(key, this.initTemplate(jcf, new RedisTemplate<String, String>()));

		assertEquals(0, redisList.size());

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).build();
		handler.handleMessage(message);
	}

	@Test
	@RedisAvailable
	public void testListWithListPayloadAsSingleEntry() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisList<List<String>> redisList =
				new DefaultRedisList<List<String>>(key, this.initTemplate(jcf, new RedisTemplate<String, List<String>>()));

		assertEquals(0, redisList.size());

		RedisTemplate<String, List<String>> template = this.initTemplate(jcf, new RedisTemplate<String, List<String>>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);
		handler.setKey(key);
		handler.setExtractPayloadElements(false);
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<List<String>>(list);
		handler.handleMessage(message);

		assertEquals(1, redisList.size());
		List<String> resultList = redisList.get(0);
		assertEquals("Manny", resultList.get(0));
		assertEquals("Moe", resultList.get(1));
		assertEquals("Jack", resultList.get(2));
	}

	@Test
	@RedisAvailable
	public void testZsetWithListPayloadParsedAndProvidedKeyDefault() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>(key, this.initTemplate(jcf, new StringRedisTemplate()));

		assertEquals(0, redisZset.size());

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = new GenericMessage<List<String>>(list);
		handler.handleMessage(message);

		assertEquals(3, redisZset.size());
		Set<TypedTuple<String>> pepboys = redisZset.rangeByScoreWithScores(1, 1);
		for (TypedTuple<String> pepboy : pepboys) {
			assertTrue(pepboy.getScore() == 1);
		}

		handler.handleMessage(message);
		assertEquals(3, redisZset.size());
		pepboys = redisZset.rangeByScoreWithScores(1, 2);
		// should have incremented by 1
		for (TypedTuple<String> pepboy : pepboys) {
			assertEquals(Double.valueOf(2), pepboy.getScore());
		}
	}

	@Test
	@RedisAvailable
	public void testZsetWithListPayloadParsedAndProvidedKeyScoreIncrement() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>(key, this.initTemplate(jcf, new StringRedisTemplate()));

		assertEquals(0, redisZset.size());

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, Boolean.TRUE)
				.build();

		handler.handleMessage(message);

		assertEquals(3, redisZset.size());
		Set<TypedTuple<String>> pepboys = redisZset.rangeByScoreWithScores(1, 1);
		for (TypedTuple<String> pepboy : pepboys) {
			assertTrue(pepboy.getScore() == 1);
		}

		handler.handleMessage(message);
		assertEquals(3, redisZset.size());
		pepboys = redisZset.rangeByScoreWithScores(1, 2);
		// should have incremented
		for (TypedTuple<String> pepboy : pepboys) {
			assertTrue(pepboy.getScore() == 2);
		}
	}

	@Test
	@RedisAvailable
	public void testZsetWithListPayloadParsedAndProvidedKeyScoreIncrementAsStringHeader() {// see INT-2775
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>(key, this.initTemplate(jcf, new StringRedisTemplate()));

		assertEquals(0, redisZset.size());

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list)
				.setHeader(RedisHeaders.ZSET_INCREMENT_SCORE, "true")
				.build();

		handler.handleMessage(message);

		assertEquals(3, redisZset.size());
		Set<TypedTuple<String>> pepboys = redisZset.rangeByScoreWithScores(1, 1);
		for (TypedTuple<String> pepboy : pepboys) {
			assertTrue(pepboy.getScore() == 1);
		}

		handler.handleMessage(message);
		assertEquals(3, redisZset.size());
		pepboys = redisZset.rangeByScoreWithScores(1, 2);
		// should have incremented
		for (TypedTuple<String> pepboy : pepboys) {
			assertTrue(pepboy.getScore() == 2);
		}
	}

	@Test
	@RedisAvailable
	public void testZsetWithListPayloadAsSingleEntryAndHeaderKeyHeaderScore() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisZSet<List<String>> redisZset =
				new DefaultRedisZSet<List<String>>(key, this.initTemplate(jcf, new RedisTemplate<String, List<String>>()));

		assertEquals(0, redisZset.size());

		RedisTemplate<String, List<String>> template = this.initTemplate(jcf, new RedisTemplate<String, List<String>>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);

		handler.setCollectionType(CollectionType.ZSET);
		handler.setExtractPayloadElements(false);
		handler.afterPropertiesSet();

		List<String> list = new ArrayList<String>();
		list.add("Manny");
		list.add("Moe");
		list.add("Jack");
		Message<List<String>> message = MessageBuilder.withPayload(list).setHeader("redis_key", key).
						setHeader("redis_zsetScore", 4).build();
		handler.handleMessage(message);

		assertEquals(1, redisZset.size());
		Set<TypedTuple<List<String>>> entries = redisZset.rangeByScoreWithScores(1, 4);
		for (TypedTuple<List<String>> pepboys : entries) {
			assertTrue(pepboys.getScore() == 4);
		}
	}

	@Test
	@RedisAvailable
	public void testZsetWithMapPayloadParsedHeaderKey() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "presidents";
		RedisZSet<String> redisZset =
				new DefaultRedisZSet<String>(key, this.initTemplate(jcf, new StringRedisTemplate()));

		assertEquals(0, redisZset.size());

		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.afterPropertiesSet();

		Map<String, Double> presidents = new HashMap<String, Double>();
		presidents.put("John Adams", 18D);

		presidents.put("Barack Obama", 21D);
		presidents.put("Thomas Jefferson", 19D);
		presidents.put("John Quincy Adams", 19D);
		presidents.put("Zachary Taylor", 19D);

		presidents.put("Theodore Roosevelt", 20D);
		presidents.put("Woodrow Wilson", 20D);
		presidents.put("George W. Bush", 21D);
		presidents.put("Franklin D. Roosevelt", 20D);
		presidents.put("Ronald Reagan", 20D);
		presidents.put("William J. Clinton", 20D);
		presidents.put("Abraham Lincoln", 19D);
		presidents.put("George Washington", 18D);

		Message<Map<String, Double>> message = MessageBuilder.withPayload(presidents).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertEquals(13, redisZset.size());

		Set<TypedTuple<String>> entries = redisZset.rangeByScoreWithScores(18, 19);
		assertEquals(6, entries.size());
	}

	@Test
	@RedisAvailable
	public void testZsetWithMapPayloadPojoParsedHeaderKey() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "presidents";
		RedisZSet<President> redisZset =
				new DefaultRedisZSet<President>(key, this.initTemplate(jcf, new RedisTemplate<String, President>()));

		assertEquals(0, redisZset.size());

		RedisTemplate<String, President> template = this.initTemplate(jcf, new RedisTemplate<String, President>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.afterPropertiesSet();

		Map<President, Double> presidents = new HashMap<President, Double>();
		presidents.put(new President("John Adams"), 18D);

		presidents.put(new President("Barack Obama"), 21D);
		presidents.put(new President("Thomas Jefferson"), 19D);
		presidents.put(new President("John Quincy Adams"), 19D);
		presidents.put(new President("Zachary Taylor"), 19D);

		presidents.put(new President("Theodore Roosevelt"), 20D);
		presidents.put(new President("Woodrow Wilson"), 20D);
		presidents.put(new President("George W. Bush"), 21D);
		presidents.put(new President("Franklin D. Roosevelt"), 20D);
		presidents.put(new President("Ronald Reagan"), 20D);
		presidents.put(new President("William J. Clinton"), 20D);
		presidents.put(new President("Abraham Lincoln"), 19D);
		presidents.put(new President("George Washington"), 18D);

		Message<Map<President, Double>> message = MessageBuilder.withPayload(presidents).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertEquals(13, redisZset.size());

		Set<TypedTuple<President>> entries = redisZset.rangeByScoreWithScores(18, 19);
		assertEquals(6, entries.size());
	}

	@Test
	@RedisAvailable
	public void testZsetWithMapPayloadPojoAsSingleEntryHeaderKey() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "presidents";
		RedisZSet<Map<President, Double>> redisZset =
				new DefaultRedisZSet<Map<President, Double>>(key, this.initTemplate(jcf, new RedisTemplate<String, Map<President, Double>>()));

		assertEquals(0, redisZset.size());

		RedisTemplate<String, Map<President, Double>> template = this.initTemplate(jcf, new RedisTemplate<String, Map<President, Double>>());
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(template);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setExtractPayloadElements(false);
		handler.afterPropertiesSet();

		Map<President, Double> presidents = new HashMap<President, Double>();
		presidents.put(new President("John Adams"), 18D);

		presidents.put(new President("Barack Obama"), 21D);
		presidents.put(new President("Thomas Jefferson"), 19D);

		Message<Map<President, Double>> message = MessageBuilder.withPayload(presidents).setHeader("redis_key", key).build();
		handler.handleMessage(message);

		assertEquals(1, redisZset.size());
	}

	@Test(expected=IllegalStateException.class)
	@RedisAvailable
	public void testListWithMapKeyExpression() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.afterPropertiesSet();
	}

	@Test(expected=IllegalStateException.class)
	@RedisAvailable
	public void testSetWithMapKeyExpression() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.SET);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.afterPropertiesSet();
	}

	@Test(expected=IllegalStateException.class)
	@RedisAvailable
	public void testZsetWithMapKeyExpression() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.ZSET);
		handler.setMapKeyExpression(new LiteralExpression(key));
		handler.afterPropertiesSet();
	}

	@Test
	@RedisAvailable
	public void testMapWithMapKeyExpression() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.MAP);
		handler.setMapKeyExpression(new LiteralExpression(key));
		try {
			handler.afterPropertiesSet();
		}
		catch (Exception e) {
			fail("No exception expected:" + e.getMessage());
		}
	}

	@Test
	@RedisAvailable
	public void testPropertiesWithMapKeyExpression() {
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		String key = "foo";
		RedisStoreWritingMessageHandler handler =
				new RedisStoreWritingMessageHandler(jcf);
		handler.setKey(key);
		handler.setCollectionType(CollectionType.PROPERTIES);
		handler.setMapKeyExpression(new LiteralExpression(key));
		try {
			handler.afterPropertiesSet();
		}
		catch (Exception e) {
			fail("No exception expected:" + e.getMessage());
		}
	}

	private <K,V> RedisTemplate<K,V> initTemplate(RedisConnectionFactory rcf, RedisTemplate<K,V> redisTemplate) {
		redisTemplate.setConnectionFactory(rcf);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

	private static class President implements Serializable {
		private static final long serialVersionUID = 1L;
		private String name;

		public President(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return name;
		}

		@SuppressWarnings("unused")
		public void setName(String name) {
			this.name = name;
		}
	}
}

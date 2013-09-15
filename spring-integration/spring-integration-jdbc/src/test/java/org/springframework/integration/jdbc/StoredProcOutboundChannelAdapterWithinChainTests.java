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

package org.springframework.integration.jdbc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Artem Bilan
 * @since 2.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StoredProcOutboundChannelAdapterWithinChainTests {

	@Autowired
	private AbstractApplicationContext context;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MessageChannel jdbcStoredProcOutboundChannelAdapterWithinChain;


	@Test
	public void test() {
		Message<User> message = MessageBuilder.withPayload(new User("username", "password", "email")).build();
		this.jdbcStoredProcOutboundChannelAdapterWithinChain.send(message);

		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertEquals("Wrong username", "username", map.get("USERNAME"));
		assertEquals("Wrong password", "password", map.get("PASSWORD"));
		assertEquals("Wrong email", "email", map.get("EMAIL"));
//		embeddedDatabase can be in working state. So other tests with the same embeddedDatabase beanId, type and init scripts
//		may be failed with Exception like: object in the DB already exists
		this.context.destroy();
	}

}

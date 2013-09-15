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

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.Message;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * @author Jonas Partner
 * @author Gary Russell
 */
public class JdbcPollingChannelAdapterIntegrationTests {

	private static Log logger = LogFactory.getLog(JdbcPollingChannelAdapterIntegrationTests.class);

	private EmbeddedDatabase embeddedDatabase;

	private JdbcTemplate jdbcTemplate;

	@Before
	public void setUp() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder
				.setType(EmbeddedDatabaseType.DERBY)
				.addScript(
						"classpath:org/springframework/integration/jdbc/pollingChannelAdapterIntegrationTest.sql");
		this.embeddedDatabase = builder.build();
		this.jdbcTemplate = new JdbcTemplate(this.embeddedDatabase);
	}

	@After
	public void tearDown() {
		this.embeddedDatabase.shutdown();
	}

	@Test
	public void testSimplePollForListOfMapsNoUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase, "select * from item");
		this.jdbcTemplate.update("insert into item values(1,2)");
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertTrue("Wrong payload type", payload instanceof List<?>);
		List<?> rows = (List<?>) payload;
		assertEquals("Wrong number of elements", 1, rows.size());
		assertTrue("Returned row not a map", rows.get(0) instanceof Map<?, ?>);
		Map<?, ?> row = (Map<?, ?>) rows.get(0);
		assertEquals("Wrong id", 1, row.get("id"));
		assertEquals("Wrong status", 2, row.get("status"));

	}

	@Test
	public void testParameterizedPollForListOfMapsNoUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase,
				"select * from item where status=:status");
		adapter.setSelectSqlParameterSource(new SqlParameterSource() {

			public boolean hasValue(String name) {
				return "status".equals(name);
			}

			public Object getValue(String name) throws IllegalArgumentException {
				return 2;
			}

			public String getTypeName(String name) {
				return null;
			}

			public int getSqlType(String name) {
				return Types.INTEGER;
			}
		});
		this.jdbcTemplate.update("insert into item values(1,2)");
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertTrue("Wrong payload type", payload instanceof List<?>);
		List<?> rows = (List<?>) payload;
		assertEquals("Wrong number of elements", 1, rows.size());
		assertTrue("Returned row not a map", rows.get(0) instanceof Map<?, ?>);
		Map<?, ?> row = (Map<?, ?>) rows.get(0);
		assertEquals("Wrong id", 1, row.get("id"));
		assertEquals("Wrong status", 2, row.get("status"));

	}

	@Test
	public void testSimplePollForListWithRowMapperNoUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase, "select * from item");
		adapter.setRowMapper(new ItemRowMapper());
		this.jdbcTemplate.update("insert into item values(1,2)");
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertEquals("Wrong number of elements", 1, rows.size());
		assertTrue("Wrong payload type", rows.get(0) instanceof Item);
		Item item = (Item) rows.get(0);
		assertEquals("Wrong id", 1, item.getId());
		assertEquals("Wrong status", 2, item.getStatus());

	}

	@Test
	public void testSimplePollForListWithRowMapperAndOneUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase, "select * from item where status=2");
		adapter
				.setUpdateSql("update item set status = 10 where id in (:id)");
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		this.jdbcTemplate.update("insert into item values(1,2)");
		this.jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertEquals("Wrong number of elements", 2, rows.size());
		assertTrue("Wrong payload type", rows.get(0) instanceof Item);
		Item item = (Item) rows.get(0);
		assertEquals("Wrong id", 1, item.getId());
		assertEquals("Wrong status", 2, item.getStatus());

		int countOfStatusTwo = this.jdbcTemplate
				.queryForInt("select count(*) from item where status = 2");
		assertEquals(
				"Status not updated incorect number of rows with status 2", 0,
				countOfStatusTwo);

		int countOfStatusTen = this.jdbcTemplate
				.queryForInt("select count(*) from item where status = 10");
		assertEquals(
				"Status not updated incorect number of rows with status 10", 2,
				countOfStatusTen);

	}

	@Test
	public void testSimplePollForListWithRowMapperAndUpdatePerRow() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase, "select * from item where status=2");
		adapter.setUpdateSql("update item set status = 10 where id = :id");
		adapter.setUpdatePerRow(true);
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		this.jdbcTemplate.update("insert into item values(1,2)");
		this.jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertEquals("Wrong number of elements", 2, rows.size());
		assertTrue("Wrong payload type", rows.get(0) instanceof Item);
		Item item = (Item) rows.get(0);
		assertEquals("Wrong id", 1, item.getId());
		assertEquals("Wrong status", 2, item.getStatus());

		int countOfStatusTwo = this.jdbcTemplate
				.queryForInt("select count(*) from item where status = 2");
		assertEquals(
				"Status not updated incorect number of rows with status 2", 0,
				countOfStatusTwo);

		int countOfStatusTen = this.jdbcTemplate
				.queryForInt("select count(*) from item where status = 10");
		assertEquals(
				"Status not updated incorect number of rows with status 10", 2,
				countOfStatusTen);

	}

	@Test
	public void testSimplePollForListWithRowMapperAndInsertPerRowAndMaxRows() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase, "select * from item where id not in (select id from copy)");
		adapter.setUpdateSql("insert into copy values(:id,10)");
		adapter.setUpdatePerRow(true);
		adapter.setMaxRowsPerPoll(1);
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		this.jdbcTemplate.update("insert into item values(1,2)");
		this.jdbcTemplate.update("insert into item values(2,2)");

		logger.debug(adapter.receive());
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertEquals("Wrong number of elements", 1, rows.size());
		assertTrue("Wrong payload type", rows.get(0) instanceof Item);
		Item item = (Item) rows.get(0);
		logger.debug(item);
		assertEquals("Wrong id", 2, item.getId());
		assertEquals("Wrong status", 2, item.getStatus());

		int countOfStatusTwo = this.jdbcTemplate
				.queryForInt("select count(*) from item where status = 2");
		assertEquals(
				"Status not updated incorect number of rows with status 2", 2,
				countOfStatusTwo);

		int countOfStatusTen = this.jdbcTemplate
				.queryForInt("select count(*) from copy where status = 10");
		assertEquals(
				"Status not updated incorect number of rows with status 10", 2,
				countOfStatusTen);

	}

	@Test
	public void testSimplePollForListWithRowMapperAndUpdatePerRowWithMaxRows() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase, "select * from item where status=2");
		adapter.setUpdateSql("update item set status = 10 where id = :id");
		adapter.setUpdatePerRow(true);
		adapter.setMaxRowsPerPoll(1);
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		this.jdbcTemplate.update("insert into item values(1,2)");
		this.jdbcTemplate.update("insert into item values(2,2)");

		adapter.receive();
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertEquals("Wrong number of elements", 1, rows.size());
		assertTrue("Wrong payload type", rows.get(0) instanceof Item);
		Item item = (Item) rows.get(0);
		assertEquals("Wrong id", 2, item.getId());
		assertEquals("Wrong status", 2, item.getStatus());

		int countOfStatusTwo = this.jdbcTemplate
				.queryForInt("select count(*) from item where status = 2");
		assertEquals(
				"Status not updated incorect number of rows with status 2", 0,
				countOfStatusTwo);

		int countOfStatusTen = this.jdbcTemplate
				.queryForInt("select count(*) from item where status = 10");
		assertEquals(
				"Status not updated incorect number of rows with status 10", 2,
				countOfStatusTen);

	}

	@Test
	public void testEmptyPoll() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(
				this.embeddedDatabase, "select * from item");
		Message<Object> message = adapter.receive();
		assertNull("Message received when no rows in table", message);

	}

	private static class Item {

		private int id;

		private int status;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		@Override
		public String toString() {
			return "Item [id=" + id + ", status=" + status + "]";
		}
	}

	private static class ItemRowMapper implements RowMapper<Item> {

		public Item mapRow(ResultSet rs, int rowNum) throws SQLException {
			Item item = new Item();
			item.setId(rs.getInt(1));
			item.setStatus(rs.getInt(2));
			return item;
		}

	}

}
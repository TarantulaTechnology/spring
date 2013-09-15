/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.mongodb.store;

import org.junit.Test;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.Message;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;

import com.mongodb.Mongo;

import static org.junit.Assert.assertEquals;

/**
 * @author Mark Fisher
 */
public class MongoDbMessageStoreClaimCheckIntegrationTests extends MongoDbAvailableTests{

	@Test 
	@MongoDbAvailable
	public void stringPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoDbMessageStore messageStore = new MongoDbMessageStore(mongoDbFactory);		
		ClaimCheckInTransformer checkin = new ClaimCheckInTransformer(messageStore);
		ClaimCheckOutTransformer checkout = new ClaimCheckOutTransformer(messageStore);
		Message<?> originalMessage = MessageBuilder.withPayload("test1").build();
		Message<?> claimCheckMessage = checkin.transform(originalMessage);
		assertEquals(originalMessage.getHeaders().getId(), claimCheckMessage.getPayload());
		Message<?> checkedOutMessage = checkout.transform(claimCheckMessage);
		assertEquals(claimCheckMessage.getPayload(), checkedOutMessage.getHeaders().getId());
		assertEquals(originalMessage.getPayload(), checkedOutMessage.getPayload());
		assertEquals(originalMessage, checkedOutMessage);
		//System.out.println("original:   " + originalMessage);
		//System.out.println("claimcheck: " + claimCheckMessage);
		//System.out.println("checkedout: " + checkedOutMessage);
	}

	@Test 
	@MongoDbAvailable
	public void objectPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoDbMessageStore messageStore = new MongoDbMessageStore(mongoDbFactory);
		ClaimCheckInTransformer checkin = new ClaimCheckInTransformer(messageStore);
		ClaimCheckOutTransformer checkout = new ClaimCheckOutTransformer(messageStore);
		Beverage payload = new Beverage();
		payload.setName("latte");
		payload.setShots(3);
		payload.setIced(false);
		Message<?> originalMessage = MessageBuilder.withPayload(payload).build();
		Message<?> claimCheckMessage = checkin.transform(originalMessage);
		assertEquals(originalMessage.getHeaders().getId(), claimCheckMessage.getPayload());
		Message<?> checkedOutMessage = checkout.transform(claimCheckMessage);
		assertEquals(originalMessage.getPayload(), checkedOutMessage.getPayload());
		assertEquals(claimCheckMessage.getPayload(), checkedOutMessage.getHeaders().getId());
		assertEquals(originalMessage, checkedOutMessage);
	}


	@SuppressWarnings("unused")
	private static class Beverage {

		private String name;
		private int shots; 
		private boolean iced;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getShots() {
			return shots;
		}

		public void setShots(int shots) {
			this.shots = shots;
		}

		public boolean isIced() {
			return iced;
		}

		public void setIced(boolean iced) {
			this.iced = iced;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (iced ? 1231 : 1237);
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + shots;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Beverage other = (Beverage) obj;
			if (iced != other.iced) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			}
			else if (!name.equals(other.name)) {
				return false;
			}
			if (shots != other.shots) {
				return false;
			}
			return true;
		}
	}

}

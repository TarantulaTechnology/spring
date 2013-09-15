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

package org.springframework.integration.store;

import java.util.Collection;

import org.springframework.integration.Message;

/**
 * A group of messages that are correlated with each other and should be processed in the same context.
 * <p>
 * The message group allows implementations to be mutable, but this behavior is optional. Implementations should take
 * care to document their thread safety and mutability.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 */
public interface MessageGroup {

	/**
	 * Query if the message can be added.
	 */
	boolean canAdd(Message<?> message);

	/**
	 * Returns all available Messages from the group at the time of invocation
	 */
	Collection<Message<?>> getMessages();

	/**
	 * @return the key that links these messages together
	 */
	Object getGroupId();

	/**
	 * Returns the sequenceNumber of the last released message. Used in Resequencer use cases only
	 */
	int getLastReleasedMessageSequenceNumber();

	/**
	 * @return true if the group is complete (i.e. no more messages are expected to be added)
	 */
	boolean isComplete();

	/**
	 *
	 */
	void complete();

	/**
	 * @return the size of the sequence expected 0 if unknown
	 */
	int getSequenceSize();

	/**
	 * @return the total number of messages in this group
	 */
	int size();

	/**
	 * @return a single message from the group
	 */
	Message<?> getOne();

	/**
	 * @return the timestamp (milliseconds since epoch) associated with the creation of this group
	 */
	long getTimestamp();

	/**
	 * @return the timestamp (milliseconds since epoch) associated with the time this group was last updated
	 */
	long getLastModified();

}

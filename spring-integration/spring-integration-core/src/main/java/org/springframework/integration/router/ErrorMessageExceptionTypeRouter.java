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

package org.springframework.integration.router;

import java.util.Collections;
import java.util.List;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;

/**
 * A Message Router that resolves the target {@link MessageChannel} for
 * messages whose payload is an Exception. The channel resolution is based upon
 * the most specific cause of the error for which a channel-mapping exists.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */  
public class ErrorMessageExceptionTypeRouter extends AbstractMappingMessageRouter {

	@Override
	protected List<Object> getChannelKeys(Message<?> message) {
		String mostSpecificCause = null;
		Object payload = message.getPayload();
		if (payload instanceof Throwable) {
			Throwable cause = (Throwable) payload;
			while (cause != null) {
				String causeName = cause.getClass().getName();
				if (this.getChannelMappings().keySet().contains(causeName)) {
					mostSpecificCause = causeName;
				}
				cause = cause.getCause();
			}
		}
		return Collections.singletonList((Object) mostSpecificCause);
	}

}

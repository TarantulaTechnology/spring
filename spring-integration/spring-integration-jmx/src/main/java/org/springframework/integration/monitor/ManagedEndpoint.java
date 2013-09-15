/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.monitor;

import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Wrapper for an {@link AbstractEndpoint} that exposes a management interface.
 * 
 * @author Dave Syer
 * 
 */
@ManagedResource
public class ManagedEndpoint implements Lifecycle {

	private final AbstractEndpoint delegate;

	public ManagedEndpoint(AbstractEndpoint delegate) {
		this.delegate = delegate;
	}

	@ManagedAttribute
	public final boolean isRunning() {
		return delegate.isRunning();
	}

	@ManagedOperation
	public final void start() {
		delegate.start();
	}

	@ManagedOperation
	public final void stop() {
		delegate.stop();
	}

}

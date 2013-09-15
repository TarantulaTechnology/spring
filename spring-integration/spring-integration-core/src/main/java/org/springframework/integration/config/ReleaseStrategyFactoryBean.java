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
package org.springframework.integration.config;

import java.lang.reflect.Method;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.SequenceSizeReleaseStrategy;
import org.springframework.util.StringUtils;

/**
 * Convenience factory for XML configuration of a {@link ReleaseStrategy}. Encapsulates the knowledge of the default
 * strategy and search algorithms for POJO and annotated methods.
 * 
 * @author Dave Syer
 * 
 */
public class ReleaseStrategyFactoryBean implements FactoryBean<ReleaseStrategy> {

	private ReleaseStrategy delegate = new SequenceSizeReleaseStrategy();

	/**
	 * Create a factory and set up the delegate which clients of the factory will see as its product.
	 * 
	 * @param target the target object (null if default strategy is acceptable)
	 */
	public ReleaseStrategyFactoryBean(Object target) {
		this(target, null);
	}

	/**
	 * Create a factory and set up the delegate which clients of the factory will see as its product.
	 * 
	 * @param target the target object (null if default strategy is acceptable)
	 * @param methodName the method name to invoke in the target (null if it can be inferred)
	 */
	public ReleaseStrategyFactoryBean(Object target, String methodName) {
		if (target instanceof ReleaseStrategy && !StringUtils.hasText(methodName)) {
			delegate = (ReleaseStrategy) target;
			return;
		}
		if (target != null) {
			if (StringUtils.hasText(methodName)) {
				delegate = new MethodInvokingReleaseStrategy(target, methodName);
			}
			else {
				Method method = AnnotationFinder.findAnnotatedMethod(target, org.springframework.integration.annotation.ReleaseStrategy.class);
				if (method != null) {
					delegate = new MethodInvokingReleaseStrategy(target, method);
				}
			}
		}
	}

	public ReleaseStrategy getObject() throws Exception {
		return delegate;
	}

	public Class<?> getObjectType() {
		return ReleaseStrategy.class;
	}

	public boolean isSingleton() {
		return true;
	}

}

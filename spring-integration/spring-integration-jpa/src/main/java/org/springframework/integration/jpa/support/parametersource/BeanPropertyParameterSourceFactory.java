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
package org.springframework.integration.jpa.support.parametersource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class BeanPropertyParameterSourceFactory implements ParameterSourceFactory {

	private volatile Map<String, Object> staticParameters;

	public BeanPropertyParameterSourceFactory() {
		this.staticParameters = Collections.unmodifiableMap(new HashMap<String, Object>());
	}

	/**
	 * If the input is a List or a Map, the output is a map parameter source, and in that case some static parameters
	 * can be added (default is empty). If the input is not a List or a Map then this value is ignored.
	 *
	 * @param staticParameters the static parameters to set
	 */
	public void setStaticParameters(Map<String, Object> staticParameters) {
		this.staticParameters = staticParameters;
	}

	public ParameterSource createParameterSource(Object input) {
		ParameterSource toReturn = new StaticBeanPropertyParameterSource(input, staticParameters);
		return toReturn;
	}

	private static class StaticBeanPropertyParameterSource implements
			ParameterSource {

		private final BeanPropertyParameterSource input;

		private final Map<String, Object> staticParameters;

		public StaticBeanPropertyParameterSource(Object input, Map<String, Object> staticParameters) {
			this.input = new BeanPropertyParameterSource(input);
			this.staticParameters = staticParameters;
		}

		public Object getValue(String paramName) {
			return staticParameters.containsKey(paramName) ? staticParameters.get(paramName) : input
					.getValue(paramName);
		}

		public boolean hasValue(String paramName) {
			return staticParameters.containsKey(paramName) || input.hasValue(paramName);
		}

	}

}

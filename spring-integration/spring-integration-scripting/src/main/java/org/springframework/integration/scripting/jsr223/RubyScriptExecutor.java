/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.scripting.jsr223;

import org.springframework.util.ClassUtils;


/**
 * @author David Turanski
 * @since 2.1
 *
 */
 class RubyScriptExecutor extends DefaultScriptExecutor {

	static {
		if (ClassUtils.isPresent("org.jruby.embed.jsr223.JRubyEngine", System.class.getClassLoader())) {
			System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
			System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");
		}
	}
	 
	public RubyScriptExecutor() {
		super("ruby");
	}
}

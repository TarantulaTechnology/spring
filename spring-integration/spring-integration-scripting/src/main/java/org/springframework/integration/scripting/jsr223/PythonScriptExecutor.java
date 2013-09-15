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

import javax.script.ScriptEngine;

import org.springframework.integration.scripting.ScriptExecutor;

/**
 * A {@link ScriptExecutor} that implements special handling required for Python to emulate behavior similar to other JSR223 scripting languages.
 * <p>
 * Script evaluation using the Jython implementation results in a <code>null</code> return value for normal variable expressions such as 
 * <code>x=2</code>. As a work around, it is necessary to get the value of 'x' explicitly following the script evaluation. This class performs
 * simple parsing on the last line of the script to obtain the variable name, if any, and return its value.  
 *    
 * @author David Turanski
 * @since 2.1
 *
 */
 class PythonScriptExecutor extends AbstractScriptExecutor {
	/**
	 * @param language
	 */
	public PythonScriptExecutor() {
		super("python"); 
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.integration.scripting.jsr223.AbstractScriptExecutor#postProcess(java.lang.Object, javax.script.ScriptEngine, java.lang.String)
	 */
	@Override
	protected Object postProcess(Object result, ScriptEngine scriptEngine, String script) {
		Object newResult= result;
		if (newResult == null) {
			String returnVariableName = PythonVariableParser.parseReturnVariable(script);
			newResult = scriptEngine.get(returnVariableName);
		}
		return newResult;
	}
	
	public static class PythonVariableParser {
		public static String parseReturnVariable(String script){
			String[] lines = script.trim().split("\n");
			String lastLine = lines[lines.length -1];
			String[] tokens = lastLine.split("=");
			return tokens[0].trim();
		}
	}
}

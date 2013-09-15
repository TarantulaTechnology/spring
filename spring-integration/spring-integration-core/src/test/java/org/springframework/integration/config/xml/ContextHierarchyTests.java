/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Mark Fisher
 */
public class ContextHierarchyTests {

	private ApplicationContext parentContext;

	private ApplicationContext childContext;


	@Before
	public void setupContext() {
		String prefix = "/org/springframework/integration/config/xml/ContextHierarchyTests-";
		this.parentContext = new ClassPathXmlApplicationContext(prefix + "parent.xml");
		this.childContext = new ClassPathXmlApplicationContext(
				new String[] { prefix + "child.xml" }, parentContext);
	}


	@Test // INT-646
	public void inputChannelInParentContext() {
		Object parentInput = parentContext.getBean("input");
		Object childInput = childContext.getBean("input");
		Object endpoint = childContext.getBean("chain");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		Object endpointInput = accessor.getPropertyValue("inputChannel");
		assertEquals(parentInput, childInput);
		assertEquals(parentInput, endpointInput);
	}

}

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
package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 * This test was influenced by INT-1483 where by registering TX Advisor
 * in the BeanFactory while having <aop:config> resent resulted in
 * TX Advisor being applied on all beans in AC
 */
public class TransactionalPollerWithMixedAopConfigTests {

	@Test
	public void validateTransactionalProxyIsolationToThePollerOnly(){
		ApplicationContext context =
			new ClassPathXmlApplicationContext("TransactionalPollerWithMixedAopConfig-context.xml", this.getClass());

		assertTrue(!(context.getBean("foo") instanceof Advised));
		assertTrue(!(context.getBean("inputChannel") instanceof Advised));
	}

	public static class SampleService{
		public void foo(String payload){}
	}

	public static class Foo{
		public Foo(String value){}
	}

//	public static class SampleAdvice implements MethodInterceptor{
//		public Object invoke(MethodInvocation invocation) throws Throwable {
//			return invocation.proceed();
//		}
//	}
}

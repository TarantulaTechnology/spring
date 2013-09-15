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
package org.springframework.integration.test.matcher;

import static org.hamcrest.CoreMatchers.anything;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;

/**
 * Matchers that examine the contents of a {@link Map}.
 * <p>
 * It is possible to match a single entry by value or matcher like this:
 * </p>
 *
 * <pre>
 * assertThat(map, hasEntry(SOME_KEY, is(SOME_VALUE)));
 * assertThat(map, hasEntry(SOME_KEY, is(String.class)));
 * assertThat(map, hasEntry(SOME_KEY, notNullValue()));
 * </pre>
 *
 * <p>
 * It's also possible to match multiple entries in a map:
 * </p>
 *
 * <pre>
 * Map&lt;String, Object&gt; expectedInMap = new HashMap&lt;String, Object&gt;();
 * expectedInMap.put(SOME_KEY, SOME_VALUE);
 * expectedInMap.put(OTHER_KEY, is(OTHER_VALUE));
 * assertThat(map, hasAllEntries(expectedInMap));
 * </pre>
 *
 * <p>If you only need to verify the existence of a key:</p>
 *
 * <pre>
 * assertThat(map, hasKey(SOME_KEY));
 * </pre>
 *
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 *
 */
public class MapContentMatchers<T, V> extends
		TypeSafeMatcher<Map<? super T, ? super V>> {

	private final T key;

	private final Matcher<V> valueMatcher;

	/**
	 * @param key
	 * @param value
	 */
	MapContentMatchers(T key, V value) {
		this(key, IsEqual.equalTo(value));
	}

	/**
	 * @param key
	 * @param valueMatcher
	 */
	MapContentMatchers(T key, Matcher<V> valueMatcher) {
		super();
		this.key = key;
		this.valueMatcher = valueMatcher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean matchesSafely(Map<? super T, ? super V> item) {
		return item.containsKey(key) && valueMatcher.matches(item.get(key));
	}

	/**
	 * {@inheritDoc}
	 */
//	@Override
	public void describeTo(Description description) {
		description.appendText("an entry with key ").appendValue(key)
				.appendText(" and value matching ").appendDescriptionOf(
						valueMatcher);

	}

	@Factory
	public static <T, V> Matcher<Map<? super T, ? super V>> hasEntry(T key,
			V value) {
		return new MapContentMatchers<T, V>(key, value);
	}

	@Factory
	public static <T, V> Matcher<Map<? super T, ? super V>> hasEntry(T key,
			Matcher<V> valueMatcher) {
		return new MapContentMatchers<T, V>(key, valueMatcher);
	}

	@Factory
	@SuppressWarnings("unchecked")
	public static <T, V> Matcher<Map<? super T, ? super V>> hasKey(T key) {
		return new MapContentMatchers<T, V>(key, (Matcher<V>) anything("any Value"));
	}

	@Factory
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T, V> Matcher<Map<? super T, ? super V>> hasAllEntries(
			Map<T, V> entries) {
		List<Matcher<? extends Map<? super T, ? super V>>> matchers = new ArrayList<Matcher<? extends Map<? super T, ? super V>>>(
				entries.size());
		for (Map.Entry<T, V> entry : entries.entrySet()) {
			final V value = entry.getValue();
			if (value instanceof Matcher<?>) {
				matchers.add(hasEntry(entry.getKey(), (Matcher<V>) value));
			}
			else {
				matchers.add(hasEntry(entry.getKey(), value));
			}
		}
		//return AllOf.allOf(matchers); //Does not work with Hamcrest 1.3
		return new AllOf(matchers);
	}
}

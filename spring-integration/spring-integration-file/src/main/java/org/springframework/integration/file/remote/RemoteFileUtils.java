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
package org.springframework.integration.file.remote;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.springframework.integration.file.remote.session.Session;

/**
 * Utility methods for supporting remote file operations.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class RemoteFileUtils {

	private RemoteFileUtils() {}

	/**
	 * Recursively create remote directories.
	 * @param <F> The session type.
	 * @param path The directory path.
	 * @param session The session.
	 * @throws IOException
	 */
	public static <F> void makeDirectories(String path, Session<F> session, String remoteFileSeparator, Log logger)
			throws IOException {

		if (!session.exists(path)){

			int nextSeparatorIndex = path.lastIndexOf(remoteFileSeparator);

			if (nextSeparatorIndex > -1){
				List<String> pathsToCreate = new LinkedList<String>();
				while (nextSeparatorIndex > -1){
					String pathSegment = path.substring(0, nextSeparatorIndex);
					if (pathSegment.length() == 0 || session.exists(pathSegment)) {
						// no more paths to create
						break;
					}
					else {
						pathsToCreate.add(0, pathSegment);
						nextSeparatorIndex = pathSegment.lastIndexOf(remoteFileSeparator);
					}
				}

				for (String pathToCreate : pathsToCreate) {
					if (logger.isDebugEnabled()){
						logger.debug("Creating '" + pathToCreate + "'");
					}
					session.mkdir(pathToCreate);
				}
			}
			else {
				session.mkdir(path);
			}
		}
	}

}

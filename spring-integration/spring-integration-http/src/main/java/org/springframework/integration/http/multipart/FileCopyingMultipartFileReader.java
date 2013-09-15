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

package org.springframework.integration.http.multipart;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.multipart.MultipartFile;

/**
 * {@link MultipartFileReader} implementation that copies the MulitpartFile's
 * content to a new temporary File in the specified directory. If no directory
 * is provided, the Files will be created in the default temporary directory.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class FileCopyingMultipartFileReader implements MultipartFileReader<MultipartFile> {

	private static final Log logger = LogFactory.getLog(FileCopyingMultipartFileReader.class);


	private final File directory;

	private volatile String prefix = "si_";

	private volatile String suffix = ".tmp";


	/**
	 * Create a {@link FileCopyingMultipartFileReader} that creates temporary
	 * Files in the default temporary directory.
	 */	
	public FileCopyingMultipartFileReader() {
		this(null);
	}

	/**
	 * Create a {@link FileCopyingMultipartFileReader} that creates temporary
	 * Files in the given directory.
	 */
	public FileCopyingMultipartFileReader(File directory) {
		this.directory = directory;
	}


	/**
	 * Specify the prefix to use for temporary files.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Specify the suffix to use for temporary files.
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public MultipartFile readMultipartFile(MultipartFile multipartFile) throws IOException {
		File upload = File.createTempFile(this.prefix, this.suffix, this.directory);
		multipartFile.transferTo(upload);
		UploadedMultipartFile uploadedMultipartFile = new UploadedMultipartFile(upload, multipartFile.getSize(),
				multipartFile.getContentType(), multipartFile.getName(), multipartFile.getOriginalFilename());
		if (logger.isDebugEnabled()) {
			logger.debug("copied uploaded file [" + multipartFile.getOriginalFilename() +
					"] to [" + upload.getAbsolutePath() + "]");
		}
		return uploadedMultipartFile;
	}

}

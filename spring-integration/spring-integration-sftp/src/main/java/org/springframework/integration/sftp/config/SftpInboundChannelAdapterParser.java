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

package org.springframework.integration.sftp.config;

import org.springframework.integration.file.config.AbstractRemoteFileInboundChannelAdapterParser;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;

/**
 * Parser for 'sftp:inbound-channel-adapter'
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class SftpInboundChannelAdapterParser extends AbstractRemoteFileInboundChannelAdapterParser {

	@Override
	protected String getMessageSourceClassname() {
		return SftpInboundFileSynchronizingMessageSource.class.getName();
	}

	@Override
	protected String getInboundFileSynchronizerClassname() {
		return SftpInboundFileSynchronizer.class.getName();
	}

	@Override
	protected String getSimplePatternFileListFilterClassname() {
		return SftpSimplePatternFileListFilter.class.getName();
	}

	@Override
	protected String getRegexPatternFileListFilterClassname() {
		return SftpRegexPatternFileListFilter.class.getName();
	}

}

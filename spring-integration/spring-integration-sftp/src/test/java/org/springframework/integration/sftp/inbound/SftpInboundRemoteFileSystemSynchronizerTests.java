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

package org.springframework.integration.sftp.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import org.junit.After;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpTestSessionFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.0
 */
public class SftpInboundRemoteFileSystemSynchronizerTests {

	private static com.jcraft.jsch.Session jschSession = mock(com.jcraft.jsch.Session.class);

	@After
	public void cleanup(){
		File file = new File("test");
		if (file.exists()){
			String[] files = file.list();
			for (String fileName : files) {
				new File(file, fileName).delete();
			}
			file.delete();
		}
	}

	@Test
	public void testCopyFileToLocalDir() throws Exception {
		this.cleanup();
		File localDirectoy = new File("test");
		assertFalse(localDirectoy.exists());

		TestSftpSessionFactory ftpSessionFactory = new TestSftpSessionFactory();
		ftpSessionFactory.setUser("kermit");
		ftpSessionFactory.setPassword("frog");
		ftpSessionFactory.setHost("foo.com");


		SftpInboundFileSynchronizer synchronizer = spy(new SftpInboundFileSynchronizer(ftpSessionFactory));
		synchronizer.setDeleteRemoteFiles(true);
		synchronizer.setRemoteDirectory("remote-test-dir");
		synchronizer.setFilter(new SftpRegexPatternFileListFilter(".*\\.test$"));
		synchronizer.setIntegrationEvaluationContext(ExpressionUtils.createStandardEvaluationContext());

		SftpInboundFileSynchronizingMessageSource ms =
				new SftpInboundFileSynchronizingMessageSource(synchronizer);
		ms.setAutoCreateLocalDirectory(true);
		ms.setLocalDirectory(localDirectoy);
		ms.afterPropertiesSet();
		Message<File> atestFile =  ms.receive();
		assertNotNull(atestFile);
		assertEquals("a.test", atestFile.getPayload().getName());
		Message<File> btestFile =  ms.receive();
		assertNotNull(btestFile);
		assertEquals("b.test", btestFile.getPayload().getName());
		Message<File> nothing =  ms.receive();
		assertNull(nothing);

		// two times because on the third receive (above) the internal queue will be empty, so it will attempt
		verify(synchronizer, times(2)).synchronizeToLocalDirectory(localDirectoy);

		assertTrue(new File("test/a.test").exists());
		assertTrue(new File("test/b.test").exists());
	}

	public static class TestSftpSessionFactory extends DefaultSftpSessionFactory {


		@Override
		public Session<LsEntry> getSession() {
			try {
				ChannelSftp channel = mock(ChannelSftp.class);

				String[] files = new File("remote-test-dir").list();
				Vector<LsEntry> sftpEntries = new Vector<LsEntry>();
				for (String fileName : files) {
					LsEntry lsEntry = mock(LsEntry.class);
					SftpATTRS attributes = mock(SftpATTRS.class);
					when(lsEntry.getAttrs()).thenReturn(attributes);
					when(lsEntry.getFilename()).thenReturn(fileName);
					sftpEntries.add(lsEntry);
					when(channel.get("remote-test-dir/"+fileName)).thenReturn(new FileInputStream("remote-test-dir/" + fileName));
				}
				when(channel.ls("remote-test-dir")).thenReturn(sftpEntries);

				when(jschSession.openChannel("sftp")).thenReturn(channel);
				return SftpTestSessionFactory.createSftpSession(jschSession);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create mock sftp session", e);
			}
		}
	}
}

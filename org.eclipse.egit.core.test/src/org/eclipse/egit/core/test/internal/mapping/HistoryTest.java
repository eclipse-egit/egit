/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.internal.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.junit.Before;
import org.junit.Test;

public class HistoryTest extends GitTestCase {

	protected static final PersonIdent jauthor;

	protected static final PersonIdent jcommitter;

	static {
		jauthor = new PersonIdent("J. Author", "jauthor@example.com",
				new Date(0L), TimeZone.getTimeZone("GMT+1"));
		jcommitter = new PersonIdent("J. Committer", "jcommitter@example.com",
				new Date(0L), TimeZone.getTimeZone("GMT+1"));
	}

	private File workDir;
	private Repository thisGit;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		// ensure we are working on an empty repository
		if (gitDir.exists())
			FileUtils.delete(gitDir, FileUtils.RECURSIVE | FileUtils.RETRY);
		thisGit = FileRepositoryBuilder.create(gitDir);
		workDir = thisGit.getWorkTree();
		thisGit.create();

		try (Git git = new Git(thisGit)) {
			createFile("Project-1/A.txt", "A.txt - first version\n");
			createFile("Project-1/B.txt", "B.txt - first version\n");
			git.add().addFilepattern("Project-1/A.txt")
					.addFilepattern("Project-1/B.txt").call();
			git.commit().setAuthor(jauthor).setCommitter(jcommitter)
					.setMessage("Foo\n\nMessage").call();

			createFile("Project-1/B.txt", "B.txt - second version\n");
			git.add().addFilepattern("Project-1/B.txt").call();
			git.commit().setAuthor(jauthor).setCommitter(jcommitter)
					.setMessage("Modified").call();
		}

		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.execute(null);
	}

	private File createFile(String name, String content) throws IOException {
		File f = new File(workDir, name);
		try (Writer fileWriter = new OutputStreamWriter(
				Files.newOutputStream(f.toPath()), "UTF-8")) {
			fileWriter.write(content);
		}
		return f;
	}

	@Test
	public void testSingleRevision_1() {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/A.txt"), IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, new NullProgressMonitor());
		IFileRevision fileRevision = fileHistory.getFileRevision("60f0d7917fe2aed5c92b5bc35dfb55b9b8ada359");
		assertEquals("60f0d7917fe2aed5c92b5bc35dfb55b9b8ada359", fileRevision.getContentIdentifier());
		assertEquals("J. Author",fileRevision.getAuthor());
	}

	@Test
	public void testSingleRevision_2() {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/A.txt"), IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, new NullProgressMonitor());
		IFileRevision fileRevision = fileHistory.getFileRevision("fd5a571c8a3a4a152e4c413d09d3ecb7d41e1e5f");
		assertNull(fileRevision); // not matched by getFileHistoryFor
	}

	@Test
	public void testSingleRevision_3() {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/B.txt"), IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, new NullProgressMonitor());
		IFileRevision fileRevision = fileHistory.getFileRevision("fd5a571c8a3a4a152e4c413d09d3ecb7d41e1e5f");
		assertEquals("fd5a571c8a3a4a152e4c413d09d3ecb7d41e1e5f", fileRevision.getContentIdentifier());
		assertEquals("J. Author",fileRevision.getAuthor());
	}

	@Test
	public void testIndexRevision() throws Exception {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/A.txt"), IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, new NullProgressMonitor());
		IFileRevision fileRevision = fileHistory.getFileRevision(GitFileRevision.INDEX);
		assertEquals(GitFileRevision.INDEX, fileRevision.getContentIdentifier());
		IStorage storage = fileRevision.getStorage(null);
		String content = testUtils.slurpAndClose(storage.getContents());
		assertEquals("A.txt - first version\n", content);
	}

	@Test
	public void testIndexRevisionSecondCommit() throws Exception {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/B.txt"), IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, new NullProgressMonitor());
		IFileRevision fileRevision = fileHistory.getFileRevision(GitFileRevision.INDEX);
		assertEquals(GitFileRevision.INDEX, fileRevision.getContentIdentifier());
		IStorage storage = fileRevision.getStorage(null);
		String content = testUtils.slurpAndClose(storage.getContents());
		assertEquals("B.txt - second version\n", content);
	}
	@Test
	public void testShallowHistory() {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/A.txt"), IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, new NullProgressMonitor());
		IFileRevision[] fileRevisions = fileHistory.getFileRevisions();
		assertEquals(1, fileRevisions.length);
		assertEquals("60f0d7917fe2aed5c92b5bc35dfb55b9b8ada359", fileRevisions[0].getContentIdentifier());
		assertEquals("J. Author",fileRevisions[0].getAuthor());
	}

	@Test
	public void testDeepHistory_A() {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/A.txt"), IFileHistoryProvider.NONE, new NullProgressMonitor());
		IFileRevision[] fileRevisions = fileHistory.getFileRevisions();
		assertEquals(1, fileRevisions.length);
		assertEquals("60f0d7917fe2aed5c92b5bc35dfb55b9b8ada359", fileRevisions[0].getContentIdentifier());
		assertEquals("J. Author",fileRevisions[0].getAuthor());
	}

	@Test
	public void testDeepHistory_B() {
		GitProvider provider = (GitProvider)RepositoryProvider.getProvider(project.project);
		assertNotNull(provider);
		IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
		IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(project.getProject().getWorkspace().getRoot().findMember("Project-1/B.txt"), IFileHistoryProvider.NONE, new NullProgressMonitor());
		IFileRevision[] fileRevisions = fileHistory.getFileRevisions();
		assertEquals(2, fileRevisions.length);
		assertEquals("fd5a571c8a3a4a152e4c413d09d3ecb7d41e1e5f", fileRevisions[0].getContentIdentifier());
		assertEquals("J. Author",fileRevisions[0].getAuthor());
		assertEquals("60f0d7917fe2aed5c92b5bc35dfb55b9b8ada359", fileRevisions[1].getContentIdentifier());
		assertEquals("J. Author",fileRevisions[0].getAuthor());
	}
}

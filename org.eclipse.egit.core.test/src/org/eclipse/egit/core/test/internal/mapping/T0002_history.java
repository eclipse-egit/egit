/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.internal.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileTreeEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.junit.Before;
import org.junit.Test;

public class T0002_history extends GitTestCase {

	protected static final PersonIdent jauthor;

	protected static final PersonIdent jcommitter;

	static {
		jauthor = new PersonIdent("J. Author", "jauthor@example.com");
		jcommitter = new PersonIdent("J. Committer", "jcommitter@example.com");
	}

	private File workDir;
	private File gitDir;
	private Repository thisGit;
	private Tree tree;
	private ObjectWriter objectWriter;

	@Before
	public void setUp() throws Exception {
		project.createSourceFolder();
		gitDir = new File(project.getProject().getWorkspace().getRoot()
				.getRawLocation().toFile(), Constants.DOT_GIT);
		thisGit = new Repository(gitDir);
		workDir = thisGit.getWorkDir();
		thisGit.create();
		objectWriter = new ObjectWriter(thisGit);

		tree = new Tree(thisGit);
		Tree projectTree = tree.addTree("Project-1");
		File project1_a_txt = createFile("Project-1/A.txt","A.txt - first version\n");
		addFile(projectTree,project1_a_txt);
		projectTree.setId(objectWriter.writeTree(projectTree));
		File project1_b_txt = createFile("Project-1/B.txt","B.txt - first version\n");
		addFile(projectTree,project1_b_txt);
		projectTree.setId(objectWriter.writeTree(projectTree));
		tree.setId(objectWriter.writeTree(tree));
		Commit commit = new Commit(thisGit);
		commit.setAuthor(new PersonIdent(jauthor, new Date(0L), TimeZone
				.getTimeZone("GMT+1")));
		commit.setCommitter(new PersonIdent(jcommitter, new Date(0L), TimeZone
				.getTimeZone("GMT+1")));
		commit.setMessage("Foo\n\nMessage");
		commit.setTree(tree);
		ObjectId commitId = objectWriter.writeCommit(commit);

		tree = new Tree(thisGit);
		projectTree = tree.addTree("Project-1");
		addFile(projectTree,project1_a_txt);

		File project1_b_v2_txt = createFile("Project-1/B.txt","B.txt - second version\n");
		addFile(projectTree,project1_b_v2_txt);
		projectTree.setId(objectWriter.writeTree(projectTree));
		tree.setId(objectWriter.writeTree(tree));
		commit = new Commit(thisGit);
		commit.setAuthor(new PersonIdent(jauthor, new Date(0L), TimeZone
				.getTimeZone("GMT+1")));
		commit.setCommitter(new PersonIdent(jcommitter, new Date(0L), TimeZone
				.getTimeZone("GMT+1")));
		commit.setMessage("Modified");
		commit.setParentIds(new ObjectId[] { commitId });
		commit.setTree(tree);
		commitId = objectWriter.writeCommit(commit);

		RefUpdate lck = thisGit.updateRef("refs/heads/master");
		assertNotNull("obtained lock", lck);
		lck.setNewObjectId(commitId);
		assertEquals(RefUpdate.Result.NEW, lck.forceUpdate());

		ConnectProviderOperation operation = new ConnectProviderOperation(
				project.getProject(), gitDir);
		operation.run(null);
	}

	private void addFile(Tree t,File f) throws IOException {
		ObjectId id = objectWriter.writeBlob(f);
		t.addEntry(new FileTreeEntry(t,id,f.getName().getBytes("UTF-8"),false));
	}

	private File createFile(String name, String content) throws IOException {
		File f = new File(workDir, name);
		FileWriter fileWriter = new FileWriter(f);
		fileWriter.write(content);
		fileWriter.close();
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

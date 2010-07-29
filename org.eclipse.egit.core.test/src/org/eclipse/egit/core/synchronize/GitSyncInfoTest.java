/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.eclipse.team.core.synchronize.SyncInfo.ADDITION;
import static org.eclipse.team.core.synchronize.SyncInfo.CHANGE;
import static org.eclipse.team.core.synchronize.SyncInfo.CONFLICTING;
import static org.eclipse.team.core.synchronize.SyncInfo.DELETION;
import static org.eclipse.team.core.synchronize.SyncInfo.INCOMING;
import static org.eclipse.team.core.synchronize.SyncInfo.IN_SYNC;
import static org.eclipse.team.core.synchronize.SyncInfo.OUTGOING;
import static org.eclipse.team.core.synchronize.SyncInfo.PSEUDO_CONFLICT;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitSyncInfoTest extends GitTestCase {

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	private GitResourceVariantComparator comparator;

	private final static String TEST_1 = Constants.R_HEADS + "test1";

	private final static String TEST_2 = Constants.R_HEADS + "test2";

	private final static String MASTER = Constants.R_HEADS + Constants.MASTER;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		comparator = new GitResourceVariantComparator(dataSet);
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	/**
	 * File is in sync when local, base and remote objects refer to identical
	 * file (same git ObjectId).
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnResourceFileInSync() throws Exception {
		// when
		String fileName = "test-file";
		byte[] localBytes = new byte[8200];
		Arrays.fill(localBytes, (byte) 'a');

		IFile local = createMock(IFile.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true).anyTimes();
		expect(local.getName()).andReturn(fileName).anyTimes();
		expect(local.getProject()).andReturn(iProject).anyTimes();
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localBytes));
		replay(local);

		File file = testRepo.createFile(iProject, fileName);
		RevCommit commit = testRepo.appendContentAndCommit(iProject, file,
				localBytes, "initial commit");

		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo, commit,
				path);

		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				commit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(IN_SYNC, gsi.getKind());
		verify(local);
	}

	/**
	 * Folders are in sync when they have same name.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnResourceFolderInSync() throws Exception {
		// when
		IResource local = createMock(IResource.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true);
		expect(local.getName()).andReturn("src").anyTimes();
		replay(local);

		File file = testRepo.createFile(iProject, "test" + File.separator
				+ ".keep");
		RevCommit commit = testRepo.addAndCommit(iProject, file,
				"initial commit");

		File fullPath = new File(file.getParent());
		String path = Repository.stripWorkDir(repo.getWorkTree(), fullPath);
		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				commit, path);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				commit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(IN_SYNC, gsi.getKind());
		verify(local);
	}

	/**
	 * Outgoing change should be returned when base RevCommitList has more
	 * commits than remote RevCommitList
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnOutgoingFileChange() throws Exception {
		// when
		String fileName = "test-file";
		byte[] localBytes = new byte[8200];
		Arrays.fill(localBytes, (byte) 'a');

		IFile local = createMock(IFile.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true).times(2);
		expect(local.getName()).andReturn(fileName).anyTimes();
		expect(local.getProject()).andReturn(iProject);
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localBytes));
		replay(local);

		File file = testRepo.createFile(iProject, fileName);
		RevCommit baseCommit = testRepo.appendContentAndCommit(iProject, file,
				localBytes, "intial commit");
		String repoRelativePath = Repository.stripWorkDir(repo.getWorkTree(),
				file);

		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit, repoRelativePath);

		testRepo.addToIndex(iProject, file);
		RevCommit remoteCommit = testRepo.commit("second commit");
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				remoteCommit, repoRelativePath);

		localBytes[8120] = 'b';

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(OUTGOING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * Should return incoming change when remote RevCommitList has more commit
	 * objects then base RevCommitList
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnIncomingFileChange() throws Exception {
		// when
		String fileName = "test-file";
		byte[] localBytes = new byte[8200];
		Arrays.fill(localBytes, (byte) 'a');

		IFile local = createMock(IFile.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true).times(2);
		expect(local.getName()).andReturn(fileName).anyTimes();
		expect(local.getProject()).andReturn(iProject);
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localBytes));
		replay(local);

		File file = testRepo.createFile(iProject, fileName);
		RevCommit baseCommit = testRepo.appendContentAndCommit(iProject, file,
				localBytes, "initial commit");
		String repoRelPath = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit, repoRelPath);

		RevCommit remoteCommit = testRepo.appendContentAndCommit(iProject,
				file, "ac", "first commit");
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				remoteCommit, repoRelPath);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(INCOMING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * Outgoing deletion should be returned when resource exist in base and
	 * remote but does not exist locally.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnOutgoingDeletion() throws Exception {
		// when
		String name = "Mian.java";
		IResource local = createMock(IResource.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(false);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		File file = testRepo.createFile(iProject, name);
		RevCommit commit = testRepo.appendContentAndCommit(iProject, file, "a",
				"initial commit");

		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo, commit,
				path);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				commit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(OUTGOING | DELETION, gsi.getKind());
		verify(local);
	}

	/**
	 * Incoming deletion should be returned when file exists locally and in base
	 * but does not exist in remote resource variant.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnIncomingFileDeletion() throws Exception {
		// when
		String fileName = "test-file";
		byte[] localBytes = new byte[8200];
		Arrays.fill(localBytes, (byte) 'a');

		IFile local = createMock(IFile.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true).times(2);
		expect(local.getName()).andReturn(fileName).anyTimes();
		expect(local.getProject()).andReturn(iProject);
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localBytes));
		replay(local);

		File file = testRepo.createFile(iProject, fileName);
		RevCommit baseCommit = testRepo.appendContentAndCommit(iProject, file,
				localBytes, "initial commit");
		String repoRelPath = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit, repoRelPath);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, null, comparator);
		gsi.init();

		// then
		assertEquals(INCOMING | DELETION, gsi.getKind());
		verify(local);
	}

	/**
	 * Outgoing addition should be returned when resource exists locally but it
	 * can't be found in base and remote resource variant.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnOutgoingAddition() throws Exception {
		// when
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true).times(2);
		expect(baseResource.isDerived()).andReturn(false);
		expect(baseResource.getName()).andReturn("Mian.java").anyTimes();
		replay(baseResource);

		// given
		GitSyncInfo gsi = new GitSyncInfo(baseResource, null, null, comparator);
		gsi.init();

		// then
		assertEquals(OUTGOING | ADDITION, gsi.getKind());
		verify(baseResource);
	}

	/**
	 * Conflicting change should be returned when remote and base RevCommitList
	 * have same number of RevCommit object's but these lists have one ore more
	 * different RevCommit objects.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingFileChange() throws Exception {
		// when
		String fileName = "test-file";
		byte[] localBytes = new byte[8200];
		Arrays.fill(localBytes, (byte) 'a');

		IFile local = createMock(IFile.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true).anyTimes();
		expect(local.getName()).andReturn(fileName).anyTimes();
		expect(local.getProject()).andReturn(iProject);
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localBytes));
		replay(local);

		testRepo.createInitialCommit("initial commit");
		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendContentAndCommit(iProject, file, localBytes,
				"initial commit");
		testRepo.createBranch(MASTER, TEST_1);
		testRepo.createBranch(MASTER, TEST_2);

		testRepo.checkoutBranch(TEST_1);
		RevCommit baseCommit = testRepo.appendContentAndCommit(iProject, file,
				"a", "first commit");
		String repoRelPath = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit, repoRelPath);

		testRepo.checkoutBranch(TEST_2);
		RevCommit remoteCommit = testRepo.appendContentAndCommit(iProject,
				file, "bc", "first commit");
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				remoteCommit, repoRelPath);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * Conflicting change should be returned when file was created locally with
	 * same name as incoming folder in remote.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingFileChange1() throws Exception {
		// when
		String name = "test-file3";
		IFile local = createMock(IFile.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true).times(1);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		testRepo.createInitialCommit("initial commit");
		testRepo.createBranch(MASTER, TEST_1);
		testRepo.createBranch(MASTER, TEST_2);
		testRepo.checkoutBranch(TEST_1);
		File file = testRepo.createFile(iProject, name);
		RevCommit fileCommit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				fileCommit, path);

		testRepo.checkoutBranch(TEST_2);
		File file2 = testRepo.createFile(iProject, name + File.separator
				+ "keep");
		RevCommit folderCommit = testRepo.addAndCommit(iProject, file2,
				"initial commit");
		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				folderCommit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * Conflicting change should be returned when local resource differ from
	 * base resource variant and remote variant does not exist.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingFileChange2() throws Exception {
		// when
		String fileName = "test-file";
		byte[] localBytes = new byte[8200];
		Arrays.fill(localBytes, (byte) 'a');

		IFile local = createMock(IFile.class);
		expect(local.isDerived()).andReturn(false);
		expect(local.exists()).andReturn(true).times(2);
		expect(local.getName()).andReturn(fileName).anyTimes();
		expect(local.getProject()).andReturn(iProject);
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localBytes));
		replay(local);

		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendFileContent(file, localBytes);
		testRepo.appendFileContent(file, "a");
		RevCommit baseCommit = testRepo.addAndCommit(iProject, file,
				"initial commit");

		String repoRelPath = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit, repoRelPath);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, null, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * Conflicting change when folder exists locally and remotely but it does
	 * not exist in base resource variant.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingFolderChange() throws Exception {
		// when
		String name = "test-folder";
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(true);
		expect(local.isDerived()).andReturn(false);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		RevCommit baseCommit = testRepo.createInitialCommit("initial commit");

		testRepo.createAndCheckoutBranch(MASTER, TEST_1);
		File keep = testRepo.createFile(iProject, name + File.separator
				+ "keep");
		RevCommit revCommit = testRepo.addAndCommit(iProject, keep,
				"second commit");

		File parent = new File(keep.getParent());
		String path = Repository.stripWorkDir(repo.getWorkTree(), parent);
		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				baseCommit, path);

		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				revCommit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * Conflicting change when folder exists in base but it does not exist in
	 * local and remote.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingFolderChange1() throws Exception {
		// when
		String name = "test-folder";
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(false);
		expect(local.isDerived()).andReturn(false);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		RevCommit baseCommit = testRepo.createInitialCommit("initial commit");

		testRepo.createAndCheckoutBranch(MASTER, TEST_1);
		File keep = testRepo.createFile(iProject, name + File.separator
				+ "keep");
		RevCommit revCommit = testRepo.addAndCommit(iProject, keep,
				"second commit");

		File parent = new File(keep.getParent());
		String path = Repository.stripWorkDir(repo.getWorkTree(), parent);
		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				revCommit, path);

		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				baseCommit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * When local resource is file, base resource variant is folder and remote
	 * variant is a file, then getKind() should return conflicting change.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingFolderAndFileChange() throws Exception {
		// when
		String name = "test";
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(false);
		expect(local.isDerived()).andReturn(false);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		testRepo.createInitialCommit("initial commit");
		testRepo.createBranch(MASTER, TEST_1);
		testRepo.createBranch(MASTER, TEST_2);
		testRepo.checkoutBranch(TEST_1);
		File file = testRepo.createFile(iProject, name + File.separator
				+ "keep");
		RevCommit baseCommit = testRepo.addAndCommit(iProject, file,
				"second commit");

		String path = Repository.stripWorkDir(repo.getWorkTree(),
				new File(file.getParent()));
		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				baseCommit, path);

		testRepo.checkoutBranch(TEST_2);
		File file2 = testRepo.createFile(iProject, name);
		RevCommit remoteCommit = testRepo.addAndCommit(iProject, file2,
				"second commit");
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				remoteCommit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * When remote is folder and base is a file getKind() should return
	 * conflicting change
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingFileAndFolderChange() throws Exception {
		// when
		String name = "test";
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(false);
		expect(local.isDerived()).andReturn(false);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		testRepo.createInitialCommit("initial commit");
		testRepo.createBranch(MASTER, TEST_1);
		testRepo.createBranch(MASTER, TEST_2);
		testRepo.checkoutBranch(TEST_1);
		File file = testRepo.createFile(iProject, name);
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		RevCommit baseCommit = testRepo.addAndCommit(iProject, file,
				"second commit");
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit, path);

		testRepo.checkoutBranch(TEST_2);
		File file2 = testRepo.createFile(iProject, name + File.separator
				+ "keep");
		RevCommit remoteCommit = testRepo.addAndCommit(iProject, file2,
				"second commit");
		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				remoteCommit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, remote, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | CHANGE, gsi.getKind());
		verify(local);
	}

	/**
	 * Remote resource variant was not found, local resource does not exist but
	 * there is a base resource. In such situation we should return conflicting
	 * deletion.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingDeletationPseudoConflict()
			throws Exception {
		// when
		String name = "Main.java";
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(false);
		expect(local.isDerived()).andReturn(false);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		File file = testRepo.createFile(iProject, name);
		RevCommit revCommit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				revCommit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, base, null, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | DELETION | PSEUDO_CONFLICT, gsi.getKind());
		verify(local);
	}

	/**
	 * Conflicting addition should be returned when resource exists in local and
	 * remote but cannot be found in base
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnConflictingAddition() throws Exception {
		// when
		String name = "Main.java";
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(true).times(3);
		expect(local.isDerived()).andReturn(false);
		expect(local.getName()).andReturn(name).anyTimes();
		replay(local);

		File file = testRepo.createFile(iProject, name);
		RevCommit revCommit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				revCommit, path);

		// given
		GitSyncInfo gsi = new GitSyncInfo(local, null, remote, comparator);
		gsi.init();

		// then
		assertEquals(CONFLICTING | ADDITION, gsi.getKind());
		verify(local);
	}

}

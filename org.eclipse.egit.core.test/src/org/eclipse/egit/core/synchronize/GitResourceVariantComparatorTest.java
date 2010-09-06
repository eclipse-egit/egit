/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.team.core.variants.IResourceVariant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitResourceVariantComparatorTest extends GitTestCase {

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		new Git(repo).commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initall commit").call();
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	/* ============================================
	 * compare(IResource, IResourceVariant) tests
	 * ============================================ */

	/**
	 * When remote variant wasn't found, compare method is called with null as
	 * second parameter. In this case compare should return false.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenRemoteDoesNotExist() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(false);
		replay(local);

		// then
		assertFalse(grvc.compare(local, null));
		verify(local);
	}

	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenRemoteDoesNotExist2() throws Exception{
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(false);
		replay(local);
		IResourceVariant remote = new GitFolderResourceVariant(repo,
				ObjectId.zeroId(), "./");

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local);
	}

	/**
	 * It is possible to have a local file that has same name as a remote
	 * folder. In some cases that two resources can be compared. In this case
	 * compare method should return false, because they aren't same resources
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenComparingFileAndContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		replay(local);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(true);
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote);
	}

	/**
	 * Comparing two folders that have different path should return false.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenComparingContainerAndContainer()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		IPath localPath = createMock(IPath.class);
		replay(localPath);
		IContainer local = createMock(IContainer.class);
		expect(local.exists()).andReturn(true).times(2);
		expect(local.getFullPath()).andReturn(localPath);
		replay(local);

		File file = testRepo.createFile(iProject, "test" + File.separator
				+ "keep");
		RevCommit commit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);

		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				commit.getTree(), path);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, localPath);
	}

	/**
	 * When comparing two folders that have same path, compare() method should
	 * return true.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnTrueWhenComparingContainerAndContainer()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		File file = testRepo.createFile(iProject, "test" + File.separator
				+ "keep");
		RevCommit commit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		IPath iPath = new Path(File.separator + path);

		IContainer local = createMock(IContainer.class);
		expect(local.exists()).andReturn(true).times(2);
		expect(local.getFullPath()).andReturn(iPath).anyTimes();
		replay(local);

		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				commit.getTree(), path);

		// then
		assertTrue(grvc.compare(local, remote));
		verify(local);
	}

	/**
	 * Compare() should return false when comparing two files with different
	 * content length
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenContentLengthIsDifferent()
			throws Exception {
		// when
		byte[] shortContent = "short content".getBytes();
		byte[] longContent = "very long long content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject()).anyTimes();
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(longContent));
		replay(local);

		IStorage storage = createMock(IStorage.class);
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(shortContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote, storage);
	}

	/**
	 * Comparing two files that have same content length but having small
	 * difference inside content should return false.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenShortContentIsDifferent() throws Exception {
		// when
		byte[] localContent = "very long long content".getBytes();
		// this typo should be here
		byte[] remoteContent = "very long lonk content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject());
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localContent));
		replay(local);

		IStorage storage = createMock(IStorage.class);
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(remoteContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote);
	}

	/**
	 * Comparing two 'large' files that have same length and almost identical
	 * content should return false.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenLongContentIsDifferent() throws Exception {
		// when
		byte[] localContent = new byte[8192];
		Arrays.fill(localContent, (byte) 'a');
		byte[] remoteContent = new byte[8192];
		Arrays.fill(remoteContent, (byte) 'a');
		remoteContent[8101] = 'b';
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject());
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localContent));
		replay(local);

		IStorage storage = createMock(IStorage.class);
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(remoteContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote);
	}

	/**
	 * Comparing two 'large' files that have different length but same content
	 * should return false.
	 * <p>
	 * This and previous three test cases cover almost the same functionality
	 * but they are covering all return points in compare methods that can be
	 * used when comparing files content
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenLongContentLengthIsDifferent()
			throws Exception {
		// when
		byte[] localContent = new byte[8192];
		Arrays.fill(localContent, (byte) 'a');
		byte[] remoteContent = new byte[8200];
		Arrays.fill(remoteContent, (byte) 'a');
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject());
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localContent));
		replay(local);

		IStorage storage = createMock(IStorage.class);
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(remoteContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote, storage);
	}

	/**
	 * Comparing two files that have the same content and content length should
	 * return true
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnTrueWhenShortContentIsDifferent() throws Exception {
		// when
		byte[] localContent = "very long long content".getBytes();
		byte[] remoteContent = "very long long content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject());
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localContent));
		replay(local);

		IStorage storage = createMock(IStorage.class);
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(remoteContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertTrue(grvc.compare(local, remote));
		verify(local, remote);
	}

	/**
	 * Compare two 'large' files that have same content length and content
	 * should return true.
	 *
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnTrueWhenLongContentLengthIsDifferent()
			throws Exception {
		// when
		byte[] localContent = new byte[8192];
		Arrays.fill(localContent, (byte) 'a');
		byte[] remoteContent = new byte[8192];
		Arrays.fill(remoteContent, (byte) 'a');
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject());
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localContent));
		replay(local);

		IStorage storage = createMock(IStorage.class);
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(remoteContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertTrue(grvc.compare(local, remote));
		verify(local, remote, storage);
	}

	/* ==================================================
	 * compare(IResourceVariant, IResourceVariant) tests
	 * ================================================== */

	/**
	 * When comparing file that don't exist in base, but exists in remote
	 * compare method should return false.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnFalseWhenBaseDoesntExist() throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		RevCommit baseCommit = testRepo.createInitialCommit("initial commit");
		testRepo.createAndCheckoutBranch(Constants.HEAD, Constants.R_HEADS
				+ "test");
		File file = testRepo.createFile(iProject, "test-file");
		RevCommit remoteCommit = testRepo.addAndCommit(iProject, file,
				"second commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);

		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit.getTree(), path);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				remoteCommit.getTree(), path);

		// then
		assertFalse(grvc.compare(base, remote));
	}

	/**
	 * Compare() should return false when remote file does not exists, but
	 * equivalent local file exist.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnFalseWhenRemoteVariantDoesntExist()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		RevCommit remoteCommit = testRepo.createInitialCommit("initial commit");
		testRepo.createAndCheckoutBranch(Constants.HEAD, Constants.R_HEADS
				+ "test");
		File file = testRepo.createFile(iProject, "test-file");
		RevCommit baseCommit = testRepo.addAndCommit(iProject, file,
				"second commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);

		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit.getTree(), path);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				remoteCommit.getTree(), path);

		// then
		assertFalse(grvc.compare(base, remote));
	}

	/**
	 * Return false when comparing incompatible types (file against folder) that
	 * also maps onto different resources
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnFalseWhenComparingRemoteVariantFileWithContainer()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		File file = testRepo.createFile(iProject, "test" + File.separator
				+ "keep");
		RevCommit commit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String filePath = Repository.stripWorkDir(repo.getWorkTree(), file);
		String folderPath = Repository.stripWorkDir(repo.getWorkTree(),
				new File(file.getParent()));
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				commit.getTree(), filePath);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				commit.getTree(), folderPath);

		// then
		assertFalse(grvc.compare(base, remote));
	}

	/**
	 * Return false when comparing incompatible types (folder against file) that
	 * also map onto different resources
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnFalseWhenComparingRemoteVariantContainerWithFile()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		File file = testRepo.createFile(iProject, "test" + File.separator
				+ "keep");
		RevCommit commit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String filePath = Repository.stripWorkDir(repo.getWorkTree(), file);
		String folderPath = Repository.stripWorkDir(repo.getWorkTree(),
				new File(file.getParent()));

		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				commit.getTree(), folderPath);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				commit.getTree(), filePath);

		// then
		assertFalse(grvc.compare(base, remote));
	}

	/**
	 * When comparing two remote variants that have different path compare
	 * method should return false
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnFalseWhenComparingRemoteVariantContainerWithContainer()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		File file1 = testRepo.createFile(iProject, "test1" + File.separator
				+ "keep1");
		File file2 = testRepo.createFile(iProject, "test2" + File.separator
				+ "keep2");
		testRepo.track(file1);
		testRepo.track(file2);
		testRepo.addToIndex(testRepo.getIFile(iProject, file1));
		testRepo.addToIndex(testRepo.getIFile(iProject, file2));
		RevCommit commit = testRepo.commit("initial commit");

		TreeWalk tw = new TreeWalk(repo);
		int nth = tw.addTree(commit.getTree());

		tw.next();
		tw.enterSubtree(); // enter project node
		tw.next();
		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				tw.getObjectId(nth), tw.getNameString());

		tw.next();
		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				tw.getObjectId(nth), tw.getNameString());

		// then
		assertFalse(grvc.compare(base, remote));
	}

	/**
	 * Comparing two remote folders that have same path should return true
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnTrueWhenComparingRemoteVariantContainerWithContainer()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		File file1 = testRepo.createFile(iProject, "test1" + File.separator
				+ "keep1");
		testRepo.track(file1);
		testRepo.addToIndex(testRepo.getIFile(iProject, file1));
		RevCommit commit = testRepo.commit("initial commit");

		String path1 = Repository.stripWorkDir(repo.getWorkTree(), new File(
				file1.getParent()));

		GitFolderResourceVariant base = new GitFolderResourceVariant(repo,
				commit.getTree(), path1);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(repo,
				commit.getTree(), path1);

		// then
		assertTrue(grvc.compare(base, remote));
	}

	@Test
	/**
	 * Comparing two remote files that have different git ObjectId should return false.
	 *
	 * @throws Exception
	 */
	public void shouldReturnFalseWhenComparingRemoteVariantWithDifferentObjectId()
			throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		File file = testRepo.createFile(iProject, "test-file");
		RevCommit baseCommit = testRepo.appendContentAndCommit(iProject, file,
				"a", "initial commit");
		RevCommit remoteCommit = testRepo.appendContentAndCommit(iProject,
				file, "bc", "second commit");

		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				baseCommit.getTree(), path);

		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				remoteCommit.getTree(), path);

		// then
		assertFalse(grvc.compare(base, remote));
	}

	/**
	 * Comparing two remote files that have the same git ObjectId should return
	 * true.
	 * @throws Exception
	 */
	@Test
	public void shouldReturnTrueWhenComparingRemoteVariant() throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		File file = testRepo.createFile(iProject, "test-file");
		RevCommit commit = testRepo.appendContentAndCommit(iProject, file,
				"a", "initial commit");

		String path = Repository.stripWorkDir(repo.getWorkTree(), file);
		GitBlobResourceVariant base = new GitBlobResourceVariant(repo,
				commit.getTree(), path);

		GitBlobResourceVariant remote = new GitBlobResourceVariant(repo,
				commit.getTree(), path);

		// then
		assertTrue(grvc.compare(base, remote));
	}
}

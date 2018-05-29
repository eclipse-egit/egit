/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		try (Git git = new Git(repo)) {
			git.commit().setAuthor("JUnit", "junit@jgit.org")
					.setMessage("Initial commit").call();
		}
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
		IResource local = mock(IResource.class);
		when(local.exists()).thenReturn(false);

		// then
		assertFalse(grvc.compare(local, null));
	}

	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenRemoteDoesNotExist2() throws Exception {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null);

		// given
		IResource local = mock(IResource.class);
		when(local.exists()).thenReturn(false);
		IResourceVariant remote = new GitRemoteFolder(repo, null, null, null, "./");

		// then
		assertFalse(grvc.compare(local, remote));
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
		IFile local = mock(IFile.class);
		when(local.exists()).thenReturn(true);

		IResourceVariant remote = mock(IResourceVariant.class);
		when(remote.isContainer()).thenReturn(true);

		// then
		assertFalse(grvc.compare(local, remote));
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
		IPath localPath = mock(IPath.class);
		IContainer local = mock(IContainer.class);
		when(local.exists()).thenReturn(true);
		when(local.getLocation()).thenReturn(localPath);

		File file = testRepo.createFile(iProject, "test" + File.separator
				+ "keep");
		RevCommit commit = testRepo.addAndCommit(iProject, file,
				"initial commit");
		String path = Repository.stripWorkDir(repo.getWorkTree(), file);

		GitRemoteFolder remote = new GitRemoteFolder(repo, null, commit,
				commit.getTree(), path);

		// then
		assertFalse(grvc.compare(local, remote));
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
		IPath iPath = new Path(path);

		IContainer local = mock(IContainer.class);
		when(local.exists()).thenReturn(true);
		when(local.getLocation()).thenReturn(iPath);

		GitRemoteFolder remote = new GitRemoteFolder(repo, null, commit,
				commit.getTree(), path);

		// then
		assertTrue(grvc.compare(local, remote));
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
		byte[] shortContent = "short content".getBytes("UTF-8");
		byte[] longContent = "very long long content".getBytes("UTF-8");
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = mock(IFile.class);
		when(local.exists()).thenReturn(true);
		when(local.getProject()).thenReturn(project.getProject());
		when(local.getContents()).thenReturn(
				new ByteArrayInputStream(longContent));

		IStorage storage = mock(IStorage.class);
		when(storage.getContents()).thenReturn(
				new ByteArrayInputStream(shortContent));

		IResourceVariant remote = mock(IResourceVariant.class);
		when(remote.isContainer()).thenReturn(false);
		when(remote.getStorage(any(IProgressMonitor.class))).thenReturn(
				storage);

		// then
		assertFalse(grvc.compare(local, remote));
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
		byte[] localContent = "very long long content".getBytes("UTF-8");
		// this typo should be here
		byte[] remoteContent = "very long lonk content".getBytes("UTF-8");
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = mock(IFile.class);
		when(local.exists()).thenReturn(true);
		when(local.getProject()).thenReturn(project.getProject());
		when(local.getContents()).thenReturn(
				new ByteArrayInputStream(localContent));

		IStorage storage = mock(IStorage.class);
		when(storage.getContents()).thenReturn(
				new ByteArrayInputStream(remoteContent));

		IResourceVariant remote = mock(IResourceVariant.class);
		when(remote.isContainer()).thenReturn(false);
		when(remote.getStorage(any(IProgressMonitor.class))).thenReturn(
				storage);

		// then
		assertFalse(grvc.compare(local, remote));
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
		IFile local = mock(IFile.class);
		when(local.exists()).thenReturn(true);
		when(local.getProject()).thenReturn(project.getProject());
		when(local.getContents()).thenReturn(
				new ByteArrayInputStream(localContent));

		IStorage storage = mock(IStorage.class);
		when(storage.getContents()).thenReturn(
				new ByteArrayInputStream(remoteContent));

		IResourceVariant remote = mock(IResourceVariant.class);
		when(remote.isContainer()).thenReturn(false);
		when(remote.getStorage(any(IProgressMonitor.class))).thenReturn(
				storage);

		// then
		assertFalse(grvc.compare(local, remote));
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
		IFile local = mock(IFile.class);
		when(local.exists()).thenReturn(true);
		when(local.getProject()).thenReturn(project.getProject());
		when(local.getContents()).thenReturn(
				new ByteArrayInputStream(localContent));

		IStorage storage = mock(IStorage.class);
		when(storage.getContents()).thenReturn(
				new ByteArrayInputStream(remoteContent));

		IResourceVariant remote = mock(IResourceVariant.class);
		when(remote.isContainer()).thenReturn(false);
		when(remote.getStorage(any(IProgressMonitor.class))).thenReturn(
				storage);

		// then
		assertFalse(grvc.compare(local, remote));
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
		byte[] localContent = "very long long content".getBytes("UTF-8");
		byte[] remoteContent = "very long long content".getBytes("UTF-8");
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet);

		// given
		IFile local = mock(IFile.class);
		when(local.exists()).thenReturn(true);
		when(local.getProject()).thenReturn(project.getProject());
		when(local.getContents()).thenReturn(
				new ByteArrayInputStream(localContent));

		IStorage storage = mock(IStorage.class);
		when(storage.getContents()).thenReturn(
				new ByteArrayInputStream(remoteContent));

		IResourceVariant remote = mock(IResourceVariant.class);
		when(remote.isContainer()).thenReturn(false);
		when(remote.getStorage(any(IProgressMonitor.class))).thenReturn(
				storage);

		// then
		assertTrue(grvc.compare(local, remote));
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
		IFile local = mock(IFile.class);
		when(local.exists()).thenReturn(true);
		when(local.getProject()).thenReturn(project.getProject());
		when(local.getContents()).thenReturn(
				new ByteArrayInputStream(localContent));

		IStorage storage = mock(IStorage.class);
		when(storage.getContents()).thenReturn(
				new ByteArrayInputStream(remoteContent));

		IResourceVariant remote = mock(IResourceVariant.class);
		when(remote.isContainer()).thenReturn(false);
		when(remote.getStorage(any(IProgressMonitor.class))).thenReturn(
				storage);

		// then
		assertTrue(grvc.compare(local, remote));
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

		GitRemoteFile base = new GitRemoteFile(repo, baseCommit,
				baseCommit.getTree(), path, null);
		GitRemoteFile remote = new GitRemoteFile(repo, baseCommit,
				remoteCommit.getTree(), path, null);

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

		GitRemoteFile base = new GitRemoteFile(repo, baseCommit,
				baseCommit.getTree(), path, null);
		GitRemoteFile remote = new GitRemoteFile(repo, remoteCommit,
				remoteCommit.getTree(), path, null);

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
		GitRemoteFile base = new GitRemoteFile(repo, commit, commit.getTree(),
				filePath, null);
		GitRemoteFolder remote = new GitRemoteFolder(repo, null, commit,
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

		GitRemoteFolder base = new GitRemoteFolder(repo, null, commit,
				commit.getTree(), folderPath);
		GitRemoteFile remote = new GitRemoteFile(repo, commit,
				commit.getTree(), filePath, null);

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

		try (TreeWalk tw = new TreeWalk(repo)) {
			int nth = tw.addTree(commit.getTree());

			tw.next();
			tw.enterSubtree(); // enter project node
			tw.next();
			GitRemoteFolder base = new GitRemoteFolder(repo, null, commit,
					tw.getObjectId(nth), tw.getNameString());

			tw.next();
			GitRemoteFolder remote = new GitRemoteFolder(repo, null, commit,
					tw.getObjectId(nth), tw.getNameString());

			// then
			assertFalse(grvc.compare(base, remote));
		}
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

		GitRemoteFolder base = new GitRemoteFolder(repo, null, commit,
				commit.getTree(), path1);
		GitRemoteFolder remote = new GitRemoteFolder(repo, null, commit,
				commit.getTree(), path1);

		// then
		assertTrue(grvc.compare(base, remote));
	}

	/**
	 * Comparing two remote files that have different git ObjectId should return false.
	 *
	 * @throws Exception
	 */
	@Test
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
		GitRemoteFile base = new GitRemoteFile(repo, baseCommit,
				baseCommit.getTree(), path, null);

		GitRemoteFile remote = new GitRemoteFile(repo, remoteCommit,
				remoteCommit.getTree(), path, null);

		// then
		assertFalse(grvc.compare(base, remote));
	}

	/**
	 * Comparing two remote files that have the same git ObjectId should return
	 * true.
	 *
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
		GitRemoteFile base = new GitRemoteFile(repo, commit, commit.getTree(),
				path, null);

		GitRemoteFile remote = new GitRemoteFile(repo, commit,
				commit.getTree(), path, null);

		// then
		assertTrue(grvc.compare(base, remote));
	}
}

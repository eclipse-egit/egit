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

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.team.core.variants.IResourceVariant;
import org.junit.Before;
import org.junit.Test;

public class GitResourceVariantComparatorTest extends GitTestCase {

	private Repository repo;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		IProject iProject = project.project;
		if (!gitDir.exists())
			new FileRepository(gitDir).create();

		new ConnectProviderOperation(iProject, gitDir).execute(null);
		repo = RepositoryMapping.getMapping(iProject).getRepository();
	}

	/*============================================
	 * compare(IResource, IResourceVariant) tests
	 *============================================*/

	/**
	 * When remote variant wasn't found, compare method is called with null as
	 * second parameter. In this case compare should return false.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenRemoteDoesNotExist() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IResource local = createMock(IResource.class);
		expect(local.exists()).andReturn(false);
		replay(local);

		// then
		assertFalse(grvc.compare(local, null));
		verify(local);
	}

	/**
	 * It is possible to have a local file that has same name as a remote folder.
	 * In some cases that two resources can be compared. In this case compare
	 * method should return false, because they aren't same resources
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenComparingFileAndContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

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
	 *  Comparing two folders that have different path should return false.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenComparingContainerAndContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IPath localPath = createMock(IPath.class);
		replay(localPath);
		IContainer local = createMock(IContainer.class);
		expect(local.exists()).andReturn(true);
		expect(local.getFullPath()).andReturn(localPath);
		replay(local);

		IPath remotePath = createMock(IPath.class);
		replay(remotePath);
		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.getFullPath()).andReturn(remotePath);
		replay(remoteResource);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(
				remoteResource);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, localPath, remotePath, remoteResource);
	}

	/**
	 * When comparing two folders that have same path, compare() method should
	 * return true.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnTrueWhenComparingContainerAndContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IPath path = createMock(IPath.class);
		replay(path);

		IContainer local = createMock(IContainer.class);
		expect(local.exists()).andReturn(true);
		expect(local.getFullPath()).andReturn(path);
		replay(local);

		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.getFullPath()).andReturn(path);
		replay(remoteResource);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(
				remoteResource);

		// then
		assertTrue(grvc.compare(local, remote));
		verify(local, path, remoteResource);
	}

	/**
	 * Compare() should return false when comparing two files with different
	 * content length
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenContentLengthIsDifferent()
			throws Exception {
		// when
		byte[] shortContent = "short content".getBytes();
		byte[] longContent = "very long long content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

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
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenShortContentIsDifferent() throws Exception {
		// when
		byte[] localContent = "very long long content".getBytes();
		// this typo should be here
		byte[] remoteContent = "very long lonk content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

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
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

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
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

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
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnTrueWhenShortContentIsDifferent() throws Exception {
		// when
		byte[] localContent = "very long long content".getBytes();
		byte[] remoteContent = "very long long content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

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
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

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

	/**
	 * When comparing locally not existing file with file that exists in remote,
	 * compare method should return false.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenBaseDoesntExist() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(false);
		replay(baseResource);
		GitBlobResourceVariant base = new GitBlobResourceVariant(baseResource,
				repo, ObjectId.zeroId(), null);
		IResource remoteResource = createMock(IResource.class);
		replay(remoteResource);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(
				remoteResource, repo, ObjectId.zeroId(), null);

		// then
		assertFalse(grvc.compare(base, remote));
		verify(baseResource, remoteResource);
	}

	/**
	 * Compare() should return false when remote file does not exists, but
	 * equivalent local file exist.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenRemoteVariantDoesntExist() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true);
		replay(baseResource);
		GitBlobResourceVariant base = new GitBlobResourceVariant(baseResource,
				repo, ObjectId.zeroId(), null);
		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.exists()).andReturn(false);
		replay(remoteResource);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(
				remoteResource, repo, ObjectId.zeroId(), null);

		// then
		assertFalse(grvc.compare(base, remote));
		verify(baseResource, remoteResource);
	}

	/*==================================================
	 * compare(IResourceVariant, IResourceVariant) tests
	 *==================================================*/

	/**
	 * Return false when comparing incompatible types (file against folder) that
	 * also maps onto different resources
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenComparingRemoteVariantFileWithContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true);
		replay(baseResource);
		GitBlobResourceVariant base = new GitBlobResourceVariant(baseResource,
				repo, ObjectId.zeroId(), null);
		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.exists()).andReturn(true);
		replay(remoteResource);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(
				remoteResource);

		// then
		assertFalse(grvc.compare(base, remote));
		verify(baseResource, remoteResource);
	}

	/**
	 * Return false when comparing incompatible types (folder against file) that
	 * also map onto different resources
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenComparingRemoteVariantContainerWithFile() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true);
		replay(baseResource);
		GitFolderResourceVariant base = new GitFolderResourceVariant(
				baseResource);
		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.exists()).andReturn(true);
		replay(remoteResource);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(
				remoteResource, repo, ObjectId.zeroId(), null);

		// then
		assertFalse(grvc.compare(base, remote));
		verify(baseResource, remoteResource);
	}

	/**
	 * When comparing two remote variants that have different path compare
	 * method should return false
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnFalseWhenComparingRemoteVariantContainerWithContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IPath basePath = createMock(IPath.class);
		replay(basePath);
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true);
		expect(baseResource.getFullPath()).andReturn(basePath);
		replay(baseResource);
		GitFolderResourceVariant base = new GitFolderResourceVariant(
				baseResource);

		IPath remotePath = createMock(IPath.class);
		replay(remotePath);
		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.exists()).andReturn(true);
		expect(remoteResource.getFullPath()).andReturn(remotePath);
		replay(remoteResource);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(
				remoteResource);

		// then
		assertFalse(grvc.compare(base, remote));
		verify(baseResource, remoteResource, basePath, remotePath);
	}

	/**
	 * Comparing two remote folders that have same path should return true
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnTrueWhenComparingRemoteVariantContainerWithContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IPath path = createMock(IPath.class);
		replay(path);

		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true);
		expect(baseResource.getFullPath()).andReturn(path);
		replay(baseResource);
		GitFolderResourceVariant base = new GitFolderResourceVariant(
				baseResource);

		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.exists()).andReturn(true);
		expect(remoteResource.getFullPath()).andReturn(path);
		replay(remoteResource);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(
				remoteResource);

		// then
		assertTrue(grvc.compare(base, remote));
		verify(baseResource, remoteResource, path);
	}

	@Test
	@SuppressWarnings("boxing")
	/**
	 * Comparing two remote files that have different git ObjectId should return false.
	 */
	public void shouldReturnFalseWhenComparingRemoteVariantWithDifferentObjectId() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true);
		replay(baseResource);
		GitBlobResourceVariant base = new GitBlobResourceVariant(
				baseResource,
				repo,
				ObjectId.fromString("0123456789012345678901234567890123456789"),
				null);

		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.exists()).andReturn(true);
		replay(remoteResource);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(
				remoteResource, repo, ObjectId.zeroId(), null);

		// then
		assertFalse(grvc.compare(base, remote));
		verify(baseResource, remoteResource);
	}

	/**
	 * Comparing two remote files that have the same git ObjectId should return
	 * true.
	 */
	@Test
	@SuppressWarnings("boxing")
	public void shouldReturnTrueWhenComparingRemoteVariant() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IResource baseResource = createMock(IResource.class);
		expect(baseResource.exists()).andReturn(true);
		replay(baseResource);
		GitBlobResourceVariant base = new GitBlobResourceVariant(
				baseResource,
				repo,
				ObjectId.fromString("0123456789012345678901234567890123456789"),
				null);

		IResource remoteResource = createMock(IResource.class);
		expect(remoteResource.exists()).andReturn(true);
		replay(remoteResource);
		GitBlobResourceVariant remote = new GitBlobResourceVariant(
				remoteResource,
				repo,
				ObjectId.fromString("0123456789012345678901234567890123456789"),
				null);

		// then
		assertTrue(grvc.compare(base, remote));
		verify(baseResource, remoteResource);
	}
}

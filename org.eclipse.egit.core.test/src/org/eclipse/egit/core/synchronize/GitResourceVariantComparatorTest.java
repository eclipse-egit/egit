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
			new Repository(gitDir).create();

		new ConnectProviderOperation(iProject, gitDir).execute(null);
		repo = RepositoryMapping.getMapping(iProject).getRepository();
	}

	@Test
	@SuppressWarnings("boxing")
	// compare should return false because remote resource was not found
	public void shouldReturnFalseWhenRemoteDoesNotExist() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		// record call sequence for mocked objects
		IResource local = createMock(IResource.class);
		// we are expecting that method exists will be executed only once
		// and it will return false
		expect(local.exists()).andReturn(false);
		// save record
		replay(local);

		// then
		assertFalse(grvc.compare(local, null));
		// verify that our recorded scenario matches with real execution
		verify(local);
	}

	@Test
	@SuppressWarnings("boxing")
	// compare should return false because local resource is a file and remote
	// is a container
	public void shouldReturnFalseWhenComparingFileAndContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		IFile local = createMock(IFile.class);
		// mock that local.exists() will be used only once and it will return
		// true value
		expect(local.exists()).andReturn(true);
		replay(local);

		IResourceVariant remote = createMock(IResourceVariant.class);
		// mock that remote.isContainer() will be used only once and it will
		// return true value
		expect(remote.isContainer()).andReturn(true);
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote);
	}

	@Test
	@SuppressWarnings("boxing")
	// should return false because we are comparing two different containers
	// (they have different path)
	public void shouldReturnFalseWhenComparingContainerAndContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		// create mocked instance of IPath that will be used for local resource
		IPath localPath = createMock(IPath.class);
		replay(localPath);
		IContainer local = createMock(IContainer.class);
		expect(local.exists()).andReturn(true);
		// mock that local.getFullPatch will be used only once and will return
		// value of previous mocked localPath object
		expect(local.getFullPath()).andReturn(localPath);
		replay(local);

		// create mocked instance of IPath that will be used for remote
		// resource. it is important to remember that localPath != remotePath
		IPath remotePath = createMock(IPath.class);
		replay(remotePath);
		IResource remoteResource = createMock(IResource.class);
		// mock that remoteResource.getFullPatch() will be used only once and it
		// will return previous mocked remotePath object
		expect(remoteResource.getFullPath()).andReturn(remotePath);
		replay(remoteResource);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(
				remoteResource);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, localPath, remotePath, remoteResource);
	}

	@Test
	@SuppressWarnings("boxing")
	// here we are comparing two containers that have the same path
	public void shouldReturnTrueWhenComparingContainerAndContainer() {
		// when
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				null, null);

		// given
		// mock IPath object that will be returned by local and remoteResource
		// getFullPath() method
		IPath path = createMock(IPath.class);
		replay(path);

		IContainer local = createMock(IContainer.class);
		// local.exists() will return true value
		expect(local.exists()).andReturn(true);
		// local.getFullPath() will return previous mocked path value
		expect(local.getFullPath()).andReturn(path);
		replay(local);

		IResource remoteResource = createMock(IResource.class);
		// remoteResource.getFullPatch() will return same value as
		// local.getFullPath()
		expect(remoteResource.getFullPath()).andReturn(path);
		replay(remoteResource);
		GitFolderResourceVariant remote = new GitFolderResourceVariant(
				remoteResource);

		// then
		assertTrue(grvc.compare(local, remote));
		verify(local, path, remoteResource);
	}

	@Test
	@SuppressWarnings("boxing")
	// here we are comparing length of file's content
	public void shouldReturnFalseWhenContentLengthIsDifferent()
			throws Exception {
		// when
		// value of content for remote file
		byte[] shortContent = "short content".getBytes();
		// value of content for local file
		byte[] longContent = "very long long content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject()).anyTimes();
		// local.getContents() will return instance of ByteArrayInputStream that
		// will contain previously declared longContent data
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(longContent));
		replay(local);

		// mock IStorage object for remote resource
		IStorage storage = createMock(IStorage.class);
		// sotoreage.getContents() will return instance of ByteArrayInputStream
		// that will contain previously declared shorContent value
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(shortContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		// remote.getStorage for any instance of IProgressMonitor will return
		// previous mocked storage instance
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote, storage);
	}

	@Test
	@SuppressWarnings("boxing")
	// compare two file with same content length but with some difference inside
	// content
	public void shouldReturnFalseWhenShortContentIsDifferent() throws Exception {
		// when
		// declare content for both, local and remote, resource; this typo is
		// deliberately made.
		byte[] localContent = "very long long content".getBytes();
		byte[] remoteContent = "very long lonk content".getBytes();
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject());
		// return InputStream instance with localContent when calling
		// local.getContents()
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localContent));
		replay(local);

		// mock IStorage instance for remote resource
		IStorage storage = createMock(IStorage.class);
		// return InputStream instance with remoteContent when calling
		// storage.getContents()
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(remoteContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		// return previous mocked IStorage instance for any calling of
		// remote.getStorate()
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote);
	}

	@Test
	@SuppressWarnings("boxing")
	// compare two large file contents
	public void shouldReturnFalseWhenLongContentIsDifferent() throws Exception {
		// when
		// create byte array that will be used for simulating local file content
		byte[] localContent = new byte[8192];
		Arrays.fill(localContent, (byte) 'a');
		// create byte array that will be used for simulating remote file
		// content
		byte[] remoteContent = new byte[8192];
		Arrays.fill(remoteContent, (byte) 'a');
		// change single letter in remote content
		remoteContent[8101] = 'b';
		GitSynchronizeData data = new GitSynchronizeData(repo, "", "", true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitResourceVariantComparator grvc = new GitResourceVariantComparator(
				dataSet, null);

		// given
		IFile local = createMock(IFile.class);
		expect(local.exists()).andReturn(true);
		expect(local.getProject()).andReturn(project.getProject());
		// instance of InputStream with localContent value will be returned when
		// local.getContents() method will be called
		expect(local.getContents()).andReturn(
				new ByteArrayInputStream(localContent));
		replay(local);

		IStorage storage = createMock(IStorage.class);
		// instance of InputStream with remoteContent value will be returned
		// with storage.getContents() will be called
		expect(storage.getContents()).andReturn(
				new ByteArrayInputStream(remoteContent));
		replay(storage);

		IResourceVariant remote = createMock(IResourceVariant.class);
		expect(remote.isContainer()).andReturn(false);
		// return previously mocked IStorage instance when remote.getStorage
		// will be called
		expect(remote.getStorage((IProgressMonitor) anyObject())).andReturn(
				storage).anyTimes();
		replay(remote);

		// then
		assertFalse(grvc.compare(local, remote));
		verify(local, remote);
	}

	@Test
	@SuppressWarnings("boxing")
	// compare two 'large' files with different length
	public void shouldReturnFalseWhenLongContentLengthIsDifferent()
			throws Exception {
		// when
		byte[] localContent = new byte[8192];
		Arrays.fill(localContent, (byte) 'a');
		// remote file content will be 8 bytes longer then local
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

	@Test
	@SuppressWarnings("boxing")
	// because base.exists() return false and remote != null compare should
	// return false
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

	@Test
	@SuppressWarnings("boxing")
	// baseResource.exists() will return true, but remoteResours.exits() will
	// return false; because that compare should return false
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

	@Test
	@SuppressWarnings("boxing")
	// compare two IResourceVariant's
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

	@Test
	@SuppressWarnings("boxing")
	// compare two IResourceVariant's
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

	@Test
	@SuppressWarnings("boxing")
	// compare two GitFolderResourceVariant that have different path value
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

	@Test
	@SuppressWarnings("boxing")
	// comapre two GitFolderResourceVariant that have same path value
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
	// compare two GitBlobResourceVariant's that have different ObjectId
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

	@Test
	@SuppressWarnings("boxing")
	// compare two GitBlobResourceVariant's that have same ObjecId
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

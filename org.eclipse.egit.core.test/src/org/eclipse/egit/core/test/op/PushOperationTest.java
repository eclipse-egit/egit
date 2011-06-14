/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PushOperationTest extends DualRepositoryTestCase {

	File workdir;

	File workdir2;

	String projectName = "PushTest";

	/**
	 * Set up repository1 with branch "master", create some project and commit
	 * it; then clone into repository2; finally create a branch "test" on top of
	 * "master" in repository2
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		workdir = testUtils.createTempDir("Repository1");
		workdir2 = testUtils.createTempDir("Repository2");

		repository1 = new TestRepository(new File(workdir, Constants.DOT_GIT));

		// now we create a project in repo1
		IProject project = testUtils.createProjectInLocalFileSystem(workdir,
				projectName);
		testUtils.addFileToProject(project, "folder1/file1.txt", "Hello world");

		repository1.connect(project);

		project.accept(new IResourceVisitor() {

			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile) {
					try {
						repository1
								.track(EFS.getStore(resource.getLocationURI())
										.toLocalFile(0, null));
					} catch (IOException e) {
						throw new CoreException(Activator.error(e.getMessage(),
								e));
					}
				}
				return true;
			}
		});

		repository1.commit("Initial commit");

		// let's get rid of the project
		project.delete(false, false, null);

		// let's clone repository1 to repository2
		URIish uri = new URIish("file:///"
				+ repository1.getRepository().getDirectory().toString());
		CloneOperation clop = new CloneOperation(uri, true, null, workdir2,
				"refs/heads/master", "origin", 0);
		clop.run(null);

		Repository repo2 = Activator.getDefault().getRepositoryCache().lookupRepository(new File(workdir2,
				Constants.DOT_GIT));
		repository2 = new TestRepository(repo2);
		// we push to branch "test" of repository2
		RefUpdate createBranch = repository2.getRepository().updateRef(
				"refs/heads/test");
		createBranch.setNewObjectId(repository2.getRepository().resolve(
				"refs/heads/master"));
		createBranch.update();
	}

	@After
	public void tearDown() throws Exception {
		repository1.dispose();
		repository2.dispose();
		repository1 = null;
		repository2 = null;
		testUtils.deleteTempDirs();
	}

	/**
	 * Push from repository1 "master" into "test" of repository2.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPush() throws Exception {

		// push from repository1 to repository2
		PushOperation pop = createPushOperation();
		pop.run(new NullProgressMonitor());
		assertEquals(Status.UP_TO_DATE, getStatus(pop.getOperationResult()));

		// let's add a new file to the project shared with repository1
		IProject proj = importProject(repository1, projectName);
		ArrayList<IFile> files = new ArrayList<IFile>();
		IFile newFile = testUtils.addFileToProject(proj, "folder2/file2.txt",
				"New file");
		files.add(newFile);
		IFile[] fileArr = files.toArray(new IFile[files.size()]);

		AddToIndexOperation trop = new AddToIndexOperation(files);
		trop.execute(null);
		CommitOperation cop = new CommitOperation(fileArr, files, files,
				TestUtils.AUTHOR, TestUtils.COMMITTER, "Added file");
		cop.execute(null);

		proj.delete(false, false, null);

		pop = createPushOperation();
		pop.run(null);
		assertEquals(Status.OK, getStatus(pop.getOperationResult()));

		try {
			// assert that we can not run this again
			pop.run(null);
			fail("Expected Exception not thrown");
		} catch (IllegalStateException e) {
			// expected
		}

		pop = createPushOperation();
		pop.run(null);
		assertEquals(Status.UP_TO_DATE, getStatus(pop.getOperationResult()));

		String newFilePath = newFile.getFullPath().toOSString();

		File testFile = new File(workdir2, newFilePath);
		assertFalse(testFile.exists());
		testFile = new File(workdir, newFilePath);
		assertTrue(testFile.exists());

		// check out test and verify the file is there
		BranchOperation bop = new BranchOperation(repository2.getRepository(),
				"refs/heads/test");
		bop.execute(null);
		testFile = new File(workdir2, newFilePath);
		assertTrue(testFile.exists());
	}

	/**
	 * We should get an {@link IllegalStateException} if we run
	 * getOperationResult before run()
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalStateExceptionOnGetResultWithoutRun()
			throws Exception {
		// push from repository1 to repository2
		PushOperation pop = createPushOperation();
		try {
			pop.getOperationResult();
			fail("Expected Exception not thrown");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * We should get an {@link IllegalStateException} if the spec was re-used
	 *
	 * @throws Exception
	 */
	@Test
	public void testPushWithReusedSpec() throws Exception {

		PushOperationSpecification spec = new PushOperationSpecification();
		// the remote is repo2
		URIish remote = new URIish("file:///"
				+ repository2.getRepository().getDirectory().toString());
		// update master upon master
		List<RemoteRefUpdate> refUpdates = new ArrayList<RemoteRefUpdate>();
		RemoteRefUpdate update = new RemoteRefUpdate(repository1
				.getRepository(), "HEAD", "refs/heads/test", false, null, null);
		refUpdates.add(update);
		spec.addURIRefUpdates(remote, refUpdates);

		PushOperation pop = new PushOperation(repository1.getRepository(),
				spec, false, 0);
		pop.run(null);

		pop = new PushOperation(repository1.getRepository(), spec, false, 0);
		try {
			pop.run(null);
			fail("Expected Exception not thrown");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	private Status getStatus(PushOperationResult operationResult) {
		URIish uri = operationResult.getURIs().iterator().next();
		return operationResult.getPushResult(uri).getRemoteUpdates().iterator()
				.next().getStatus();
	}

	private PushOperation createPushOperation() throws Exception {
		// set up push from repository1 to repository2
		// we can not re-use the RemoteRefUpdate!!!
		PushOperationSpecification spec = new PushOperationSpecification();
		// the remote is repo2
		URIish remote = new URIish("file:///"
				+ repository2.getRepository().getDirectory().toString());
		// update master upon master
		List<RemoteRefUpdate> refUpdates = new ArrayList<RemoteRefUpdate>();
		RemoteRefUpdate update = new RemoteRefUpdate(repository1
				.getRepository(), "HEAD", "refs/heads/test", false, null, null);
		refUpdates.add(update);
		spec.addURIRefUpdates(remote, refUpdates);
		// now we can construct the push operation
		PushOperation pop = new PushOperation(repository1.getRepository(),
				spec, false, 0);
		return pop;
	}

}

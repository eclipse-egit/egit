/*******************************************************************************
 * Copyright (C) 2010, 2012 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ListRemoteOperationTest extends DualRepositoryTestCase {

	File workdir;

	File workdir2;

	String projectName = "ListRemoteTest";

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
		repository1.trackAllFiles(project);
		repository1.commit("Initial commit");

		// let's get rid of the project
		project.delete(false, false, null);

		// let's clone repository1 to repository2
		URIish uri = new URIish("file:///"
				+ repository1.getRepository().getDirectory().toString());
		CloneOperation clop = new CloneOperation(uri, true, null, workdir2,
				"refs/heads/master", "origin", 0);
		clop.run(null);

		Repository existingRepo = Activator
				.getDefault()
				.getRepositoryCache()
				.lookupRepository(
						new File(workdir2, Constants.DOT_GIT));
		repository2 = new TestRepository(existingRepo);
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
		testUtils.deleteTempDirs();
	}

	/**
	 * List the refs both ways
	 *
	 * @throws Exception
	 */
	@Test
	public void testListRemote() throws Exception {

		URIish uri = new URIish("file:///"
				+ repository2.getRepository().getDirectory().getPath());
		ListRemoteOperation lrop = new ListRemoteOperation(repository1
				.getRepository(), uri, 0);
		lrop.run(null);
		assertEquals(4, lrop.getRemoteRefs().size());
		assertNotNull(lrop.getRemoteRef("refs/heads/test"));

		uri = new URIish("file:///"
				+ repository1.getRepository().getDirectory().getPath());
		lrop = new ListRemoteOperation(repository2.getRepository(), uri, 0);
		lrop.run(new NullProgressMonitor());
		assertEquals(2, lrop.getRemoteRefs().size());
		assertNotNull(lrop.getRemoteRef("refs/heads/master"));
	}

	/**
	 * Call getRemoteRefs without having run the op
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalStateException() throws Exception {

		URIish uri = new URIish("file:///"
				+ repository2.getRepository().getDirectory().getPath());
		ListRemoteOperation lrop = new ListRemoteOperation(repository1
				.getRepository(), uri, 0);
		try {
			lrop.getRemoteRefs();
			fail("Expected Exception not thrown");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Test with illegal URI
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalURI() throws Exception {

		URIish uri = new URIish("file:///" + "no/path");
		ListRemoteOperation lrop = new ListRemoteOperation(repository1
				.getRepository(), uri, 0);
		try {
			lrop.run(new NullProgressMonitor());
			fail("Expected Exception not thrown");
		} catch (InvocationTargetException e) {
			// expected
		}
	}
}

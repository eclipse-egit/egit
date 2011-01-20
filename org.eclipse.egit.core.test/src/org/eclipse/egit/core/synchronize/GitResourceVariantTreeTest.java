/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.MASTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitResourceVariantTreeTest extends GitTestCase {

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Before
	public void createGitRepository() throws Exception {
		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	/**
	 * roots() method should return list of projects that are associated with
	 * given repository. In this case there is only one project associated with
	 * this repository therefore only one root should be returned.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnOneRoot() throws Exception {
		// when
		new Git(repo).commit().setAuthor("JUnit", "junit@egit.org")
				.setMessage("Initial commit").call();
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

		// given
		GitResourceVariantTree grvt = new GitTestResourceVariantTree(dataSet,
				null);

		// then
		assertEquals(1, grvt.roots().length);
		IResource actualProject = grvt.roots()[0];
		assertEquals(this.project.getProject(), actualProject);
	}

	/**
	 * When we have two or more project associated with repository, roots()
	 * method should return list of project. In this case we have two project
	 * associated with particular repository, therefore '2' value is expected.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnTwoRoots() throws Exception {
		// when
		// create second project
		TestProject secondProject = new TestProject(true, "Project-2");
		IProject secondIProject = secondProject.project;
		// add connect project with repository
		new ConnectProviderOperation(secondIProject, gitDir).execute(null);
		new Git(repo).commit().setAuthor("JUnit", "junit@egit.org")
				.setMessage("Initial commit").call();
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD, false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

		// given
		GitResourceVariantTree grvt = new GitTestResourceVariantTree(dataSet,
				null);

		// then
		IResource[] roots = grvt.roots();
		// sort in order to be able to assert the project instances
		Arrays.sort(roots, new Comparator<IResource>() {
			public int compare(IResource r1, IResource r2) {
				String path1 = r1.getFullPath().toString();
				String path2 = r2.getFullPath().toString();
				return path1.compareTo(path2);
			}
		});
		assertEquals(2, roots.length);
		IResource actualProject = roots[0];
		assertEquals(this.project.project, actualProject);
		IResource actualProject1 = roots[1];
		assertEquals(secondIProject, actualProject1);
	}

	/**
	 * Checks that getResourceVariant will not throw NPE for null argument. This
	 * method is called with null argument when local or remote resource does
	 * not exist.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnNullResourceVariant() throws Exception {
		// when
		new Git(repo).commit().setAuthor("JUnit", "junit@egit.org")
				.setMessage("Initial commit").call();
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, MASTER,
				false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

		// given
		GitResourceVariantTree grvt = new GitRemoteResourceVariantTree(dataSet);

		// then
		assertNull(grvt.getResourceVariant(null));
	}

	/**
	 * getResourceVariant() should return null when given resource doesn't exist
	 * in repository.
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnNullResourceVariant2() throws Exception {
		// when
		IPackageFragment iPackage = project.createPackage("org.egit.test");
		IType mainJava = project.createType(iPackage, "Main.java",
				"class Main {}");
		new Git(repo).commit().setAuthor("JUnit", "junit@egit.org")
				.setMessage("Initial commit").call();
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, MASTER,
				false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

		// given
		GitResourceVariantTree grvt = new GitRemoteResourceVariantTree(dataSet);

		// then
		assertNull(grvt.getResourceVariant(mainJava.getResource()));
	}

}

/*******************************************************************************
 * Copyright (C) 2010, 2015 Dariusz Luksza <dariusz@luksza.org> and others.
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
import static org.eclipse.jgit.lib.Constants.MASTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.models.SampleModelProvider;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantTree;
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
		try (Git git = new Git(repo)) {
			git.commit().setAuthor("JUnit", "junit@egit.org")
					.setMessage("Initial commit").call();
		}
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD,
				false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

		// given
		GitResourceVariantTree grvt = new GitTestResourceVariantTree(dataSet,
				null, null);

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
		try {
			IProject secondIProject = secondProject.project;
			// add connect project with repository
			new ConnectProviderOperation(secondIProject, gitDir).execute(null);
			try (Git git = new Git(repo)) {
				git.commit().setAuthor("JUnit", "junit@egit.org")
						.setMessage("Initial commit").call();
			}
			GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, HEAD,
					false);
			GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

			// given
			GitResourceVariantTree grvt = new GitTestResourceVariantTree(dataSet,
					null, null);

			// then
			IResource[] roots = grvt.roots();
			// sort in order to be able to assert the project instances
			Arrays.sort(roots, Comparator
					.comparing(resource -> resource.getFullPath().toString()));
			assertEquals(2, roots.length);
			IResource actualProject = roots[0];
			assertEquals(this.project.project, actualProject);
			IResource actualProject1 = roots[1];
			assertEquals(secondIProject, actualProject1);
		} finally {
			secondProject.dispose();
		}
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
		try (Git git = new Git(repo)) {
			git.commit().setAuthor("JUnit", "junit@egit.org")
					.setMessage("Initial commit").call();
		}
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, MASTER,
				false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

		// given
		GitResourceVariantTree grvt = new GitRemoteResourceVariantTree(null,
				dataSet);

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
		try (Git git = new Git(repo)) {
			git.commit().setAuthor("JUnit", "junit@egit.org")
					.setMessage("Initial commit").call();
		}
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, MASTER,
				false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitSyncCache cache = GitSyncCache.getAllData(dataSet,
				new NullProgressMonitor());

		// given
		GitResourceVariantTree grvt = new GitRemoteResourceVariantTree(cache,
				dataSet);

		// then
		assertNull(grvt.getResourceVariant(mainJava.getResource()));
	}

	/**
	 * Check if getResourceVariant() does return the same resource that was
	 * committed. Passes only when it is run as a single test, not as a part of
	 * largest test suite
	 *
	 * @throws Exception
	 */
	@Test
	public void shoulReturnSameResourceVariant() throws Exception {
		// when
		String fileName = "Main.java";
		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendContentAndCommit(iProject, file, "class Main {}",
				"initial commit");
		IFile mainJava = testRepo.getIFile(iProject, file);
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, MASTER,
				false);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitSyncCache cache = GitSyncCache.getAllData(dataSet,
				new NullProgressMonitor());

		// given
		GitResourceVariantTree grvt = new GitRemoteResourceVariantTree(cache,
				dataSet);

		// then
		// null variant indicates that resource wasn't changed
		assertNull(grvt.getResourceVariant(mainJava));
	}

	@Test
	public void shouldNotReturnNullOnSameResouceVariant() throws Exception {
		String modifiedFileName = "changingFile."
				+ SampleModelProvider.SAMPLE_FILE_EXTENSION;
		String unchangedFileName = "notChangingFile."
				+ SampleModelProvider.SAMPLE_FILE_EXTENSION;
		String removedFileName = "toBeRemovedFile."
				+ SampleModelProvider.SAMPLE_FILE_EXTENSION;

		File modifiedFile = testRepo.createFile(iProject, modifiedFileName);
		File unchangedFile = testRepo.createFile(iProject, unchangedFileName);
		File removedFile = testRepo.createFile(iProject, removedFileName);

		testRepo.appendFileContent(modifiedFile, "My content is changing");
		testRepo.appendFileContent(unchangedFile, "My content is constant");
		testRepo.appendFileContent(removedFile, "I will be removed");

		IFile iModifiedFile = testRepo.getIFile(iProject, modifiedFile);
		IFile iUnchangedFile = testRepo.getIFile(iProject, unchangedFile);
		IFile iRemovedFile = testRepo.getIFile(iProject, removedFile);

		testRepo.trackAllFiles(iProject);

		RevCommit firstCommit = testRepo.commit("C1");

		testRepo.appendFileContent(modifiedFile, " My content has changed");
		testRepo.track(modifiedFile);
		testRepo.removeFromIndex(removedFile);

		RevCommit secondCommit = testRepo.commit("C2");

		//@formatter:off
		// History (X means has changed)
		//------------------------------------------------------------
		// files					   C1 [HEAD]		  	C2
		// changingFile.sample   	|-----X----------|-------X-------|->
		// notChangingFile.sample	|-----X----------|---------------|->
		// toBeRemovedFile.sample	|-----X----------|-------X-------|->
		//-------------------------------------------------------------
		//@formatter:on

		testRepo.checkoutBranch(firstCommit.getName());

		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());

		// Now synchronize the two commits using our logical model provider
		SampleModelProvider provider = new SampleModelProvider();
		// Get the affected resources
		ResourceMapping[] mappings = provider
				.getMappings(iModifiedFile,
						ResourceMappingContext.LOCAL_CONTEXT,
						new NullProgressMonitor());

		Set<IResource> includedResource = collectResources(mappings);
		Set<IResource> expectedIncludedResources = new HashSet<IResource>();
		expectedIncludedResources.add(iModifiedFile);
		expectedIncludedResources.add(iUnchangedFile);
		expectedIncludedResources.add(iRemovedFile);

		assertEquals(expectedIncludedResources, includedResource);

		// Synchronize the data
		final GitSynchronizeData data = new GitSynchronizeData(
				testRepo.getRepository(), firstCommit.getName(),
				secondCommit.getName(), true, includedResource);
		GitSynchronizeDataSet gitSynchDataSet = new GitSynchronizeDataSet(data);
		final GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gitSynchDataSet);
		subscriber.init(new NullProgressMonitor());

		IResourceVariantTree sourceVariantTree = subscriber.getSourceTree();
		assertNotNull(sourceVariantTree);

		IResourceVariantTree remoteVariantTree = subscriber.getRemoteTree();
		assertNotNull(remoteVariantTree);

		// In the use case in which the file has been deleted the source variant is
		// not null whereas the remote variant is null.It seems quite logic.
		// However in the second use case we have the same result, the source variant is
		// not null whereas the remote is null. In both cases the null value does
		// not mean the same thing. In the first case, the null value means that
		// the resource is no longer in the repository and in the second the
		// null value means there is no change between the two versions.
		// Using these values I am not able to distinguish both case.
		// It is in contradiction with test #shouldReturnNullResourceVariant2()
		// and test #shoulReturnSameResourceVariant(). However I haven't found
		// another way to handle this case. Maybe something can be
		// done with ThreeWayDiffEntry.scan(tw) to force including in the cache
		// some entry even if they have not changed. For example,
		// ThreeWayDiffEntry.scan(tw,includedSource) or maybe try preventing the variant
		// tree to return null by walking throught the repository and looking for the file...

		IResourceVariant unchangedSourceVariant = sourceVariantTree
				.getResourceVariant(iUnchangedFile);
		IResourceVariant unchangedRemoteVariant = remoteVariantTree
				.getResourceVariant(iUnchangedFile);

		assertNotNull(unchangedSourceVariant);
		assertNotNull(unchangedRemoteVariant);

		IResourceVariant removedSourceVariant = sourceVariantTree
				.getResourceVariant(iRemovedFile);
		IResourceVariant removedRemoteVariant = remoteVariantTree
				.getResourceVariant(iRemovedFile);

		assertNotNull(removedSourceVariant);
		assertNull(removedRemoteVariant);

		GitSubscriberResourceMappingContext context = new GitSubscriberResourceMappingContext(subscriber, gitSynchDataSet);
		assertFalse(context.hasLocalChange(iUnchangedFile,
				new NullProgressMonitor()));
		assertFalse(context.hasRemoteChange(iUnchangedFile,
				new NullProgressMonitor()));

		assertFalse(context.hasLocalChange(iModifiedFile,
				new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iModifiedFile,
				new NullProgressMonitor()));

		assertFalse(context.hasLocalChange(iRemovedFile,
				new NullProgressMonitor()));
		assertTrue(context.hasRemoteChange(iRemovedFile,
				new NullProgressMonitor()));
	}

	private static Set<IResource> collectResources(ResourceMapping[] mappings)
			throws CoreException {
		final Set<IResource> resources = new HashSet<IResource>();
		ResourceMappingContext context = ResourceMappingContext.LOCAL_CONTEXT;
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] traversals = mapping.getTraversals(context,
					new NullProgressMonitor());
			for (ResourceTraversal traversal : traversals) {
				resources.addAll(Arrays.asList(traversal.getResources()));
			}
		}
		return resources;
	}

	/**
	 * Create and commit Main.java file in master branch, then create branch
	 * "test" checkout nearly created branch and modify Main.java file.
	 * getResourceVariant() should obtain Main.java file content from "master"
	 * branch. Passes only when it is run as a single test, not as a part of
	 * largest test suite
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldReturnDifferentResourceVariant() throws Exception {
		// when
		String fileName = "Main.java";
		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendContentAndCommit(iProject, file, "class Main {}",
				"initial commit");
		IFile mainJava = testRepo.getIFile(iProject, file);

		testRepo.createAndCheckoutBranch(Constants.R_HEADS + Constants.MASTER,
				Constants.R_HEADS + "test");
		testRepo.appendContentAndCommit(iProject, file, "// test",
				"first commit");
		GitSynchronizeData data = new GitSynchronizeData(repo, HEAD, MASTER,
				true);
		GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);
		GitSyncCache cache = GitSyncCache.getAllData(dataSet,
				new NullProgressMonitor());

		// given
		GitResourceVariantTree grvt = new GitBaseResourceVariantTree(cache,
				dataSet);

		// then
		IResourceVariant actual = grvt.getResourceVariant(mainJava);
		assertNotNull(actual);
		assertEquals(fileName, actual.getName());

		InputStream actualIn = actual.getStorage(new NullProgressMonitor())
				.getContents();
		byte[] actualByte = getBytesAndCloseStream(actualIn);
		InputStream expectedIn = mainJava.getContents();
		byte[] expectedByte = getBytesAndCloseStream(expectedIn);

		// assert arrays not equals
		assertFalse(Arrays.equals(expectedByte, actualByte));
	}

	private byte[] getBytesAndCloseStream(InputStream stream) throws Exception {
		try {
			byte[] actualByte = new byte[stream.available()];
			stream.read(actualByte);
			return actualByte;
		} finally {
			stream.close();
		}
	}
}

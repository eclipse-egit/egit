/*******************************************************************************
 * Copyright (C) 2011, Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static junit.framework.Assert.assertTrue;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.internal.resources.mapping.SimpleResourceMapping;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitSubscriberMergeContextTest extends GitTestCase {

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
				.setMessage("Initial commit").call();
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	@Test
	public void markAsMerged() throws Exception {
		new Git(repo).commit().setAuthor("JUnit", "junit@egit.org")
				.setMessage("Initial commit").call();

		GitSynchronizeData gsd = new GitSynchronizeData(repo, HEAD, HEAD, false);
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(gsd);
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);

		String fileName = "src/Main.java";
		File file = testRepo.createFile(iProject, fileName);
		testRepo.appendContentAndCommit(iProject, file, "class Main {}",
				"some file");
		testRepo.addToIndex(iProject.getFile(".classpath"));
		testRepo.addToIndex(iProject.getFile(".project"));
		testRepo.commit("project files");

		IFile workspaceFile = testRepo.getIFile(iProject, file);

		ResourceMapping mapping = new SimpleResourceMapping(workspaceFile);
		ResourceMapping[] inputMappings = new ResourceMapping[] { mapping };
		SubscriberScopeManager manager = new SubscriberScopeManager("Scope",
				inputMappings, subscriber, true);

		testRepo.appendFileContent(file, "some changes");
		Status status = new Git(repo).status().call();
		assertEquals(0, status.getAdded().size());
		assertEquals(1, status.getModified().size());
		String repoRelativePath = testRepo.getRepoRelativePath(workspaceFile.getLocation().toPortableString());
		assertTrue(status.getModified().contains(repoRelativePath));

		GitSubscriberMergeContext mergeContext = new GitSubscriberMergeContext(
				subscriber, manager, gsds);
		IDiff node = new ResourceDiff(iProject.getFolder("src"), IDiff.CHANGE);
		mergeContext.markAsMerged(node, true, null);

		status = new Git(repo).status().call();
		assertEquals(1, status.getChanged().size());
		assertEquals(0, status.getModified().size());
		assertTrue(status.getChanged().contains(repoRelativePath));

	}
}

/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.junit.Assert.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class ReleaseStartOperationTest extends AbstractGitFlowOperationTest {
	@Test
	public void testReleaseBranchCreated() throws Exception {
		testRepository
				.createInitialCommit("testReleaseBranchCreated\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		ReleaseStartOperation releaseStartOperation = new ReleaseStartOperation(
				gfRepo, MY_RELEASE);
		releaseStartOperation.execute(null);

		assertNull(releaseStartOperation.getSchedulingRule());

		assertEquals(gfRepo.getConfig().getFullReleaseBranchName(MY_RELEASE),
				repository.getFullBranch());
	}

	@Test
	public void testReleaseBranchCreatedFromHeadCommit() throws Exception {
		RevCommit initialCommit = testRepository
				.createInitialCommit("testReleaseBranchCreatedFromHeadCommit\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		ReleaseStartOperation releaseStartOperation = new ReleaseStartOperation(
				gfRepo, initialCommit.getName(), MY_RELEASE);
		releaseStartOperation.execute(null);

		assertNull(releaseStartOperation.getSchedulingRule());

		assertEquals(gfRepo.getConfig().getFullReleaseBranchName(MY_RELEASE),
				repository.getFullBranch());
	}

	@Test
	public void testReleaseBranchCreatedFromCommit() throws Exception {
		RevCommit initialCommit = testRepository
				.createInitialCommit("testReleaseBranchCreatedFromCommit\n\nfirst commit\n");
		testRepository
				.createInitialCommit("testReleaseBranchCreatedFromCommit\n\nsecond commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		ReleaseStartOperation releaseStartOperation = new ReleaseStartOperation(
				gfRepo, initialCommit.getName(), MY_RELEASE);
		releaseStartOperation.execute(null);

		assertNotNull(releaseStartOperation.getSchedulingRule());

		assertEquals(gfRepo.getConfig().getFullReleaseBranchName(MY_RELEASE),
				repository.getFullBranch());

		assertEquals(initialCommit, gfRepo.findHead());
	}

	@Test
	public void testReleaseStartWithContent() throws Exception {
		testRepository
				.createInitialCommit("testReleaseStartWithContent\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		testUtils.addFileToProject(project.getProject(), "folder1/file1.txt",
				"Hello world");
		testRepository.connect(project.getProject());
		testRepository.trackAllFiles(project.getProject());
		RevCommit developCommit = testRepository.commit("Initial commit");

		new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);

		RevCommit releaseHead = gfRepo.findHead(gfRepo
				.getConfig().getReleaseBranchName(MY_RELEASE));
		assertEquals(developCommit, releaseHead);
	}

	@Test
	public void testReleaseStartFailed() throws Exception {
		testRepository
				.createInitialCommit("testReleaseStart\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		createTag(gfRepo.findHead(), MY_RELEASE, "irrelevant", repository);

		try {
			new ReleaseStartOperation(gfRepo, MY_RELEASE).execute(null);
			fail();
		} catch (CoreException e) {
			assertEquals(gfRepo.getConfig().getDevelopFull(), repository.getFullBranch());
		}
	}

	protected void createTag(RevCommit head, String name, String message,
			Repository repository) throws CoreException {
		TagBuilder tag = new TagBuilder();
		tag.setTag(name);
		tag.setMessage(message);
		tag.setObjectId(head);
		new TagOperation(repository, tag, false).execute(null);
	}
}

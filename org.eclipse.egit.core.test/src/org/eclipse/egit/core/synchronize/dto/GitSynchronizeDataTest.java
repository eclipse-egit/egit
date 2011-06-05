/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize.dto;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

public class GitSynchronizeDataTest extends GitTestCase {

	private Repository repo;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		TestRepository testRepo = new TestRepository(gitDir);
		testRepo.connect(project.project);
		repo = RepositoryMapping.getMapping(project.project).getRepository();

		// make initial commit
		new Git(repo).commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initall commit").call();
	}

	@Test
	public void shouldReturnSourceMergeForSymbolicRef() throws Exception {
		// given
		Git git = new Git(repo);
		git.branchCreate().setName("test").setStartPoint("refs/heads/master")
				.setUpstreamMode(SetupUpstreamMode.TRACK).call();
		git.checkout().setName("test").call();
		GitSynchronizeData gsd = new GitSynchronizeData(repo, HEAD, HEAD, false);

		// when
		String srcMerge = gsd.getSrcMerge();

		// then
		assertThat(srcMerge, is("refs/heads/master"));
	}

	@Test
	public void shouldReturnSourceMergeForLocalRef() throws Exception {
		// given
		Git git = new Git(repo);
		git.branchCreate().setName("test2").setStartPoint("refs/heads/master")
				.setUpstreamMode(SetupUpstreamMode.TRACK).call();
		git.checkout().setName("test2").call();
		GitSynchronizeData gsd = new GitSynchronizeData(repo, R_HEADS + "test2",
				HEAD, false);

		// when
		String srcMerge = gsd.getSrcMerge();

		// then
		assertThat(srcMerge, is("refs/heads/master"));
	}

	@Test
	public void shouldReturnSourceMergeForRemoteBranch() throws Exception {
		// given
		Git git = new Git(repo);
		git.branchCreate().setName("test3").setStartPoint("refs/heads/master")
				.setUpstreamMode(SetupUpstreamMode.TRACK).call();
		git.checkout().setName("test3").call();
		repo.renameRef(R_HEADS + "test3", Constants.R_REMOTES + "origin/master").rename();
		GitSynchronizeData gsd = new GitSynchronizeData(repo, "refs/remotes/origin/master",
				HEAD, false);

		// when
		String srcMerge = gsd.getSrcMerge();

		// then
		assertThat(srcMerge, is("refs/heads/master"));
	}

}

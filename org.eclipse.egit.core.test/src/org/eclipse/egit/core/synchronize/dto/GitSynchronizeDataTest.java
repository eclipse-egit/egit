/*******************************************************************************
 * Copyright (C) 2011, 2014 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize.dto;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.junit.Before;
import org.junit.Test;

public class GitSynchronizeDataTest extends GitTestCase {

	private Repository repo;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		TestRepository testRepo = new TestRepository(gitDir);
		testRepo.connect(project.project);
		repo = RepositoryMapping.getMapping(project.project).getRepository();

		// make initial commit
		new Git(repo).commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initial commit").call();
	}

	@Test
	public void shouldReturnDstMergeForRemoteBranch() throws Exception {
		// given
		StoredConfig config = repo.getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		remoteConfig.setFetchRefSpecs(Arrays.asList(new RefSpec("+refs/heads/*:refs/remotes/origin/*")));
		remoteConfig.update(config);

		GitSynchronizeData gsd = new GitSynchronizeData(repo, HEAD,
				"refs/remotes/origin/master", false);

		assertThat(gsd.getDstRemoteName(), is("origin"));
		assertThat(gsd.getDstMerge(), is("refs/heads/master"));
	}

}

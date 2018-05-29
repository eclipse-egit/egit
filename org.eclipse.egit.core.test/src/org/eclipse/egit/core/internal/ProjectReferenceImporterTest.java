/*******************************************************************************
 * Copyright (C) 2015, Christian Georgi (SAP SE)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link ProjectReferenceImporter}
 */
public class ProjectReferenceImporterTest extends GitTestCase {

	private TestRepository testRepository;

	private Repository repository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
		RepositoryUtil util = Activator.getDefault().getRepositoryUtil();
		util.addConfiguredRepository(repository.getDirectory());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		RepositoryUtil util = Activator.getDefault().getRepositoryUtil();
		util.removeDir(repository.getDirectory());
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void findRepository_SameURLs() throws Exception {
		addRemote(repository, "origin", uri("ssh://user@host.com:42/repo"));

		File foundRepo = ProjectReferenceImporter
				.findConfiguredRepository(uri("ssh://user@host.com:42/repo"));
		assertEquals(repository.getDirectory(), foundRepo);
	}

	@Test
	public void findRepository_MultipleRemotes() throws Exception {
		addRemote(repository, "remote_1", uri("ssh://user@host.com:42/repo"));
		addRemote(repository, "remote_2", uri("http://host.com/other/repo"));

		File foundRepo = ProjectReferenceImporter
				.findConfiguredRepository(uri("ssh://user@host.com:42/repo"));
		assertEquals(repository.getDirectory(), foundRepo);
	}

	@Test
	public void findRepository_DifferentUsers() throws Exception {
		addRemote(repository, "origin", uri("ssh://user_1@host.com:42/repo"));

		File foundRepo = ProjectReferenceImporter
				.findConfiguredRepository(uri("ssh://user_2@host.com:42/repo"));
		assertEquals(repository.getDirectory(), foundRepo);

		foundRepo = ProjectReferenceImporter
				.findConfiguredRepository(uri("ssh://host.com:42/repo"));
		assertEquals(repository.getDirectory(), foundRepo);
	}

	@Test
	public void findRepository_DifferentUsersAndRepoSuffixes() throws Exception {
		addRemote(repository, "origin", uri("ssh://user_1@host.com:42/repo"));

		File foundRepo = ProjectReferenceImporter
				.findConfiguredRepository(uri("ssh://user_2@host.com:42/repo.git"));
		assertEquals(repository.getDirectory(), foundRepo);
	}

	@Test
	public void findRepository_DifferentRepos() throws Exception {
		addRemote(repository, "origin", uri("ssh://host.com:42/repo_1"));

		File foundRepo = ProjectReferenceImporter
				.findConfiguredRepository(uri("ssh://host.com:42/repo_2"));
		assertNotEquals(repository.getDirectory(), foundRepo);
	}

	private static void addRemote(Repository repository, String name, URIish url)
			throws IOException {
		StoredConfig config = repository.getConfig();
		config.setString("remote", name, "url", url.toString());
		config.save();
	}

	private static URIish uri(String s) throws URISyntaxException {
		return new URIish(s);
	}

}

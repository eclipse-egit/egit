/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.egit.core.RevUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RevUtilsTest extends GitTestCase {

	private TestRepository testRepository;
	private Repository repository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void isContainedInAnyRef() throws Exception {
		RevCommit first = testRepository.createInitialCommit("First commit");
		testRepository.createBranch("refs/heads/master", "refs/heads/other");
		assertFalse(isContainedInAnyRemoteRef(first));

		testRepository.createBranch("refs/heads/master", "refs/remotes/origin/a");
		assertTrue(isContainedInAnyRemoteRef(first));

		RevCommit second = testRepository.commit("Second commit");
		assertFalse(isContainedInAnyRemoteRef(second));

		testRepository.createBranch("refs/heads/master", "refs/remotes/origin/b");
		assertTrue(isContainedInAnyRemoteRef(second));

		RevCommit third = testRepository.commit("Third commit");
		testRepository.commit("Fourth commit");
		testRepository.createBranch("refs/heads/master", "refs/remotes/origin/c");
		assertTrue(isContainedInAnyRemoteRef(third));
	}

	private boolean isContainedInAnyRemoteRef(RevCommit commit) throws IOException {
		Collection<Ref> remoteRefs = repository.getRefDatabase()
				.getRefsByPrefix(Constants.R_REMOTES);
		return RevUtils.isContainedInAnyRef(repository, commit, remoteRefs);
	}

}

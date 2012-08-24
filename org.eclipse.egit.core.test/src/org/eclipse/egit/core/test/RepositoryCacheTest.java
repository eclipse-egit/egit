/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andre Dietisheim (Red Hat) - initial implementation
 ******************************************************************************/
package org.eclipse.egit.core.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;

import java.io.IOException;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RepositoryCacheTest extends GitTestCase {

	private TestRepository testRepository;
	private Repository repository;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepository = new TestRepository(gitDir);
		repository = testRepository.getRepository();
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void shouldNotContainDeletedRepository() throws IOException {
		RepositoryCache cache = Activator.getDefault().getRepositoryCache();
		cache.lookupRepository(repository.getDirectory());
		assertThat(repository, isIn(cache.getAllRepositories()));
		repository.close();
		assertThat(repository, isIn(cache.getAllRepositories()));
	}

}

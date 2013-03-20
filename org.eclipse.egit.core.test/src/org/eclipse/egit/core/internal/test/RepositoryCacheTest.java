/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andre Dietisheim - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.IsNot.not;

import java.io.IOException;

import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.RepositoryCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RepositoryCacheTest extends GitTestCase {

	private TestRepository testRepository;
	private Repository repository;
	private RepositoryCache cache;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		this.testRepository = new TestRepository(gitDir);
		this.repository = testRepository.getRepository();
		this.cache = Activator.getDefault().getRepositoryCache();
	}

	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		repository = null;
		super.tearDown();
	}

	@Test
	public void shouldNotContainDeletedRepository() throws IOException {
		cache.lookupRepository(repository.getDirectory());
		assertThat(repository, isIn(cache.getAllRepositories()));
		FileUtils.delete(repository.getDirectory(), FileUtils.RECURSIVE);
		assertThat(repository, not(isIn(cache.getAllRepositories())));
	}
}

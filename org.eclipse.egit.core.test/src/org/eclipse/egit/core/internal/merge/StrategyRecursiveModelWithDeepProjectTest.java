/*******************************************************************************
 * Copyright (C) 2015 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestProject;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;

/**
 * This executes the same tests as its super-class but in a configuration where
 * the project is not at the root of the git repository. It has been introduced
 * after detecting a bug about such "deep" projects in the preliminary
 * implementations.
 */
public class StrategyRecursiveModelWithDeepProjectTest extends
		StrategyRecursiveModelTest {

	@Before
	public void setUp() throws Exception {
		super.setUp();
		testRepo.disconnect(iProject);

		project = new TestProject(true, "a/b/deepProject");
		iProject = project.project;
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		try (Git git = new Git(repo)) {
			git.commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initial commit").call();
		}
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;

		super.tearDown();
	}
}

/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitModelRootTest extends GitModelTestCase {

	private static Repository repo1, repo2;

	@BeforeClass
	public static void setupEnvironment() throws Exception {
		File repoFile = createProjectAndCommitToRepository(REPO1);
		repo1 = lookupRepository(repoFile);

		repoFile = createProjectAndCommitToRepository(REPO2);
		repo2 = lookupRepository(repoFile);
	}

	@Test
	public void shouldIgnoreEmptyRepositories() throws Exception {
		// given
		touchAndSubmit("second commit");
		GitSynchronizeDataSet gsds = new GitSynchronizeDataSet();
		gsds.add(new GitSynchronizeData(repo1, HEAD + "~1", HEAD, false));
		gsds.add(new GitSynchronizeData(repo2, HEAD, HEAD, false));

		// when
		GitModelRoot root = new GitModelRoot(gsds);

		// then
		assertThat(Integer.valueOf(root.getChildren().length),
				is(Integer.valueOf(1)));
	}

}

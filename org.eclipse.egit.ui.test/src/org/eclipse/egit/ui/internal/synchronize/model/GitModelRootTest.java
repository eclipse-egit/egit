/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

public class GitModelRootTest extends GitModelTestCase {

	private static Repository repo1, repo2;

	@Before
	public void setupEnvironment() throws Exception {
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
		gsds.add(new GitSynchronizeData(repo1, HEAD, HEAD + "^1", false));
		gsds.add(new GitSynchronizeData(repo2, HEAD, HEAD, false));

		// when
		GitModelRoot root = new GitModelRoot(gsds);

		// then
		assertThat(Integer.valueOf(root.getChildren().length),
				is(Integer.valueOf(1)));
	}

}

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
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.mockito.Mockito;

abstract class GitModelTestCase extends LocalRepositoryTestCase {

	protected File leftRepoFile;

	protected File rightRepoFile;

	protected GitModelRepository createModelRepository() throws Exception {
		return new GitModelRepository(getGSD(lookupRepository(leftRepoFile)));
	}

	protected GitModelCommit createModelCommit() throws Exception {
		return new GitModelCommit(createModelRepository(),
				lookupRepository(leftRepoFile), getCommit(leftRepoFile, HEAD),
				null);
	}

	protected Commit getCommit(File repoFile, String rev) throws Exception {
		Repository repo = lookupRepository(repoFile);
		ObjectId revId = repo.resolve(rev);

		Commit commit = mock(Commit.class);
		Mockito.when(commit.getId()).thenReturn(
				AbbreviatedObjectId.fromObjectId(revId));

		return commit;
	}

	protected GitSynchronizeData getGSD(Repository repo) throws IOException {
		return new GitSynchronizeData(repo, Constants.HEAD,
				Constants.HEAD, true);
	}

	protected IPath getTreeLocation() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder")).getLocation();
	}

	protected IPath getTree1Location() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder1")).getLocation();
	}

	protected IPath getFile1Location() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt")).getLocation();
	}

	protected IPath getFile2Location() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test1.txt")).getLocation();
	}

}

/*******************************************************************************
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import java.io.File;

import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractStagingViewTestCase
		extends GitRepositoriesViewTestBase {

	protected File repositoryFile;

	protected Repository repository;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		repository = lookupRepository(repositoryFile);
		TestUtil.configureTestCommitterAsUser(repository);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);

		selectRepositoryNode();
	}

	@After
	public void after() {
		repository = null;
		Activator.getDefault().getRepositoryUtil().removeDir(repositoryFile);
	}

	protected void setContent(String content) throws Exception {
		setTestFileContent(content);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
	}

	protected void selectRepositoryNode() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();

		SWTBotTreeItem repoNode = myRepoViewUtil.getRootItem(tree,
				repositoryFile);
		repoNode.select();
	}

}

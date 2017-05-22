/*******************************************************************************
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import java.io.File;

import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;

public class AbstractStagingViewTestCase extends LocalRepositoryTestCase {

	protected static final GitRepositoriesViewTestUtils repoViewUtil = new GitRepositoriesViewTestUtils();

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
		TestUtil.hideView(RepositoriesView.VIEW_ID);
		TestUtil.hideView(StagingView.VIEW_ID);
		Activator.getDefault().getRepositoryUtil().removeDir(repositoryFile);
	}

	protected void setContent(String content) throws Exception {
		setTestFileContent(content);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
	}

	protected void selectRepositoryNode() throws Exception {
		SWTBotView repositoriesView = TestUtil
				.showView(RepositoriesView.VIEW_ID);
		SWTBotTree tree = repositoriesView.bot().tree();

		SWTBotTreeItem repoNode = repoViewUtil.getRootItem(tree,
				repositoryFile);
		repoNode.select();
	}

}

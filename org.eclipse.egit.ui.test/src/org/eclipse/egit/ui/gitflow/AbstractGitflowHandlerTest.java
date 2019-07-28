/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.gitflow;

import static org.eclipse.egit.gitflow.ui.internal.UIPreferences.FEATURE_FINISH_KEEP_BRANCH;
import static org.eclipse.egit.gitflow.ui.internal.UIPreferences.FEATURE_FINISH_SQUASH;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureCheckoutOperation;
import org.eclipse.egit.gitflow.op.FeatureStartOperation;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Gitflow
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class AbstractGitflowHandlerTest extends LocalRepositoryTestCase {
	protected static final String DEVELOP = "develop";
	protected static final String FEATURE_NAME = "myFeature";

	protected Repository repository;
	protected File repositoryFile;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		repository = lookupRepository(repositoryFile);

		resetPreferences();
	}

	@After
	public void teardown() {
		repository = null;
	}

	private void resetPreferences() {
		IPreferenceStore prefStore = Activator.getDefault()
				.getPreferenceStore();
		prefStore.setValue(FEATURE_FINISH_SQUASH, false);
		prefStore.setValue(FEATURE_FINISH_KEEP_BRANCH, false);
	}

	protected RevCommit setContentAddAndCommit(String newContent) throws Exception, GitAPIException, NoHeadException,
	NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException, WrongRepositoryStateException,
	AbortedByHookException, IOException {
		Git git = setContentAndStage(newContent);
		CommitCommand commit = git.commit().setMessage(newContent);
		commit.setAuthor(TestUtil.TESTCOMMITTER_NAME, TestUtil.TESTCOMMITTER_EMAIL);
		commit.setCommitter(TestUtil.TESTCOMMITTER_NAME, TestUtil.TESTCOMMITTER_EMAIL);
		return commit.call();
	}

	protected Git setContentAndStage(String newContent)
			throws Exception, GitAPIException, NoFilepatternException {
		setTestFileContent(newContent);

		Git git = Git.wrap(repository);
		git.add().addFilepattern(".").call();
		return git;
	}

	protected void createFeature(String featureName) throws CoreException {
		new FeatureStartOperation(new GitFlowRepository(repository),
				featureName).execute(null);
	}

	protected void checkoutFeature(String featureName) throws CoreException {
		new FeatureCheckoutOperation(new GitFlowRepository(repository),
				featureName).execute(null);
	}

	protected Ref findBranch(String branchName) throws IOException {
		return repository.exactRef(R_HEADS + branchName);
	}

	protected void init() throws CoreException {
		new InitOperation(repository).execute(null);
	}

	protected void checkoutBranch(String branchToCheckout)
			throws CoreException {
		new BranchOperation(repository, branchToCheckout).execute(null);
	}

	protected void createFeatureUi(String featureName) {
		final SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		final String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("TeamGitFlowMenu.name", false, Activator.getDefault().getBundle()),
				util.getPluginLocalizedValue("TeamGitFlowFeatureStart.name", false, Activator.getDefault().getBundle()) };

		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);

		bot.waitUntil(shellIsActive(UIText.FeatureStartHandler_provideFeatureName));
		bot.text().setText(featureName);
		bot.button(UIText.StartDialog_ButtonOK).click();
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY, "Git flow jobs"));
	}
}

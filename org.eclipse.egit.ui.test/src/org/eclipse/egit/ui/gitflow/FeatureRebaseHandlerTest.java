/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.gitflow;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureCheckoutOperation;
import org.eclipse.egit.gitflow.op.FeatureStartOperation;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Gitflow->Rebase Feature actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FeatureRebaseHandlerTest extends AbstractGitflowHandlerTest {

	@Test
	public void testRebase() throws Exception {
		Git git = Git.wrap(repository);

		init();

		createFeature(FEATURE_NAME);
		setContentAddAndCommit("foo");

		checkoutBranch(DEVELOP);
		setContentAddAndCommit("bar");

		checkoutFeature(FEATURE_NAME);

		rebaseFeature();

		Status call = git.status().call();
		Object[] conflicting = call.getConflicting().toArray();
		assertEquals(1, conflicting.length);
		assertEquals(FILE1_PATH, conflicting[0]);

		assertEquals("org.eclipse.egit.ui.InteractiveRebaseView", bot.activeView().getReference().getId());
	}

	private void init() throws CoreException {
		new InitOperation(repository).execute(null);
	}

	private void createFeature(String featureName) throws CoreException {
		new FeatureStartOperation(new GitFlowRepository(repository),
				featureName).execute(null);
	}

	private void checkoutFeature(String featureName) throws CoreException {
		new FeatureCheckoutOperation(new GitFlowRepository(repository), featureName).execute(null);
	}

	private void checkoutBranch(String branchToCheckout) throws CoreException {
		new BranchOperation(repository, branchToCheckout).execute(null);
	}

	private void rebaseFeature() {
		final SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		final String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("TeamGitFlowMenu.name", false, Activator.getDefault().getBundle()),
				util.getPluginLocalizedValue("TeamGitFlowFeatureRebase.name", false, Activator.getDefault().getBundle()) };

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				ContextMenuHelper.clickContextMenuSync(projectExplorerTree, menuPath);
			}
		});
		bot.button().click();
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY, "Git flow jobs"));
	}
}

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
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Gitflow->Feature Start/Finish actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FeatureStartFinishHandlerTest extends AbstractGitflowHandlerTest {

	@Test
	public void testFeatureStart() throws Exception {
		init();

		setContentAddAndCommit("bar");

		createFeature(FEATURE_NAME);
		RevCommit featureBranchCommit = setContentAddAndCommit("foo");

		checkoutBranch(DEVELOP);

		checkoutFeature(FEATURE_NAME);

		finishFeature();

		RevCommit developHead = new GitFlowRepository(repository).findHead();
		assertEquals(developHead, featureBranchCommit);
	}

	private void finishFeature() {
		final SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		final String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("TeamGitFlowMenu.name", false, Activator.getDefault().getBundle()),
				util.getPluginLocalizedValue("TeamGitFlowFeatureFinish.name", false, Activator.getDefault().getBundle()) };
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				ContextMenuHelper.clickContextMenuSync(projectExplorerTree, menuPath);
			}
		});
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY, "Git flow jobs"));
	}

	private void init() throws CoreException {
		new InitOperation(repository).execute(null);
	}

	private void createFeature(String featureName) {
		final SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		final String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("TeamGitFlowMenu.name", false, Activator.getDefault().getBundle()),
				util.getPluginLocalizedValue("TeamGitFlowFeatureStart.name", false, Activator.getDefault().getBundle()) };

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				ContextMenuHelper.clickContextMenuSync(projectExplorerTree, menuPath);
			}
		});

		bot.waitUntil(shellIsActive(UIText.FeatureStartHandler_provideFeatureName));
		bot.text().typeText(featureName);
		bot.button("OK").click();
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY, "Git flow jobs"));
	}

	private void checkoutFeature(String featureName) {
		final SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		final String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("TeamGitFlowMenu.name", false, Activator.getDefault().getBundle()),
				util.getPluginLocalizedValue("TeamGitFlowFeatureCheckout.name", false, Activator.getDefault().getBundle()) };

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				ContextMenuHelper.clickContextMenuSync(projectExplorerTree, menuPath);
			}
		});

		bot.waitUntil(shellIsActive(UIText.FeatureCheckoutHandler_selectFeature));
		bot.table().select(featureName);
		bot.button("OK").click();
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY, "Git flow jobs"));
	}

	private void checkoutBranch(String branchToCheckout) throws CoreException {
		new BranchOperation(repository, branchToCheckout).execute(null);
	}
}

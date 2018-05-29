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

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureCheckoutOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.osgi.util.NLS;
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

	private static void disableAutomatedMode() {
		// if AUTOMATED_MODE is true, we wouldn't get the error
		// dialog which is part of what we want to test here
		ErrorDialog.AUTOMATED_MODE = false;
	}

	@Test
	public void testRebaseFailOnConflict() throws Exception {
		disableAutomatedMode();

		Git git = Git.wrap(repository);

		init();

		createFeature(FEATURE_NAME);
		setContentAddAndCommit("foo");

		checkoutBranch(DEVELOP);
		setContentAddAndCommit("bar");

		checkoutFeature(FEATURE_NAME);

		rebaseFeature();
		acceptError(RebaseResult.Status.STOPPED);

		Status status = git.status().call();
		Object[] conflicting = status.getConflicting().toArray();
		assertEquals(1, conflicting.length);
		assertEquals(FILE1_PATH, conflicting[0]);

		assertEquals("org.eclipse.egit.ui.InteractiveRebaseView", bot.activeView().getReference().getId());
	}

	private void acceptError(org.eclipse.jgit.api.RebaseResult.Status status) {
		bot.button("Details >>").click();
		bot.list().select(NLS.bind(
				UIText.FeatureRebaseHandler_statusWas, status.toString()));
		bot.button("OK").click();
	}

	@Test
	public void testRebaseFailOnDirtyWorkingDirectory() throws Exception {
		disableAutomatedMode();

		Git git = Git.wrap(repository);

		init();
		setContentAddAndCommit("bar");

		createFeature(FEATURE_NAME);
		setContentAddAndCommit("foo");

		setTestFileContent("foobar");

		rebaseFeature();
		acceptError(RebaseResult.Status.UNCOMMITTED_CHANGES);

		Status status = git.status().call();
		Object[] uncommitted = status.getUncommittedChanges().toArray();
		assertEquals(1, uncommitted.length);
		assertEquals(FILE1_PATH, uncommitted[0]);
	}

	@Override
	protected void checkoutFeature(String featureName) throws CoreException {
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
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY, "Git flow jobs"));
	}
}

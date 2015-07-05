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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.JobFamilies;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Gitflow->Feature Finish action with squash option
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FeatureFinishSquashHandlerTest extends AbstractGitflowHandlerTest {

	private static final String SQUASHED_COMMENT_SUMMARY = "Hello World";

	@Test
	public void testFeatureFinishSquash() throws Exception {
		int expectedCommitCount = 2;

		init();

		setContentAddAndCommit("bar");
		expectedCommitCount++;

		createFeature(FEATURE_NAME);
		RevCommit commit1 = setContentAddAndCommit("commit 1");
		expectedCommitCount++;
		RevCommit commit2 = setContentAddAndCommit("commit 2");
		expectedCommitCount++;

		checkoutBranch(DEVELOP);

		checkoutFeature(FEATURE_NAME);

		finishFeature();
		expectedCommitCount--;

		RevCommit developHead = new GitFlowRepository(repository).findHead();
		assertNotEquals(developHead, commit1);
		assertNotEquals(developHead, commit2);

		assertEquals(expectedCommitCount, countCommits());

		assertTrue(developHead.getFullMessage().startsWith(
				SQUASHED_COMMENT_SUMMARY));
	}

	private int countCommits() throws GitAPIException, NoHeadException,
			IOException {
		Iterable<RevCommit> commits = Git.wrap(repository).log().all().call();
		Iterator<RevCommit> iterator = commits.iterator();
		int count = 0;

		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return count;
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
		bot.checkBox(UIText.FinishFeatureDialog_squashCheck).click();
		bot.button("OK").click();
		int firstLine = 0;
		bot.styledText().selectLine(firstLine);
		bot.styledText().typeText(SQUASHED_COMMENT_SUMMARY);
		bot.button("OK").click();
		bot.waitUntil(Conditions.waitForJobs(JobFamilies.GITFLOW_FAMILY, "Git flow jobs"));
	}

	private void init() throws CoreException {
		new InitOperation(repository).execute(null);
	}

	private void checkoutBranch(String branchToCheckout) throws CoreException {
		new BranchOperation(repository, branchToCheckout).execute(null);
	}
}

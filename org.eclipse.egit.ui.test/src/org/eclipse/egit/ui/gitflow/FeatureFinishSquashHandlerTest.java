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
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Gitflow->Feature Finish action with squash option
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FeatureFinishSquashHandlerTest extends
		AbstractFeatureFinishHandlerTest {

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

		IPreferenceStore prefStore = Activator.getDefault()
				.getPreferenceStore();
		assertTrue(prefStore.getBoolean(FEATURE_FINISH_SQUASH));
		assertFalse(prefStore.getBoolean(FEATURE_FINISH_KEEP_BRANCH));
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

	@Override
	protected void preFinish() {
		bot.waitUntil(shellIsActive(
				UIText.FeatureFinishHandler_rewordSquashedCommitMessage));
		String text = bot.styledText().getText();
		text = text.substring(text.indexOf('\n'));
		bot.styledText().setText(SQUASHED_COMMENT_SUMMARY + text);
		bot.button(
				org.eclipse.egit.ui.internal.UIText.RebaseInteractiveHandler_EditMessageDialogOkButton)
				.click();
	}

	@Override
	protected void selectOptions() {
		bot.checkBox(UIText.FinishFeatureDialog_squashCheck).click();
	}
}

/*******************************************************************************
 * Copyright (C) 2011, 2019 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable.syncExec;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class StagingViewTester {

	private SWTBotView stagingView;

	public StagingViewTester(SWTBotView view) {
		stagingView = view;
	}

	public static StagingViewTester openStagingView() throws Exception {
		// This is needed so that we can find staging entries by full path.
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.STAGING_VIEW_FILENAME_MODE, false);

		SWTBotView view = TestUtil.showView(StagingView.VIEW_ID);
		TestUtil.joinJobs(org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		return new StagingViewTester(view);
	}

	public SWTBotView getView() {
		return stagingView;
	}

	public void setAuthor(String author) {
		stagingView.bot().textWithLabel(UIText.StagingView_Author)
				.setText(author);
	}

	public void setCommitter(String committer) {
		stagingView.bot().textWithLabel(UIText.StagingView_Committer)
				.setText(committer);
	}

	public String getAuthor() {
		return stagingView.bot().textWithLabel(UIText.StagingView_Author)
				.getText();
	}

	public String getCommitter() {
		return stagingView.bot().textWithLabel(UIText.StagingView_Committer)
				.getText();
	}

	public void setCommitMessage(String message) {
		stagingView.bot().styledTextWithLabel(UIText.StagingView_CommitMessage)
				.setText(message);
	}

	public void compareWithIndex(String path) {
		SWTBotTree unstagedTree = stagingView.bot().tree(0);

		TestUtil.waitUntilTreeHasNodeContainsText(stagingView.bot(),
				unstagedTree, path, 10000);

		TestUtil.getNode(unstagedTree.getAllItems(), path).select();

		ContextMenuHelper.clickContextMenuSync(unstagedTree,
				UIText.StagingView_CompareWithIndexMenuLabel);
	}

	public void stageFile(String path) {
		SWTBotTree unstagedTree = stagingView.bot().tree(0);

		TestUtil.waitUntilTreeHasNodeContainsText(stagingView.bot(),
				unstagedTree, path, 10000);

		TestUtil.getNode(unstagedTree.getAllItems(), path).select();

		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);

		ContextMenuHelper.clickContextMenu(unstagedTree,
				UIText.StagingView_StageItemMenuLabel);

		jobJoiner.join();
	}

	public boolean stageAllFiles(String path) {
		SWTBotTree unstagedTree = stagingView.bot().tree(0);
		TestUtil.waitUntilTreeHasNodeContainsText(stagingView.bot(),
				unstagedTree, path, 10000);

		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);

		stagingView.bot().toolbarButtonWithTooltip(
				UIText.StagingView_StageAllItemTooltip).click();

		jobJoiner.join();

		TestUtil.waitUntilTreeHasNodeContainsText(stagingView.bot(),
				stagingView.bot().tree(1), path, 10000);
		if (stagingView.bot().tree(0).getAllItems().length == 0) {
			return true;
		}
		return false;
	}

	public void unStageFile(String path) {
		SWTBotTree stagedTree = stagingView.bot().tree(1);

		TestUtil.waitUntilTreeHasNodeContainsText(stagingView.bot(), stagedTree,
				path, 10000);

		TestUtil.getNode(stagedTree.getAllItems(), path).select();

		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);

		ContextMenuHelper.clickContextMenu(stagedTree,
				UIText.StagingView_UnstageItemMenuLabel);

		jobJoiner.join();
	}

	public void commit() throws Exception {
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.COMMIT, 30,
				TimeUnit.SECONDS);
		stagingView.bot().button(UIText.StagingView_Commit).click();
		jobJoiner.join();
	}

	public void assertCommitEnabled(boolean expectEnabled) {
		boolean actual = stagingView.bot().button(UIText.StagingView_Commit)
				.isEnabled();
		if (expectEnabled)
			assertTrue("Expected Commit button to be enabled", actual);
		else
			assertFalse("Expected Commit button to be disabled", actual);
	}

	public void setAmend(boolean amend) {
		SWTBotToolbarToggleButton button = stagingView.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.StagingView_Ammend_Previous_Commit);
		selectToolbarToggle(button, amend);
	}

	public boolean getAmend() {
		SWTBotToolbarToggleButton button = stagingView.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.StagingView_Ammend_Previous_Commit);
		return button.isChecked();
	}

	public void setInsertChangeId(boolean insertChangeId) {
		SWTBotToolbarToggleButton button = stagingView.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.StagingView_Add_Change_ID);
		selectToolbarToggle(button, insertChangeId);
	}

	public boolean getInsertChangeId() {
		SWTBotToolbarToggleButton button = stagingView.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.StagingView_Add_Change_ID);
		return button.isChecked();
	}

	public void setSignedOff(boolean signedOff) {
		SWTBotToolbarToggleButton button = stagingView.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.StagingView_Add_Signed_Off_By);
		selectToolbarToggle(button, signedOff);
	}

	public boolean getSignedOff() {
		SWTBotToolbarToggleButton button = stagingView.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.StagingView_Add_Signed_Off_By);
		return button.isChecked();
	}

	public boolean isCommitEnabled() {
		return stagingView.bot().button(UIText.StagingView_Commit).isEnabled();
	}

	private void selectToolbarToggle(SWTBotToolbarToggleButton button,
			boolean select) {
		if (select) {
			if (!button.isChecked())
				button.select();
		} else {
			if (button.isChecked())
				button.deselect();
		}
	}

	public String getCommitMessage() {
		return stagingView.bot()
				.styledTextWithLabel(UIText.StagingView_CommitMessage)
				.getText();
	}

	public int getCaretPosition() {
		SWTBotStyledText commitMessageArea = stagingView.bot().styledTextWithLabel(UIText.StagingView_CommitMessage);
		Integer pos = syncExec(() -> Integer
				.valueOf(commitMessageArea.widget.getCaretOffset()));
		return pos == null ? -1 : pos.intValue();
	}

	/**
	 * Trigger a explicit index refresh for {@code myRepository} and wait until
	 * the refresh is finished.
	 *
	 * @param myRepository
	 *            Git repository to refresh
	 */
	public void refreshIndex(Repository myRepository) {
		JobJoiner jobJoiner = JobJoiner.startListening(
				org.eclipse.egit.core.JobFamilies.INDEX_DIFF_CACHE_UPDATE, 30,
				TimeUnit.SECONDS);
		IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(myRepository).refresh();
		jobJoiner.join();
	}
}

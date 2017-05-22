/*******************************************************************************
 * Copyright (C) 2011, 2014 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.Position;
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
		TestUtil.processUIEvents();

		return new StagingViewTester(view);
	}

	public void setAuthor(String author) {
		stagingView.bot().textWithLabel(UIText.StagingView_Author)
				.setText(author);
	}

	public void setCommitter(String committer) {
		stagingView.bot().textWithLabel(UIText.StagingView_Committer)
				.setText(committer);
	}

	public void setCommitMessage(String message) {
		stagingView.bot().styledTextWithLabel(UIText.StagingView_CommitMessage)
				.setText(message);
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
		Position cursorPosition = commitMessageArea.cursorPosition();
		List<String> lines = commitMessageArea.getLines();

		int caretPosition = 0;
		for (int i = 0; i <= cursorPosition.line; i++) {
			if (i < cursorPosition.line) {
				caretPosition += lines.get(i).length();
			} else {
				caretPosition += cursorPosition.column;
			}
		}

		return caretPosition;
	}
}

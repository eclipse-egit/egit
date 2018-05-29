/*******************************************************************************
 * Copyright (C) 2011, 2013 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.SWTBotTreeColumn;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class CommitDialogTester {

	public static class NoFilesToCommitPopup {

		SWTBotShell shell;

		public NoFilesToCommitPopup(SWTBotShell shell) {
			this.shell = shell;
		}

		public CommitDialogTester confirmPopup() {
			shell.bot().button(IDialogConstants.YES_LABEL).click();
			SWTWorkbenchBot workbenchBot = new SWTWorkbenchBot();
			SWTBotShell commitDialogShell = workbenchBot
					.shell(UIText.CommitDialog_CommitChanges);
			return new CommitDialogTester(commitDialogShell);
		}

		public void cancelPopup() {
			shell.close();
		}

	}

	private static final TestUtil util = new TestUtil();

	private SWTBotShell commitDialog;

	public CommitDialogTester(SWTBotShell dialogShell) {
		commitDialog = dialogShell;
	}

	public static CommitDialogTester openCommitDialog(String projectName) {
		clickCommitAction(projectName);
		SWTWorkbenchBot workbenchBot = new SWTWorkbenchBot();
		SWTBotShell shell = workbenchBot
				.shell(UIText.CommitDialog_CommitChanges);
		return new CommitDialogTester(shell);
	}

	public static NoFilesToCommitPopup openCommitDialogExpectNoFilesToCommit(
			String projectName) throws Exception {
		clickCommitAction(projectName);
		SWTWorkbenchBot workbenchBot = new SWTWorkbenchBot();
		return new NoFilesToCommitPopup(
				workbenchBot.shell(UIText.CommitAction_noFilesToCommit));
	}

	private static void clickCommitAction(String projectName) {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		util.getProjectItems(projectExplorerTree, projectName)[0].select();
		String menuString = util.getPluginLocalizedValue("CommitAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
	}

	public void setAuthor(String author) {
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Author)
				.setText(author);
	}

	public void setCommitter(String committer) {
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Committer)
				.setText(committer);
	}

	public void setCommitMessage(String message) {
		commitDialog.bot()
				.styledTextWithLabel(UIText.CommitDialog_CommitMessage)
				.setText(message);
	}

	public void commit() throws Exception {
		commitDialog.bot().button(UIText.CommitDialog_Commit).click();
		// wait until commit is completed
		Job.getJobManager().join(JobFamilies.COMMIT, null);
	}

	public void cancel() {
		commitDialog.bot().button(IDialogConstants.CANCEL_LABEL).click();
	}

	public void setAmend(boolean amend) {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.CommitDialog_AmendPreviousCommit);
		selectToolbarToggle(button, amend);
	}

	public boolean getAmend() {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.CommitDialog_AmendPreviousCommit);
		return button.isChecked();
	}

	public void setInsertChangeId(boolean insertChangeId) {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.CommitDialog_AddChangeIdLabel);
		selectToolbarToggle(button, insertChangeId);
	}

	public boolean getInsertChangeId() {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.CommitDialog_AddChangeIdLabel);
		return button.isChecked();
	}

	public void setSignedOff(boolean signedOff) {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(UIText.CommitDialog_AddSOB);
		selectToolbarToggle(button, signedOff);
	}

	public boolean getSignedOff() {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(UIText.CommitDialog_AddSOB);
		return button.isChecked();
	}

	private void selectToolbarToggle(SWTBotToolbarToggleButton button,
			boolean select) {
		if (select) {
			if (!button.isChecked())
				button.select();
		} else if (button.isChecked())
			button.deselect();
	}

	public int getRowCount() {
		return commitDialog.bot().tree().rowCount();
	}

	public String getEntryText(int rowIndex) {
		SWTBotTreeItem treeItem = commitDialog.bot().tree().getAllItems()[rowIndex];
		return treeItem.cell(1);
	}

	public String getCommitMessage() {
		return commitDialog.bot()
				.styledTextWithLabel(UIText.CommitDialog_CommitMessage)
				.getText();
	}

	public boolean isEntryChecked(int rowIndex) {
		SWTBotTreeItem treeItem = commitDialog.bot().tree().getAllItems()[rowIndex];
		return treeItem.isChecked();
	}

	public void setShowUntracked(boolean untracked) {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.CommitDialog_ShowUntrackedFiles);
		selectToolbarToggle(button, untracked);
	}

	public boolean getShowUntracked() {
		SWTBotToolbarToggleButton button = commitDialog.bot()
				.toolbarToggleButtonWithTooltip(
						UIText.CommitDialog_ShowUntrackedFiles);
		return button.isChecked();
	}

	public void sortByName() {
		final Tree tree = commitDialog.bot().tree().widget;
		SWTBotTreeColumn column = SWTBotTreeColumn.getColumn(tree, 1);
		column.click();
	}
}

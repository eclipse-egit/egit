/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

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

	public static CommitDialogTester openCommitDialog(String projectName)
			throws Exception {
		SWTWorkbenchBot workbenchBot = new SWTWorkbenchBot();
		openCommitDialog(projectName, workbenchBot);
		SWTBotShell shell = workbenchBot
				.shell(UIText.CommitDialog_CommitChanges);
		return new CommitDialogTester(shell);
	}

	public static NoFilesToCommitPopup openCommitDialogExpectNoFilesToCommit(
			String projectName) throws Exception {
		SWTWorkbenchBot workbenchBot = new SWTWorkbenchBot();
		openCommitDialog(projectName, workbenchBot);
		return new NoFilesToCommitPopup(
				workbenchBot.shell(UIText.CommitAction_noFilesToCommit));
	}

	private static void openCommitDialog(String projectName,
			SWTWorkbenchBot workbenchBot) {
		SWTBotTree projectExplorerTree = workbenchBot
				.viewById("org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		util.getProjectItem(projectExplorerTree, projectName).select();
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
	
	public void setAmend(boolean amend) {
		SWTBotToolbarToggleButton button = commitDialog.bot().toolbarToggleButtonWithTooltip(UIText.CommitDialog_AmendPreviousCommit);
		selectToolbarToggle(button, amend);
	}
	
	public boolean getAmend() {
		SWTBotToolbarToggleButton button = commitDialog.bot().toolbarToggleButtonWithTooltip(UIText.CommitDialog_AmendPreviousCommit);
		return button.isChecked();
	}

	public void setInsertChangeId(boolean insertChangeId) {
		SWTBotToolbarToggleButton button = commitDialog.bot().toolbarToggleButtonWithTooltip(UIText.CommitDialog_AddChangeIdLabel);
		selectToolbarToggle(button, insertChangeId);
	}

	public boolean getInsertChangeId() {
		SWTBotToolbarToggleButton button = commitDialog.bot().toolbarToggleButtonWithTooltip(UIText.CommitDialog_AddChangeIdLabel);
		return button.isChecked();
	}
	
	public void setSignedOff(boolean signedOff) {
		SWTBotToolbarToggleButton button = commitDialog.bot().toolbarToggleButtonWithTooltip(UIText.CommitDialog_AddSOB);
		selectToolbarToggle(button, signedOff);
	}

	public boolean getSignedOff() {
		SWTBotToolbarToggleButton button = commitDialog.bot().toolbarToggleButtonWithTooltip(UIText.CommitDialog_AddSOB);
		return button.isChecked();
	}
	
	
	private void selectToolbarToggle(SWTBotToolbarToggleButton button, boolean select) {
		if (select) {
			if (!button.isChecked())
				button.select();
		} else {
			if (button.isChecked())
				button.deselect();
		}
	}
	
	public int getRowCount() {
		return commitDialog.bot().table().rowCount();
	}

	public String getEntryText(int rowIndex) {
		return commitDialog.bot().table().getTableItem(rowIndex).getText(1);		
	}

	public String getCommitMessage() {
		return commitDialog.bot()
				.styledTextWithLabel(UIText.CommitDialog_CommitMessage)
				.getText();
	}
}

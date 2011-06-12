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
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.ui.PlatformUI;

public class StagingViewTester {

	// private static final TestUtil util = new TestUtil();

	private SWTBotView stagingView;

	public StagingViewTester(SWTBotView view) {
		stagingView = view;
	}

	public static StagingViewTester openStagingView() throws Exception {
		SWTWorkbenchBot workbenchBot = new SWTWorkbenchBot();
		UIThreadRunnable.syncExec(new VoidResult() {
			public void run() {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().showView(StagingView.VIEW_ID);
				} catch (Exception e) {
					throw new WidgetNotFoundException(e.getMessage(), e);
				}
			}
		});
		SWTBotView view = workbenchBot.viewById(StagingView.VIEW_ID);
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

	public void commit() throws Exception {
		stagingView.toolbarPushButton(UIText.StagingView_Commit).click();
		// wait until commit is completed
		Job.getJobManager().join(JobFamilies.COMMIT, null);
	}

	public void setAmend(boolean amend) {
		SWTBotToolbarToggleButton button = stagingView
				.toolbarToggleButton(UIText.StagingView_Ammend_Previous_Commit);
		selectToolbarToggle(button, amend);
	}

	public boolean getAmend() {
		SWTBotToolbarToggleButton button = stagingView
				.toolbarToggleButton(UIText.StagingView_Ammend_Previous_Commit);
		return button.isChecked();
	}

	public void setInsertChangeId(boolean insertChangeId) {
		SWTBotToolbarToggleButton button = stagingView
				.toolbarToggleButton(UIText.StagingView_Add_Change_ID);
		selectToolbarToggle(button, insertChangeId);
	}

	public boolean getInsertChangeId() {
		SWTBotToolbarToggleButton button = stagingView
				.toolbarToggleButton(UIText.StagingView_Add_Change_ID);
		return button.isChecked();
	}

	public void setSignedOff(boolean signedOff) {
		SWTBotToolbarToggleButton button = stagingView
				.toolbarToggleButton(UIText.StagingView_Add_Signed_Off_By);
		selectToolbarToggle(button, signedOff);
	}

	public boolean getSignedOff() {
		SWTBotToolbarToggleButton button = stagingView
				.toolbarToggleButton(UIText.StagingView_Add_Signed_Off_By);
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
}

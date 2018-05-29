/*******************************************************************************
 * Copyright (c) 2013, 2016 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class PushBranchWizardTester {

	private final SWTBot wizard;

	public static PushBranchWizardTester startWizard(SWTBotTree projectTree,
			String branchName) {
		String pushBranchMenu = branchName.equals(Constants.HEAD)
				? UIText.PushMenu_PushHEAD
				: NLS.bind(UIText.PushMenu_PushBranch, branchName);
		ContextMenuHelper.clickContextMenu(projectTree, "Team", pushBranchMenu);
		return forBranchName(branchName);
	}

	public static PushBranchWizardTester forBranchName(String branchName) {
		SWTWorkbenchBot bot = new SWTWorkbenchBot();
		String title = branchName.equals(Constants.HEAD)
				? UIText.PushCommitHandler_pushCommitTitle
				: MessageFormat.format(UIText.PushBranchWizard_WindowTitle,
						branchName);
		SWTBot wizard = bot.shell(title).bot();
		return new PushBranchWizardTester(wizard);
	}

	public PushBranchWizardTester(SWTBot wizard) {
		this.wizard = wizard;
	}

	public void selectRemote(String remoteName) {
		SWTBotCombo remoteCombo = wizard
				.comboBoxWithLabel(UIText.PushBranchPage_RemoteLabel);
		String[] items = remoteCombo.items();
		for (String item : items) {
			if (item.startsWith(remoteName + ":")) {
				remoteCombo.setSelection(item);
				return;
			}
		}
		throw new IllegalStateException("Could not select remote '"
				+ remoteName + "', items were: " + Arrays.toString(items));
	}

	public void selectNewRemoteOnBranchPage(String remoteName, String uri) {
		wizard.button(UIText.PushBranchPage_NewRemoteButton).click();
		SWTBot addRemoteWizard = wizard.shell(UIText.AddRemoteWizard_Title)
				.bot();
		setRemoteNameAndUri(remoteName, uri, addRemoteWizard);
		addRemoteWizard.button(IDialogConstants.FINISH_LABEL).click();
	}

	public void enterRemoteOnInitialPage(String remoteName, String uri) {
		setRemoteNameAndUri(remoteName, uri, wizard);
	}

	private void setRemoteNameAndUri(String remoteName, String uri,
			SWTBot remotePage) {
		SWTBotText remoteNameText = remotePage
				.textWithLabel(UIText.AddRemotePage_RemoteNameLabel);
		remoteNameText.setText(remoteName);
		SWTBotText uriText = remotePage
				.textWithLabel(UIText.RepositorySelectionPage_promptURI
				+ ":");
		uriText.setText(uri);
	}

	public void enterBranchName(String branchName) {
		wizard.textWithLabel(UIText.PushBranchPage_RemoteBranchNameLabel)
				.setText(
				branchName);
	}

	public void assertBranchName(String branchName) {
		assertEquals(branchName,
				wizard.textWithLabel(
						UIText.PushBranchPage_RemoteBranchNameLabel)
						.getText());
	}

	public void deselectConfigureUpstream() {
		wizard.checkBox(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.deselect();
	}

	public void selectMerge() {
		wizard.checkBox(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.select();
		wizard.comboBoxWithLabel(UIText.BranchRebaseModeCombo_RebaseModeLabel)
				.setSelection(UIText.BranchRebaseMode_None);
	}

	public void selectRebase() {
		wizard.checkBox(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.select();
		wizard.comboBoxWithLabel(UIText.BranchRebaseModeCombo_RebaseModeLabel)
				.setSelection(UIText.BranchRebaseMode_Rebase);
	}

	public void assertConfigureUpstreamSelected() {
		assertTrue(wizard.checkBox(
				UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.isChecked());
	}

	public void assertMergeSelected() {
		assertConfigureUpstreamSelected();
		assertEquals(UIText.BranchRebaseMode_None, wizard
				.comboBoxWithLabel(UIText.BranchRebaseModeCombo_RebaseModeLabel)
				.selection());
	}

	public void assertRebaseSelected() {
		assertConfigureUpstreamSelected();
		assertEquals(UIText.BranchRebaseMode_Rebase, wizard
				.comboBoxWithLabel(UIText.BranchRebaseModeCombo_RebaseModeLabel)
				.selection());
	}

	public boolean isUpstreamConfigOverwriteWarningShown() {
		return wizard.text(1).getText()
				.contains(UIText.PushBranchPage_UpstreamConfigOverwriteWarning);
	}

	public void next() {
		wizard.button(UIText.PushBranchWizard_previewButton).click();
	}

	public void finish() {
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.PUSH, 60,
				TimeUnit.SECONDS);
		wizard.button(UIText.PushBranchWizard_pushButton).click();
		jobJoiner.join();
	}
}

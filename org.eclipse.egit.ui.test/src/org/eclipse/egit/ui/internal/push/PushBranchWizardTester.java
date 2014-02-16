/*******************************************************************************
 * Copyright (c) 2013, 2014 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class PushBranchWizardTester {

	private final SWTBot wizard;

	public static PushBranchWizardTester startWizard(SWTBotTree projectTree,
			String branchName) {
		TestUtil util = new TestUtil();
		String pushBranchMenu = util
				.getPluginLocalizedValue("PushBranchAction.label");
		ContextMenuHelper.clickContextMenu(projectTree, "Team", pushBranchMenu);
		return forBranchName(branchName);
	}

	public static PushBranchWizardTester forBranchName(String branchName) {
		SWTWorkbenchBot bot = new SWTWorkbenchBot();
		String title = MessageFormat.format(
				UIText.PushBranchWizard_WindowTitle, branchName);
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
		wizard.textWithLabel(UIText.PushBranchPage_BranchNameLabel).setText(
				branchName);
	}

	public void deselectConfigureUpstream() {
		wizard.checkBox(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.deselect();
	}

	public void selectMerge() {
		wizard.checkBox(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.select();
		wizard.radio(UIText.UpstreamConfigComponent_MergeRadio).click();
	}

	public void selectRebase() {
		wizard.checkBox(UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.select();
		wizard.radio(UIText.UpstreamConfigComponent_RebaseRadio).click();
	}

	public void assertRebaseSelected() {
		assertTrue(wizard.checkBox(
				UIText.UpstreamConfigComponent_ConfigureUpstreamCheck)
				.isChecked());
		assertTrue(wizard.radio(UIText.UpstreamConfigComponent_RebaseRadio)
				.isSelected());
	}

	public void next() {
		wizard.button(IDialogConstants.NEXT_LABEL).click();
	}

	public void finish() {
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.PUSH, 60,
				TimeUnit.SECONDS);
		wizard.button(IDialogConstants.FINISH_LABEL).click();
		jobJoiner.join();
	}
}

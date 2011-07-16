/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Push action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class PushActionTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static File remoteRepositoryFile;

	private static SWTBotPerspective perspective;

	@BeforeClass
	public static void setup() throws Exception {
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		repositoryFile = createProjectAndCommitToRepository();
		remoteRepositoryFile = createRemoteRepository(repositoryFile);
		touchAndSubmit(null);
		waitInUI();
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	@Test
	public void testPushToBothDestination() throws Exception {
		pushTo("both", true);
		pushTo("both", false);
	}

	@Test
	public void testPushToPushDestination() throws Exception {
		pushTo("push", true);
		pushTo("push", false);
	}

	private void pushTo(String destination, boolean withConfirmPage)
			throws Exception, MissingObjectException,
			IncorrectObjectTypeException, IOException {
		Repository repo = lookupRepository(remoteRepositoryFile);
		RevWalk rw = new RevWalk(repo);
		String previous = rw.parseCommit(repo.resolve("HEAD")).name();

		touchAndSubmit(null);
		SWTBotShell pushDialog = openPushDialog();

		SWTBotCombo destinationCombo = pushDialog.bot().comboBox();
		String[] items = destinationCombo.items();
		for (int i = 0; i < items.length; i++) {
			if (items[i].startsWith(destination))
				destinationCombo.setSelection(i);
		}

		pushDialog.bot().button(IDialogConstants.NEXT_LABEL).click();
		if (withConfirmPage)
			pushDialog.bot().button(IDialogConstants.NEXT_LABEL).click();
		pushDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		SWTBotShell confirm = bot.shell(NLS.bind(UIText.ResultDialog_title,
				destination));
		String result = confirm.bot().tree().getAllItems()[0].getText();

		assertTrue("Wrong result", result.contains(previous.substring(0, 7)));

		confirm.close();

		pushDialog = openPushDialog();

		destinationCombo = pushDialog.bot().comboBox();
		for (int i = 0; i < items.length; i++) {
			if (items[i].startsWith(destination))
				destinationCombo.setSelection(i);
		}

		pushDialog.bot().button(IDialogConstants.NEXT_LABEL).click();
		if (withConfirmPage)
			pushDialog.bot().button(IDialogConstants.NEXT_LABEL).click();
		pushDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		confirm = bot.shell(NLS.bind(UIText.ResultDialog_title, destination));
		result = confirm.bot().tree().getAllItems()[0].getText();

		confirm.close();

		assertTrue("Wrong result",
				result.contains(UIText.PushResultTable_statusUpToDate));
	}

	private SWTBotShell openPushDialog() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util.getPluginLocalizedValue("PushAction_label");
		String submenuString = util
				.getPluginLocalizedValue("RemoteSubMenu.label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				submenuString, menuString);
		SWTBotShell dialog = bot.shell(UIText.PushWizard_windowTitleDefault);
		return dialog;
	}
}

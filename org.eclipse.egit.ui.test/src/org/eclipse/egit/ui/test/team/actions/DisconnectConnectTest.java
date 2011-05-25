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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Fetch and Team->Merge actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class DisconnectConnectTest extends LocalRepositoryTestCase {

	private static SWTBotPerspective perspective;

	@BeforeClass
	public static void setup() throws Exception {
		createProjectAndCommitToRepository();
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		waitInUI();
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	@Test
	public void testDisconnectAndReconnect() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
				PROJ1);
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		assertNotNull(mapping);
		clickOnDisconnect();
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
				IResource.DEPTH_INFINITE, null);
		waitInUI();
		mapping = RepositoryMapping.getMapping(project);
		assertNull(mapping);
		SWTBotShell connectDialog = openConnectDialog();
		// test the "share with repository in parent folder" scenario
		connectDialog.bot()
				.checkBox(UIText.ExistingOrNewPage_InternalModeCheckbox)
				.select();
		connectDialog.bot().tree().getAllItems()[0].select();
		connectDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
				IResource.DEPTH_INFINITE, null);
		waitInUI();
		mapping = RepositoryMapping.getMapping(project);
		assertNotNull(mapping);
	}

	private void clickOnDisconnect() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util
				.getPluginLocalizedValue("DisconnectAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
	}

	private SWTBotShell openConnectDialog() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = "Share Project...";
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
		bot.shell("Share Project").bot().table().getTableItem("Git").select();
		bot.button(IDialogConstants.NEXT_LABEL).click();
		return bot.shell(UIText.SharingWizard_windowTitle);
	}
}

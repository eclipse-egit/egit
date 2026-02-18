/*******************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.share;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.common.ExistingOrNewPage;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.common.SharingWizard;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for sharing projects in nested configurations where parent directories
 * contain .project files. This verifies that EGit correctly handles nested
 * projects, a feature that Eclipse has supported since 2015.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class SharingWizardNestedProjectTest extends LocalRepositoryTestCase {

	private static final String projectName1 = "ChildProject";
	private SharingWizard sharingWizard;
	private File repoFolder;

	@Before
	public void setupViews() {
		TestUtil.showExplorerView();
		sharingWizard = new SharingWizard();
	}

	@After
	public void after() throws Exception {
		// Clean up projects if they exist
		try {
			erase(projectName1);
		} catch (Exception e) {
			// ignore
		}

		if (repoFolder != null && repoFolder.exists()) {
			FileUtils.delete(repoFolder, FileUtils.RECURSIVE);
		}

		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
				IResource.DEPTH_INFINITE, null);
		new Eclipse().reset();
	}

	private void erase(String projectName) {
		// Simplified erase for this test
		try {
			ResourcesPlugin.getWorkspace().getRoot().getProject(projectName)
					.delete(true, true, null);
		} catch (CoreException e) {
			// ignore
		}
	}

	private static void createProject(String projectName) throws CoreException {
		bot.menu("File").menu("New").menu("Project...").click();
		SWTBotShell createProjectDialogShell = bot.shell("New Project");
		SWTBotTreeItem item = bot.tree().getTreeItem("General");
		TestUtil.expandAndWait(item).getNode("Project").select();
		bot.button("Next >").click();

		bot.textWithLabel("Project name:").setText(projectName);

		bot.button("Finish").click();
		// Wait explicitly for the shell to close
		bot.waitUntil(org.eclipse.swtbot.swt.finder.waits.Conditions
				.shellCloses(createProjectDialogShell), 10000);
		ResourcesPlugin.getWorkspace().getRoot()
				.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	@Test
	public void shareProjectIntoRepoWithProjectFile() throws Exception {
		String repoName = "RepoWithProjectFile";
		createProject(projectName1);

		// 1. Open Wizard
		ExistingOrNewPage existingOrNewPage = sharingWizard.openWizard(projectName1);

		// 2. Create External Repository
		SWTBotShell createRepoDialog = existingOrNewPage.clickCreateRepository();
		String repoDir = RepositoryUtil.getDefaultRepositoryDir();
		repoFolder = new File(repoDir, repoName);
		if (repoFolder.exists()) {
			FileUtils.delete(repoFolder, FileUtils.RECURSIVE);
		}
		createRepoDialog.bot()
				.textWithLabel(UIText.CreateRepositoryPage_DirectoryLabel)
				.setText(repoFolder.getAbsolutePath());
		createRepoDialog.bot().button(IDialogConstants.FINISH_LABEL).click();

		// 3. Create a .project file in the repository root to simulate the repo being a project itself
		new File(repoFolder, ".project").createNewFile();

		// 4. Select the repository
		SWTBotCombo combo = bot.comboBoxWithLabel(UIText.ExistingOrNewPage_ExistingRepositoryLabel);
		assertTrue(combo.getText().startsWith(repoName));

		// 5. Verify that the wizard allows sharing (nested project check removed)

		// Wait a bit for UI to update
		bot.sleep(1000);

		// Check if "Finish" is enabled. It should be enabled now.
		boolean finishEnabled = bot.button(IDialogConstants.FINISH_LABEL).isEnabled();
		assertTrue("Finish button should be enabled for nested projects", finishEnabled);

		// Finish the wizard
		SWTBotShell shell = bot.activeShell();
		bot.button(IDialogConstants.FINISH_LABEL).click();
		bot.waitUntil(org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses(shell));

		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);

		// Verify that the project was shared
		String projectLocation = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName1).getLocation().toOSString();
		String expectedLocation = new File(repoFolder, projectName1).getAbsolutePath();
		assertEquals("Project should be moved to the repository", expectedLocation, projectLocation);
	}
}

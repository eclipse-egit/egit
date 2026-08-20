/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Lars Vogel - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the "Projects" menu of the Git Repositories View on a selection of
 * several repositories.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewCloseProjectsTest
		extends GitRepositoriesViewTestBase {

	private static final String SECOND_REPO = "SecondRepository";

	private static final String SECOND_PROJ = "SecondProject";

	private static final String OUTSIDE_PROJ = "OutsideProject";

	@Before
	public void prepare() throws Exception {
		clearView();
		RepositoryUtil.INSTANCE.addConfiguredRepository(
				createProjectAndCommitToRepository(REPO1, PROJ1));
		RepositoryUtil.INSTANCE.addConfiguredRepository(
				createProjectAndCommitToRepository(SECOND_REPO, SECOND_PROJ));
		refreshAndWait();
	}

	@Test
	public void closeProjectsOfMultipleRepositories() throws Exception {
		IProject first = getProject(PROJ1);
		IProject second = getProject(SECOND_PROJ);
		assertTrue(first.isOpen());
		assertTrue(second.isOpen());

		selectBothRepositories();
		clickProjectsMenu("RepoViewCloseAllProjects.label");

		assertFalse(PROJ1 + " should be closed", first.isOpen());
		assertFalse(SECOND_PROJ + " should be closed", second.isOpen());
	}

	@Test
	public void openProjectsOfMultipleRepositories() throws Exception {
		IProject first = getProject(PROJ1);
		IProject second = getProject(SECOND_PROJ);
		first.close(null);
		second.close(null);
		refreshAndWait();
		assertFalse(first.isOpen());
		assertFalse(second.isOpen());

		selectBothRepositories();
		clickProjectsMenu("RepoViewOpenAllProjects.label");

		assertTrue(PROJ1 + " should be open", first.isOpen());
		assertTrue(SECOND_PROJ + " should be open", second.isOpen());
	}

	@Test
	public void closeProjectsOutsideMultipleRepositories() throws Exception {
		IProject first = getProject(PROJ1);
		IProject second = getProject(SECOND_PROJ);
		IProject outside = getProject(OUTSIDE_PROJ);
		outside.create(null);
		outside.open(null);
		refreshAndWait();
		assertTrue(outside.isOpen());

		selectBothRepositories();
		ContextMenuHelper.clickContextMenu(getOrOpenView().bot().tree(),
				myUtil.getPluginLocalizedValue("RepoViewProjectsMenu.label"),
				myUtil.getPluginLocalizedValue(
						"RepoViewCloseProjectsOutsideRepository.label"));
		SWTBotShell confirm = bot.shell(
				UIText.CloseProjectsOutsideRepositoryCommand_confirmTitle);
		confirm.bot().button(IDialogConstants.OK_LABEL).click();
		bot.waitUntil(Conditions.shellCloses(confirm));
		TestUtil.waitForJobs(50, 30000);
		refreshAndWait();

		assertFalse(OUTSIDE_PROJ + " should be closed", outside.isOpen());
		assertTrue(PROJ1 + " should stay open", first.isOpen());
		assertTrue(SECOND_PROJ + " should stay open", second.isOpen());
	}

	private void selectBothRepositories() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		assertEquals(2, tree.getAllItems().length);
		tree.select(0, 1);
	}

	private void clickProjectsMenu(String labelKey) throws Exception {
		ContextMenuHelper.clickContextMenuSync(getOrOpenView().bot().tree(),
				myUtil.getPluginLocalizedValue("RepoViewProjectsMenu.label"),
				myUtil.getPluginLocalizedValue(labelKey));
		TestUtil.waitForJobs(50, 30000);
		refreshAndWait();
	}

	private static IProject getProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}
}

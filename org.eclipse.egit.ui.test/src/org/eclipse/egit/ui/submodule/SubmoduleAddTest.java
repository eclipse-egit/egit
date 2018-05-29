/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.submodule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for adding submodules to a repository
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class SubmoduleAddTest extends GitRepositoriesViewTestBase {

	private static final String ADD_SUBMODULE_CONTEXT_MENU_LABEL = "SubmoduleAddCommand.label";

	private File repositoryFile;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@Test
	public void addAtRoot() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		clearView();
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		shareProjects(repositoryFile);
		assertProjectExistence(PROJ1, true);
		refreshAndWait();
		assertHasRepo(repositoryFile);
		Repository repo = lookupRepository(repositoryFile);

		SWTBotTree tree = getOrOpenView().bot().tree();
		tree.getAllItems()[0].select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue(ADD_SUBMODULE_CONTEXT_MENU_LABEL));
		SWTBotShell shell = bot.shell(UIText.AddSubmoduleWizard_WindowTitle);
		shell.activate();
		shell.bot().textWithLabel(UIText.SubmodulePathWizardPage_PathLabel)
				.setText("sub");
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();

		shell.bot()
				.textWithLabel(UIText.RepositorySelectionPage_promptURI + ":")
				.setText(repo.getDirectory().toURI().toString());

		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		waitInUI();
		TestUtil.joinJobs(JobFamilies.SUBMODULE_ADD);
		refreshAndWait();

		tree = getOrOpenView().bot().tree();
		SWTBotTreeItem submodules = tree.getAllItems()[0].select();
		submodules = TestUtil.expandAndWait(submodules).getNode(
				UIText.RepositoriesViewLabelProvider_SubmodulesNodeText);
		assertNotNull(submodules);
		TestUtil.expandAndWait(submodules);
		assertEquals(1, submodules.rowCount());
	}
}

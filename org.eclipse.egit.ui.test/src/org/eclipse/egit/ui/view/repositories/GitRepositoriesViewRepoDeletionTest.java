/*******************************************************************************
 * Copyright (c) 2012, Matthias Sohn <matthias.sohn@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View (repository deletion)
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewRepoDeletionTest extends
		GitRepositoriesViewTestBase {

	private static final String DELETE_REPOSITORY_CONTEXT_MENU_LABEL = "RepoViewDeleteRepository.label";

	private File repositoryFile;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@Test
	public void testDeleteRepositoryWithContentOk() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		clearView();
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				repositoryFile);
		shareProjects(repositoryFile);
		assertProjectExistence(PROJ1, true);
		refreshAndWait();
		assertHasRepo(repositoryFile);
		SWTBotTree tree = getOrOpenView().bot().tree();
		tree.getAllItems()[0].select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue(DELETE_REPOSITORY_CONTEXT_MENU_LABEL));
		SWTBotShell shell = bot.shell(UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryWindowTitle);
		shell.activate();
		FileRepository repo = lookupRepository(repositoryFile);
		String workDir=repo.getWorkTree().getPath();
		String checkboxLabel = NLS
				.bind(UIText.DeleteRepositoryConfirmDialog_DeleteWorkingDirectoryCheckbox,
						workDir);
		shell.bot().checkBox(checkboxLabel).select();
		shell.bot().button(IDialogConstants.OK_LABEL).click();
		TestUtil.joinJobs(JobFamilies.REPOSITORY_DELETE);

		refreshAndWait();
		assertEmpty();
		assertProjectExistence(PROJ1, false);
		assertFalse(repositoryFile.exists());
	}

	@Test
	public void testDeleteSubmoduleRepository() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		clearView();
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
		shareProjects(repositoryFile);
		assertProjectExistence(PROJ1, true);
		refreshAndWait();
		assertHasRepo(repositoryFile);

		Repository db = lookupRepository(repositoryFile);
		SubmoduleAddCommand command = new SubmoduleAddCommand(db);
		String path = "sub";
		command.setPath(path);
		String uri = db.getDirectory().toURI().toString();
		command.setURI(uri);
		Repository subRepo = command.call();
		assertNotNull(subRepo);

		refreshAndWait();

		SWTBotTree tree = getOrOpenView().bot().tree();
		tree.getAllItems()[0]
				.expand()
				.expandNode(
						UIText.RepositoriesViewLabelProvider_SubmodulesNodeText)
				.getItems()[0].select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue(DELETE_REPOSITORY_CONTEXT_MENU_LABEL));
		SWTBotShell shell = bot
				.shell(UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryWindowTitle);
		shell.activate();
		String workDir = subRepo.getWorkTree().getPath();
		String checkboxLabel = NLS
				.bind(UIText.DeleteRepositoryConfirmDialog_DeleteWorkingDirectoryCheckbox,
						workDir);
		shell.bot().checkBox(checkboxLabel).select();
		shell.bot().button(IDialogConstants.OK_LABEL).click();
		TestUtil.joinJobs(JobFamilies.REPOSITORY_DELETE);

		refreshAndWait();
		assertFalse(subRepo.getDirectory().exists());
		assertFalse(subRepo.getWorkTree().exists());
	}
}

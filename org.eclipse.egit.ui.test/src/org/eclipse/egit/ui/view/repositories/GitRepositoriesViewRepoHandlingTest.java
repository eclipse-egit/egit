/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Laurent Goubet <laurent.goubet@obeo.fr - 404121
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.widgetIsEnabled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View (repository handling)
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewRepoHandlingTest extends
		GitRepositoriesViewTestBase {

	private static final String REMOVE_REPOSITORY_CONTEXT_MENU_LABEL = "RepoViewRemove.label";

	private File repositoryFile;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@Test
	public void testCopyPathToClipboard() throws Exception {
		clearView();
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				repositoryFile);
		refreshAndWait();
		final SWTBotTree tree = getOrOpenView().bot().tree();
		tree.getAllItems()[0].select();
		waitInUI();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				Clipboard clp = new Clipboard(Display.getCurrent());
				clp.clearContents();
				clp.setContents(new Object[] { "x" },
						new TextTransfer[] { TextTransfer.getInstance() });
				String value = (String) clp.getContents(TextTransfer
						.getInstance());
				assertEquals("Clipboard content should be x", "x", value);

				ContextMenuHelper.clickContextMenuSync(tree, myUtil
						.getPluginLocalizedValue("CopyPathCommand"));
				value = (String) clp.getContents(TextTransfer.getInstance());
				assertTrue("Clipboard content (" + value
						+ ")should be a repository path", FileKey
						.isGitRepository(new File(value), FS.DETECTED));

				clp.dispose();
			}
		});

	}

	@Test
	public void testPasteRepoPath() throws Exception {
		clearView();
		refreshAndWait();
		final Exception[] exceptions = new Exception[1];
		final SWTBotLabel label = getOrOpenView().bot().label(
				UIText.RepositoriesView_messsageEmpty);
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				Clipboard clip = null;
				try {
					clip = new Clipboard(Display.getDefault());
					clip.setContents(new Object[] { repositoryFile.getPath() },
							new Transfer[] { TextTransfer.getInstance() });

					ContextMenuHelper.clickContextMenuSync(label,
							myUtil.getPluginLocalizedValue("PastePathCommand"));
				} catch (Exception e) {
					exceptions[0] = e;
				} finally {
					if (clip != null)
						clip.dispose();
				}
			}
		});

		if (exceptions[0] != null)
			throw exceptions[0];
		refreshAndWait();
		assertHasRepo(repositoryFile);
	}

	@Test
	public void testRemoveRepositoryWithoutProjects() throws Exception {
		deleteAllProjects();
		clearView();
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				repositoryFile);
		refreshAndWait();
		assertHasRepo(repositoryFile);
		SWTBotTree tree = getOrOpenView().bot().tree();
		tree.getAllItems()[0].select();
		ContextMenuHelper.clickContextMenuSync(tree, myUtil
				.getPluginLocalizedValue(REMOVE_REPOSITORY_CONTEXT_MENU_LABEL));
		refreshAndWait();
		assertEmpty();
	}

	@Test
	public void testRemoveRepositoryWithProjectsYes() throws Exception {
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
				.getPluginLocalizedValue(REMOVE_REPOSITORY_CONTEXT_MENU_LABEL));
		SWTBotShell shell = bot
				.shell(UIText.RepositoriesView_ConfirmProjectDeletion_WindowTitle);
		shell.bot().button(IDialogConstants.YES_LABEL).click();
		refreshAndWait();
		assertEmpty();
		assertProjectExistence(PROJ1, false);
	}

	@Test
	public void testRemoveRepositoryWithProjectsNo() throws Exception {
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
				.getPluginLocalizedValue(REMOVE_REPOSITORY_CONTEXT_MENU_LABEL));
		SWTBotShell shell = bot
				.shell(UIText.RepositoriesView_ConfirmProjectDeletion_WindowTitle);
		shell.bot().button(IDialogConstants.NO_LABEL).click();
		refreshAndWait();
		assertEmpty();
		assertProjectExistence(PROJ1, true);
	}

	@Test
	public void testRemoveRepositoryWithProjectsCancel() throws Exception {
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
				.getPluginLocalizedValue(REMOVE_REPOSITORY_CONTEXT_MENU_LABEL));
		SWTBotShell shell = bot
				.shell(UIText.RepositoriesView_ConfirmProjectDeletion_WindowTitle);
		shell.bot().button(IDialogConstants.CANCEL_LABEL).click();
		refreshAndWait();
		assertHasRepo(repositoryFile);
		assertProjectExistence(PROJ1, true);
	}

	@Test
	public void testShowIn() throws Exception {
		clearView();
		deleteAllProjects();
		shareProjects(repositoryFile);
		refreshAndWait();
		assertProjectExistence(PROJ1, true);
		assertEmpty();

		getOrOpenView().show();

		SWTBotView view = TestUtil.showExplorerView();
		SWTBotTree explorerTree = view.bot().tree();
		SWTBotTreeItem projectItem = getProjectItem(explorerTree, PROJ1)
				.select();
		ContextMenuHelper.clickContextMenuSync(explorerTree, "Show In",
				viewName);
		refreshAndWait();
		assertHasRepo(repositoryFile);
		SWTBotTree viewerTree = getOrOpenView().bot().tree();

		TableCollection selection = viewerTree.selection();
		assertEquals("Selection should contain one element: " + selection, 1,
				selection.rowCount());
		String nodeText = selection.get(0).get(0);
		assertTrue("Node text should contain project name", projectItem
				.getText().startsWith(nodeText));

		view.show();
		SWTBotTreeItem item = TestUtil.expandAndWait(projectItem);
		item = TestUtil.expandAndWait(item.getNode(FOLDER));
		item.getNode(FILE1).select();

		ContextMenuHelper.clickContextMenuSync(explorerTree, "Show In",
				viewName);

		selection = viewerTree.selection();
		assertTrue("Selection should contain one eelement",
				selection.rowCount() == 1);
		nodeText = selection.get(0).get(0);
			assertEquals("Node text should contain file name", FILE1, nodeText);
	}

	@Test
	public void testAddRepoButton() throws Exception {
		deleteAllProjects();
		clearView();
		refreshAndWait();
		assertEmpty();
		getOrOpenView()
				.toolbarButton(
						myUtil
								.getPluginLocalizedValue("RepoViewAddRepository.tooltip"))
				.click();
		TestUtil.processUIEvents();
		SWTBotShell shell = bot
				.shell(UIText.RepositorySearchDialog_AddGitRepositories);
		shell.bot().textWithLabel(UIText.RepositorySearchDialog_directory)
				.setText(getTestDirectory().getPath());

		assertEquals(0, ModalContext.getModalLevel());

		shell.bot().button(UIText.RepositorySearchDialog_Search).click();
		TestUtil.processUIEvents(500);
		int max = 5000;
		int slept = 0;
		while (ModalContext.getModalLevel() > 0 && slept < max) {
			TestUtil.processUIEvents(100);
			slept += 100;
		}

		shell.activate();
		SWTBotTreeItem item = shell.bot().tree().getAllItems()[0];
		item.check();
		shell.bot().button(UIText.AddCommand_AddButtonLabel).click();
		TestUtil.joinJobs(org.eclipse.egit.core.JobFamilies.AUTO_SHARE);
		refreshAndWait();
		assertHasRepo(repositoryFile);
	}

	@Test
	public void testCloneRepoButton() throws Exception {
		clearView();
		refreshAndWait();
		assertEmpty();
		getOrOpenView()
				.toolbarButton(
						myUtil
								.getPluginLocalizedValue("RepoViewCloneRepository.tooltip"))
				.click();
		SWTBotShell shell = bot.shell(UIText.GitCloneWizard_title);
		shell.bot().tree().select("Clone URI");

		shell.bot().button("Next >").click();		// for some reason, textWithLabel doesn't seem to work
		shell.bot()
				.textInGroup(UIText.RepositorySelectionPage_groupLocation, 0)
				.setText(repositoryFile.getPath());
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		bot.waitUntil(widgetIsEnabled(shell.bot().tree()), 60000);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		waitInUI();
		// for some reason textWithLabel doesn't work; 0 is path text
		SWTBotText pathText = shell.bot().text(0);
		pathText.setText(pathText.getText() + "Cloned");
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		TestUtil.joinJobs(JobFamilies.CLONE);
		refreshAndWait();
		assertHasClonedRepo();
	}

	@Test
	public void testCreateRepository() throws Exception {
		clearView();
		refreshAndWait();
		assertEmpty();
		// create a non-bare repository
		getOrOpenView()
				.toolbarButton(
						myUtil.getPluginLocalizedValue("RepoViewCreateRepository.tooltip"))
				.click();
		SWTBotShell shell = bot.shell(UIText.NewRepositoryWizard_WizardTitle);
		IPath newPath = new Path(getTestDirectory().getPath());
		shell.bot().textWithLabel(UIText.CreateRepositoryPage_DirectoryLabel)
				.setText(newPath.append("NewRepository").toOSString());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();
		File repoFile = new File(newPath.append("NewRepository").toFile(),
				Constants.DOT_GIT);
		myRepoViewUtil.getRootItem(getOrOpenView().bot().tree(), repoFile);
		assertFalse(myRepoViewUtil.lookupRepository(repoFile).isBare());

		// create a bare repository
		getOrOpenView()
				.toolbarButton(
						myUtil.getPluginLocalizedValue("RepoViewCreateRepository.tooltip"))
				.click();
		shell = bot.shell(UIText.NewRepositoryWizard_WizardTitle);
		newPath = new Path(getTestDirectory().getPath()).append("bare").append(
				"NewBareRepository");
		shell.bot().textWithLabel(UIText.CreateRepositoryPage_DirectoryLabel)
				.setText(newPath.toOSString());
		shell.bot().checkBox(UIText.CreateRepositoryPage_BareCheckbox).select();
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();
		repoFile = newPath.toFile();
		myRepoViewUtil.getRootItem(getOrOpenView().bot().tree(), repoFile);
		assertTrue(myRepoViewUtil.lookupRepository(repoFile).isBare());
	}

	@Test
	public void testSearchDirectoryWithBareRepos() throws Exception {
		deleteAllProjects();
		shutDownRepositories();
		clearView();
		refreshAndWait();
		assertEmpty();

		Git.init().setBare(true)
				.setDirectory(new File(getTestDirectory(), "BareRepository1"))
				.call();

		Git.init().setBare(true)
				.setDirectory(new File(getTestDirectory(), "BareRepository2"))
				.call();

		getOrOpenView()
				.toolbarButton(
						myUtil.getPluginLocalizedValue("RepoViewAddRepository.tooltip"))
				.click();

		SWTBotShell shell = bot
				.shell(UIText.RepositorySearchDialog_AddGitRepositories);

		shell.bot().checkBox(UIText.RepositorySearchDialog_DeepSearch_button)
				.deselect();

		shell.bot().textWithLabel(UIText.RepositorySearchDialog_directory)
				.setText(getTestDirectory().getPath());

		assertEquals(0, ModalContext.getModalLevel());

		shell.bot().button(UIText.RepositorySearchDialog_Search).click();
		TestUtil.processUIEvents(500);
		int max = 5000;
		int slept = 0;
		while (ModalContext.getModalLevel() > 0 && slept < max) {
			TestUtil.processUIEvents(100);
			slept += 100;
		}
		shell.activate();
		TestUtil.waitUntilTreeHasNodeContainsText(shell.bot(), shell.bot()
				.tree(), "BareRepository1", 10000);
		TestUtil.waitUntilTreeHasNodeContainsText(shell.bot(), shell.bot()
				.tree(), "BareRepository2", 10000);
	}

	private void assertHasClonedRepo() throws Exception {
		final SWTBotTree tree = getOrOpenView().bot().tree();
		String text = repositoryFile.getParentFile().getName() + "Cloned";
		TestUtil.waitUntilTreeHasNodeContainsText(bot, tree, text, 10000);
	}

}

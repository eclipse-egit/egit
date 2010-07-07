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
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View.
 * <pre>
 * TODO
 * global copy and paste command
 * bare repository support including copy of path from workdir
 * copy path from file and folder
 * paste with empty and invalid path
 * create branch with selection not on a ref
 * tags altogether
 * fetch and push to configured remote
 * import wizard outside the "golden path"
 * </pre>
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewTest extends GitRepositoriesViewTestBase {

	private static File repositoryFile;

	@BeforeClass
	public static void beforeClass() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				repositoryFile);
	}

	/**
	 * First level should have 5 children
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExpandFirstLevel() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = getRootItem(tree, repositoryFile).expand();
		SWTBotTreeItem[] children = item.getItems();
		assertEquals("Wrong number of children", 5, children.length);
	}

	/**
	 * Open (expand, file->editor, branch->checkout)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testOpen() throws Exception {
		// expand first level
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = getRootItem(tree, repositoryFile);
		item.collapse();
		refreshAndWait();
		item = getRootItem(tree, repositoryFile);
		// the number of children does appear to be 1 (with empty text)...
		assertEquals("Wrong number of children", 1, item.getNodes().size());
		item.doubleClick();
		assertEquals("Wrong number of children", 5, item.getNodes().size());
		// open a file in editor
		item = getWorkdirItem(tree, repositoryFile).expand();
		SWTBotTreeItem fileiItem = item.getNode(PROJ1).expand().getNode(FOLDER)
				.expand().getNode(FILE1).select();
		fileiItem.doubleClick();
		assertTrue(bot.activeEditor().getTitle().equals(FILE1));
		bot.activeEditor().close();
		// open a branch (checkout)
		item = getLocalBranchesItem(tree, repositoryFile).expand().getNode(
				"master").doubleClick();
		refreshAndWait();
		String contentMaster = getTestFileContent();
		item = getLocalBranchesItem(tree, repositoryFile).expand().getNode(
				"stable").doubleClick();
		refreshAndWait();
		waitInUI();
		String contentStable = getTestFileContent();
		assertTrue("Content of master and stable should differ", !contentMaster
				.equals(contentStable));
	}

	/**
	 * Checks for the Symbolic Reference node
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExpandSymbolicRef() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = getSymbolicRefsItem(tree, repositoryFile)
				.expand();
		List<String> children = item.getNodes();
		boolean found = false;
		for (String child : children)
			if (child.contains(Constants.HEAD))
				found = true;
		assertTrue(found);
	}

	/**
	 * Checks the first level of the working directory
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExpandWorkDir() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		Repository myRepository = lookupRepository(repositoryFile);
		List<String> children = Arrays.asList(myRepository.getWorkDir().list());
		List<String> treeChildren = getWorkdirItem(tree, repositoryFile)
				.expand().getNodes();
		assertTrue(children.containsAll(treeChildren)
				&& treeChildren.containsAll(children));
		getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ1).expand()
				.getNode(FOLDER).expand().getNode(FILE1);
	}

	/**
	 * Checks is some context menus are available, should be replaced with real
	 * tests
	 * 
	 * @throws Exception
	 */
	@Test
	public void testContextMenuRepository() throws Exception {
		// TODO real tests instead of just context menu tests
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = getRootItem(tree, repositoryFile);
		item.select();
		assertClickOpens(tree, myUtil.getPluginLocalizedValue("FetchCommand"),
				UIText.FetchWizard_windowTitleDefault);
		assertClickOpens(tree, myUtil.getPluginLocalizedValue("PushCommand"),
				UIText.PushWizard_windowTitleDefault);
	}

	/**
	 * Show properties
	 * 
	 * @throws Exception
	 */
	@Test
	public void testShowProperties() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = getRootItem(tree, repositoryFile);
		item.select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("OpenPropertiesCommand"));
		waitInUI();
		assertEquals("org.eclipse.ui.views.PropertySheet", bot.activeView()
				.getReference().getId());
	}

	/**
	 * Import wizard golden path test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testImportWizard() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = getRootItem(tree, repositoryFile);
		String wizardTitle = NLS.bind(
				UIText.GitCreateProjectViaWizardWizard_WizardTitle,
				repositoryFile.getPath());
		// start wizard from root item
		item.select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		SWTBotShell shell = bot.shell(wizardTitle);
		// we can't find the check box, so let's use the keyboard activator
		// to select the import as existing project button
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_ImportExistingButton);
		// auto share
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_AutoShareButton);
		TableCollection selected = shell.bot().tree().selection();
		String wizardNode = selected.get(0, 0);
		// wizard directory should be working dir
		assertEquals(getWorkdirItem(tree, repositoryFile).getText(), wizardNode);
		waitInUI();
		shell.close();
		// start wizard from .git
		getWorkdirItem(tree, repositoryFile).expand()
				.getNode(Constants.DOT_GIT).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		shell = bot.shell(wizardTitle);
		selected = shell.bot().tree().selection();
		wizardNode = selected.get(0, 0);
		// wizard directory should be .git
		assertEquals(Constants.DOT_GIT, wizardNode);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		waitInUI();
		assertTrue(shell.bot().tree().getAllItems().length == 0);
		shell.bot().button(IDialogConstants.BACK_LABEL).click();
		// go to project with .project
		shell.bot().tree().getAllItems()[0].getNode(PROJ1).select();
		// next is 1
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		waitInUI();
		assertTrue(shell.bot().tree().getAllItems().length == 1);
		assertTrue(!shell.bot().button(IDialogConstants.FINISH_LABEL)
				.isEnabled());
		shell.bot().button(UIText.WizardProjectsImportPage_selectAll).click();
		assertTrue(shell.bot().button(IDialogConstants.FINISH_LABEL)
				.isEnabled());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		waitInUI();
		assertProjectExistence(PROJ1, true);
	}

	@Test
	public void testImportWizardGeneralProject() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ2, false);
		SWTBotTree tree = getOrOpenView().bot().tree();
		String wizardTitle = NLS.bind(
				UIText.GitCreateProjectViaWizardWizard_WizardTitle,
				repositoryFile.getPath());
		// start wizard from PROJ2
		getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ2).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		SWTBotShell shell = bot.shell(wizardTitle);
		shell = bot.shell(wizardTitle);
		// try import existing project first
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_ImportExistingButton);
		// auto share
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_AutoShareButton);
		TableCollection selected = shell.bot().tree().selection();
		String wizardNode = selected.get(0, 0);
		// wizard directory should be PROJ2
		assertEquals(PROJ2, wizardNode);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		waitInUI();
		assertTrue(shell.bot().tree().getAllItems().length == 0);
		shell.bot().button(IDialogConstants.BACK_LABEL).click();
		// import as general
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_ImportAsGeneralButton);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		assertEquals(PROJ2, shell.bot().textWithLabel(
				UIText.GitCreateGeneralProjectPage_ProjectNameLabel).getText());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		waitInUI();
		assertProjectExistence(PROJ2, true);
	}

	@Test
	public void testImportWizardGeneralProjectManualShareCancel()
			throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ2, false);
		SWTBotTree tree = getOrOpenView().bot().tree();
		String wizardTitle = NLS.bind(
				UIText.GitCreateProjectViaWizardWizard_WizardTitle,
				repositoryFile.getPath());
		// start wizard from PROJ2
		getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ2).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		SWTBotShell shell = bot.shell(wizardTitle);
		shell = bot.shell(wizardTitle);
		// import as general
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_ImportAsGeneralButton);
		// share manual
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_InteractiveShareButton);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		assertEquals(PROJ2, shell.bot().textWithLabel(
				UIText.GitCreateGeneralProjectPage_ProjectNameLabel).getText());
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		assertEquals(PROJ2, shell.bot().table().getTableItem(0).getText(0));
		// cancel -> not share
		shell.bot().button(IDialogConstants.CANCEL_LABEL).click();
		waitInUI();
		assertProjectExistence(PROJ2, true);
		RepositoryMapping mapping = RepositoryMapping
				.getMapping(ResourcesPlugin.getWorkspace().getRoot()
						.getProject(PROJ2));
		assertNull(mapping);
	}

	@Test
	public void testImportWizardGeneralProjectManualShareOk() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ2, false);
		SWTBotTree tree = getOrOpenView().bot().tree();
		String wizardTitle = NLS.bind(
				UIText.GitCreateProjectViaWizardWizard_WizardTitle,
				repositoryFile.getPath());
		// start wizard from PROJ2
		getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ2).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		SWTBotShell shell = bot.shell(wizardTitle);
		shell = bot.shell(wizardTitle);
		// import as general
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_ImportAsGeneralButton);
		// share manual
		activateItemByKeyboard(shell,
				UIText.GitSelectWizardPage_InteractiveShareButton);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		assertEquals(PROJ2, shell.bot().textWithLabel(
				UIText.GitCreateGeneralProjectPage_ProjectNameLabel).getText());
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		assertEquals(PROJ2, shell.bot().table().getTableItem(0).getText(0));
		// finish -> share
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		waitInUI();
		assertProjectExistence(PROJ2, true);
		RepositoryMapping mapping = RepositoryMapping
				.getMapping(ResourcesPlugin.getWorkspace().getRoot()
						.getProject(PROJ2));
		assertNotNull(mapping);
	}

	@Test
	public void testLinkWithSelection() throws Exception {
		deleteAllProjects();
		shareProjects(repositoryFile);
		SWTBotPerspective perspective = null;
		try {
			perspective = bot.activePerspective();
			bot.perspectiveById("org.eclipse.ui.resourcePerspective")
					.activate();
			SWTBotTree tree = getOrOpenView().bot().tree();
			getRootItem(tree, repositoryFile).select();
			// the selection should be root
			assertTrue(tree.selection().get(0, 0).startsWith(REPO1));

			SWTBotTree projectExplorerTree = bot.viewById(
					"org.eclipse.ui.navigator.ProjectExplorer").bot().tree();
			getProjectItem(projectExplorerTree, PROJ1).select();

			// the selection should be still be root
			assertTrue(tree.selection().get(0, 0).startsWith(REPO1));

			// activate the link with selection
			getOrOpenView().toolbarButton(
					myUtil.getPluginLocalizedValue("LinkWithSelectionCommand"))
					.click();

			// the selection should be still be root
			assertTrue(tree.selection().get(0, 0).startsWith(REPO1));

			// select again the project
			projectExplorerTree = bot.viewById(
					"org.eclipse.ui.navigator.ProjectExplorer").bot().tree();
			getProjectItem(projectExplorerTree, PROJ1).select();

			// the selection should be project
			assertTrue(tree.selection().get(0, 0).equals(PROJ1));

			// deactivate the link with selection
			getOrOpenView().toolbarButton(
					myUtil.getPluginLocalizedValue("LinkWithSelectionCommand"))
					.click();

		} finally {
			if (perspective != null)
				perspective.activate();
		}
	}

	/**
	 * Link with editor, both ways
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLinkWithEditor() throws Exception {
		deleteAllProjects();
		shareProjects(repositoryFile);
		SWTBotPerspective perspective = null;
		try {
			perspective = bot.activePerspective();
			bot.perspectiveById("org.eclipse.ui.resourcePerspective")
					.activate();
			SWTBotTree tree = getOrOpenView().bot().tree();
			getRootItem(tree, repositoryFile).select();
			// the selection should be root
			assertTrue(tree.selection().get(0, 0).startsWith(REPO1));

			SWTBotView view = bot
					.viewById("org.eclipse.ui.navigator.ProjectExplorer");
			SWTBotTree projectExplorerTree = view.bot().tree();

			SWTBotTreeItem item = getProjectItem(projectExplorerTree, PROJ1)
					.expand().getNode(FOLDER).expand().getNode(FILE1);
			view.show();
			item.doubleClick();

			item = getProjectItem(projectExplorerTree, PROJ1).expand().getNode(
					FOLDER).expand().getNode(FILE2);
			view.show();
			item.doubleClick();
			// now we should have two editors

			// the selection should be still be root
			assertTrue(tree.selection().get(0, 0).startsWith(REPO1));

			// activate the link with selection
			getOrOpenView().toolbarButton("Link with Editor").click();
			bot.editorByTitle(FILE2).show();
			waitInUI();
			// the selection should have changed to the latest editor
			assertTrue(tree.selection().get(0, 0).equals(FILE2));

			bot.editorByTitle(FILE1).show();
			waitInUI();
			// selection should have changed
			assertTrue(tree.selection().get(0, 0).equals(FILE1));

			// deactivate the link with editor
			getOrOpenView().toolbarButton("Link with Editor").click();

			bot.editorByTitle(FILE2).show();
			waitInUI();
			// the selection should be still be test.txt
			assertTrue(tree.selection().get(0, 0).equals(FILE1));

			bot.editorByTitle(FILE1).show();

			getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ1)
					.expand().getNode(FOLDER).expand().getNode(FILE2).select();

			// the editor should still be test.txt
			assertEquals(FILE1, bot.activeEditor().getTitle());

			// activate again
			getOrOpenView().toolbarButton("Link with Editor").click();
			// make sure focus is here
			waitInUI();
			getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ1)
					.expand().getNode(FOLDER).expand().getNode(FILE2).select();
			waitInUI();
			assertEquals(FILE2, bot.activeEditor().getTitle());

			getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ1)
					.expand().getNode(FOLDER).expand().getNode(FILE1).select();
			waitInUI();
			assertEquals(FILE1, bot.activeEditor().getTitle());

			// deactivate the link with editor
			getOrOpenView().toolbarButton("Link with Editor").click();

			getWorkdirItem(tree, repositoryFile).expand().getNode(PROJ1)
					.expand().getNode(FOLDER).expand().getNode(FILE2).select();
			waitInUI();
			assertEquals(FILE1, bot.activeEditor().getTitle());

		} finally {
			if (perspective != null)
				perspective.activate();
		}
	}
}
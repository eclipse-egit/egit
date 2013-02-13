/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View.
 *
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
		setVerboseBranchMode(false);
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
		SWTBotTreeItem item = myRepoViewUtil.getRootItem(tree, repositoryFile)
				.expand();
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
		SWTBotTreeItem item = myRepoViewUtil.getRootItem(tree, repositoryFile);
		item.collapse();
		refreshAndWait();
		item = myRepoViewUtil.getRootItem(tree, repositoryFile);
		assertTrue("Item should not be expanded", !item.isExpanded());
		item.doubleClick();
		assertTrue("Item should be expanded", item.isExpanded());
		// open a file in editor
		item = myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand();
		SWTBotTreeItem fileiItem = item.getNode(PROJ1).expand().getNode(FOLDER)
				.expand().getNode(FILE1).select();
		fileiItem.doubleClick();
		assertTrue(bot.activeEditor().getTitle().equals(FILE1));
		bot.activeEditor().close();
		// open a branch (checkout)
		item = myRepoViewUtil.getLocalBranchesItem(tree, repositoryFile)
				.expand().getNode("master").doubleClick();
		refreshAndWait();
		String contentMaster = getTestFileContent();
		item = myRepoViewUtil.getLocalBranchesItem(tree, repositoryFile)
				.expand().getNode("stable").doubleClick();
		refreshAndWait();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
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
		SWTBotTreeItem item = myRepoViewUtil.getSymbolicRefsItem(tree,
				repositoryFile).expand();
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
		List<String> children = Arrays
				.asList(myRepository.getWorkTree().list());
		List<String> treeChildren = myRepoViewUtil.getWorkdirItem(tree,
				repositoryFile).expand().getNodes();
		assertTrue(children.containsAll(treeChildren)
				&& treeChildren.containsAll(children));
		myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand().getNode(
				PROJ1).expand().getNode(FOLDER).expand().getNode(FILE1);
	}

	/**
	 * Checks is some context menus are available, should be replaced with real
	 * tests
	 * 
	 * @throws Exception
	 */
	@Test
	public void testContextMenuRepository() throws Exception {
		// We just check if the dialogs open, the actual commit and import projects
		// is tested elsewhere
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = myRepoViewUtil.getRootItem(tree, repositoryFile);
		item.select();
		assertClickOpens(tree,
				myUtil.getPluginLocalizedValue("RepoViewCommit.label"),
				UIText.CommitDialog_CommitChanges);
		assertClickOpens(tree,
				myUtil.getPluginLocalizedValue("RepoViewImportProjects.label"),
				NLS.bind(UIText.GitCreateProjectViaWizardWizard_WizardTitle,
						repositoryFile));
	}

	/**
	 * Show properties
	 *
	 * @throws Exception
	 */
	@Test
	public void testShowProperties() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem item = myRepoViewUtil.getRootItem(tree, repositoryFile);
		item.select();
		ContextMenuHelper.clickContextMenuSync(tree,
				myUtil.getPluginLocalizedValue("ShowIn"),
				"Properties");
		SWTBotView propertieView = bot.viewById("org.eclipse.ui.views.PropertySheet");
		assertTrue(propertieView.isActive());
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
		SWTBotTreeItem item = myRepoViewUtil.getRootItem(tree, repositoryFile);
		String wizardTitle = NLS.bind(
				UIText.GitCreateProjectViaWizardWizard_WizardTitle,
				repositoryFile.getPath());
		// start wizard from root item
		item.select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		SWTBotShell shell = bot.shell(wizardTitle);
		bot.radio(UIText.GitSelectWizardPage_ImportExistingButton).click();
		TableCollection selected = shell.bot().tree().selection();
		String wizardNode = selected.get(0, 0);
		// wizard directory should be working dir
		assertEquals(myRepoViewUtil.getWorkdirItem(tree, repositoryFile)
				.getText(), wizardNode);
		shell.close();
		// start wizard from .git
		myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand().getNode(
				Constants.DOT_GIT).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		shell = bot.shell(wizardTitle);
		selected = shell.bot().tree().selection();
		wizardNode = selected.get(0, 0);
		// wizard directory should be .git
		assertEquals(Constants.DOT_GIT, wizardNode);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		shell.bot().label("Import Projects"); // wait for import projects page
		assertEquals(0, shell.bot().tree().getAllItems().length);
		shell.bot().button(IDialogConstants.BACK_LABEL).click();
		// go to project with .project
		shell.bot().tree().getAllItems()[0].getNode(PROJ1).select();
		// next is 1
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		bot.button(UIText.WizardProjectsImportPage_deselectAll).click();
		assertEquals(1, shell.bot().tree().getAllItems().length);
		assertTrue(!shell.bot().button(IDialogConstants.FINISH_LABEL)
				.isEnabled());
		shell.bot().button(UIText.WizardProjectsImportPage_selectAll).click();
		assertTrue(shell.bot().button(IDialogConstants.FINISH_LABEL)
				.isEnabled());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		bot.waitUntil(Conditions.shellCloses(shell));
		assertProjectExistence(PROJ1, true);
		assertProjectIsShared(PROJ1, true);
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
		myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand().getNode(
				PROJ2).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		SWTBotShell shell = bot.shell(wizardTitle);
		shell = bot.shell(wizardTitle);
		// try import existing project first
		bot.radio(UIText.GitSelectWizardPage_ImportExistingButton).click();
		TableCollection selected = shell.bot().tree().selection();
		String wizardNode = selected.get(0, 0);
		// wizard directory should be PROJ2
		assertEquals(PROJ2, wizardNode);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		shell.bot().text(" " + UIText.GitProjectsImportPage_NoProjectsMessage);
		assertEquals(0, shell.bot().tree().getAllItems().length);
		shell.bot().button(IDialogConstants.BACK_LABEL).click();
		// import as general
		shell.bot().radio(UIText.GitSelectWizardPage_ImportAsGeneralButton).click();
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		assertEquals(PROJ2, shell.bot().textWithLabel(
				UIText.GitCreateGeneralProjectPage_ProjectNameLabel).getText());
		// switch to a sub directory and see if this is used
		shell.bot().button(IDialogConstants.BACK_LABEL).click();
		shell.bot().tree().getAllItems()[0].expand().getNode(PROJ2).expand()
				.getNode(FOLDER).select();
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		String name = shell.bot().textWithLabel(
				UIText.GitCreateGeneralProjectPage_ProjectNameLabel).getText();
		assertEquals(FOLDER, name);
		shell.bot().button(IDialogConstants.BACK_LABEL).click();
		// switch back to the root directory
		shell.bot().tree().getAllItems()[0].expand().getNode(PROJ2).select();
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		assertEquals(PROJ2, shell.bot().textWithLabel(
				UIText.GitCreateGeneralProjectPage_ProjectNameLabel).getText());

		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		bot.waitUntil(Conditions.shellCloses(shell));
		assertProjectExistence(PROJ2, true);
		assertProjectIsShared(PROJ2, true);
	}

	@Test
	public void testImportWizardGeneralProjectWithWorkingSet() throws Exception {
		deleteAllProjects();
		assertProjectExistence(PROJ1, false);
		String workingSetName = "myWorkingSet";
		removeWorkingSet(workingSetName);
		SWTBotTree tree = getOrOpenView().bot().tree();
		String wizardTitle = NLS.bind(
				UIText.GitCreateProjectViaWizardWizard_WizardTitle,
				repositoryFile.getPath());
		// start wizard from PROJ1
		myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand().getNode(
				PROJ1).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ImportProjectsCommand"));
		SWTBotShell shell = bot.shell(wizardTitle);
		shell = bot.shell(wizardTitle);
		// try import existing project first
		bot.radio(UIText.GitSelectWizardPage_ImportExistingButton).click();
		SWTBotButton button = shell.bot().button(IDialogConstants.NEXT_LABEL);
		// Set focus on the next button. If this is not done, Wizard Framework restores
		// the focus to the "Import as &General Project" radio button. Setting the focus on
		// the radio button selects the button and causes the test to fail.
		// See also SWTBot Bug 337465
		button.setFocus();
		button.click();
		shell.bot().text(UIText.WizardProjectsImportPage_ImportProjectsDescription);
		shell.bot().tree().getAllItems()[0].check();
		// add to working set
		shell.bot().checkBox("Add project to working sets").select();
		// create new working set
		shell.bot().button("Select...").click();
		SWTBotShell workingSetDialog = bot.shell("Select Working Sets");
		workingSetDialog.bot().button("New...").click();
		SWTBotShell newDialog = bot.shell("New Working Set");
		newDialog.bot().table().select("Java");
		newDialog.bot().button(IDialogConstants.NEXT_LABEL).click();
		newDialog.bot().text(0).setText(workingSetName);
		newDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		workingSetDialog.bot().table().getTableItem(workingSetName).check();
		workingSetDialog.bot().button(IDialogConstants.OK_LABEL).click();
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		bot.waitUntil(Conditions.shellCloses(shell));
		assertProjectExistence(PROJ1, true);
		assertProjectInWorkingSet(workingSetName, PROJ1);
		assertProjectIsShared(PROJ1, true);
		removeWorkingSet(workingSetName);
	}

	private void assertProjectInWorkingSet(String workingSetName,
			String projectName) {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
				.getWorkingSetManager();
		IWorkingSet workingSet = workingSetManager
				.getWorkingSet(workingSetName);
		IAdaptable[] elements = workingSet.getElements();
		assertEquals("Wrong number of projects in working set", 1,
				elements.length);
		IProject project = (IProject) elements[0].getAdapter(IProject.class);
		assertEquals("Wrong project in working set", projectName, project
				.getName());
	}

	private void removeWorkingSet(String name) {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
				.getWorkingSetManager();
		IWorkingSet workingSet = workingSetManager.getWorkingSet(name);
		if (workingSet != null)
			workingSetManager.removeWorkingSet(workingSet);
	}

	private void assertProjectIsShared(String projectName,
			boolean shouldBeShared) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
				projectName);
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (shouldBeShared) {
			assertNotNull(mapping);
			assertNotNull(mapping.getRepository());
		} else
			assertNull(mapping);
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
			myRepoViewUtil.getRootItem(tree, repositoryFile).select();
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
			myRepoViewUtil.getRootItem(tree, repositoryFile).select();
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
			// the selection should have changed to the latest editor
			TestUtil.waitUntilTreeHasSelectedNodeWithText(bot, tree, FILE2, 10000);

			bot.editorByTitle(FILE1).show();
			// selection should have changed
			TestUtil.waitUntilTreeHasSelectedNodeWithText(bot, tree, FILE1, 10000);

			// deactivate the link with editor
			getOrOpenView().toolbarButton("Link with Editor").click();

			bot.editorByTitle(FILE2).show();
			// the selection should be still be test.txt
			TestUtil.waitUntilTreeHasSelectedNodeWithText(bot, tree, FILE1, 10000);

			bot.editorByTitle(FILE1).show();

			myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand()
					.getNode(PROJ1).expand().getNode(FOLDER).expand().getNode(
							FILE2).select();

			// the editor should still be test.txt
			assertEquals(FILE1, bot.activeEditor().getTitle());

			// activate again
			SWTBotView repoView = getOrOpenView();
			repoView.toolbarButton("Link with Editor").click();
			// make sure focus is here
			// tried to remove this waitInUI but failed.
			// tried setting focus, waiting for focus, joining RepositoriesView
			// refresh job
			waitInUI();
			myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand()
					.getNode(PROJ1).expand().getNode(FOLDER).expand().getNode(
							FILE2).select();
			TestUtil.waitUntilEditorIsActive(bot, bot.editorByTitle(FILE2), 10000);

			myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand()
					.getNode(PROJ1).expand().getNode(FOLDER).expand().getNode(
							FILE1).select();
			TestUtil.waitUntilEditorIsActive(bot, bot.editorByTitle(FILE1), 10000);

			// deactivate the link with editor
			getOrOpenView().toolbarButton("Link with Editor").click();

			myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand()
					.getNode(PROJ1).expand().getNode(FOLDER).expand().getNode(
							FILE2).select();
			TestUtil.waitUntilEditorIsActive(bot, bot.editorByTitle(FILE1), 10000);

		} finally {
			if (perspective != null)
				perspective.activate();
		}
	}

	@Test
	public void testDeleteSingleBranch() throws Exception {
		// expand first level
		SWTBotTree tree = getOrOpenView().bot().tree();
		refreshAndWait();
		// create a branch (no checkout)
		SWTBotTreeItem localBranchesItem = myRepoViewUtil.getLocalBranchesItem(
				tree, repositoryFile).expand();
		SWTBotTreeItem masterNode = localBranchesItem.getNode("master");
		masterNode.select();
		ContextMenuHelper.clickContextMenuSync(tree, myUtil
				.getPluginLocalizedValue("RepoViewCheckout.label"));
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RepoViewCreateBranch.label"));
		SWTBotShell createBranchShell = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		createBranchShell.bot().textWithId("BranchName").setText("abc");
		createBranchShell.bot().checkBox().deselect();
		createBranchShell.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();
		// delete branch
		// lookup node again. Widget might have changed due to refresh
		localBranchesItem = myRepoViewUtil.getLocalBranchesItem(
				tree, repositoryFile).expand();
		localBranchesItem.getNode("abc").select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RepoViewDeleteBranch.label"));

		refreshAndWait();
		SWTBotTreeItem[] items = myRepoViewUtil.getLocalBranchesItem(tree,
				repositoryFile).getItems();
		assertEquals("Wrong number of branches", 2, items.length);
		assertEquals("master", items[0].getText());
		assertEquals("stable", items[1].getText());
	}

	@Test
	public void testDeleteMultipleBranches() throws Exception {
		// expand first level
		SWTBotTree tree = getOrOpenView().bot().tree();
		refreshAndWait();
		// open a branch (checkout)
		SWTBotTreeItem localBranchesItem = myRepoViewUtil.getLocalBranchesItem(
				tree, repositoryFile).expand();
		SWTBotTreeItem masterNode = localBranchesItem.getNode("master");
		// create first branch (abc)
		masterNode.select();
		ContextMenuHelper.clickContextMenu(tree, "Create Branch...");
		SWTBotShell createBranchShell = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		createBranchShell.bot().textWithId("BranchName").setText("abc");
		createBranchShell.bot().checkBox().deselect();
		createBranchShell.bot().button(IDialogConstants.FINISH_LABEL).click();
		// create second branch (123)
		ContextMenuHelper.clickContextMenu(tree, "Create Branch...");
		createBranchShell = bot.shell(UIText.CreateBranchWizard_NewBranchTitle);
		createBranchShell.bot().textWithId("BranchName").setText("123");
		createBranchShell.bot().checkBox().deselect();
		createBranchShell.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();
		localBranchesItem = myRepoViewUtil.getLocalBranchesItem(tree,
				repositoryFile).expand();
		// delete both
		localBranchesItem.select("abc", "123");
		ContextMenuHelper.clickContextMenuSync(tree, myUtil
				.getPluginLocalizedValue("RepoViewDeleteBranch.label"));
		refreshAndWait();

		SWTBotTreeItem[] items = myRepoViewUtil.getLocalBranchesItem(tree,
				repositoryFile).getItems();
		assertEquals("Wrong number of branches", 2, items.length);
		assertEquals("master", items[0].getText());
		assertEquals("stable", items[1].getText());
	}

	@Test
	public void testDeleteFileInProject() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		refreshAndWait();

		IProject project1 = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1);
		// Make sure that the refresh doesn't happen on delete and cause a timeout
		project1.refreshLocal(IResource.DEPTH_INFINITE, null);

		SWTBotTreeItem folder = findWorkdirNode(tree, PROJ1, FOLDER);
		folder.getNode(FILE1).select();

		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("RepoViewDeleteFile.label"));

		SWTBotShell confirm = bot.shell("Delete Resources");
		confirm.bot().button(IDialogConstants.OK_LABEL).click();
		bot.waitUntil(shellCloses(confirm));
		TestUtil.joinJobs(JobFamilies.REPO_VIEW_REFRESH);

		assertThat(folder.getNodes(), not(hasItem(FILE1)));
		assertThat(folder.getNodes(), hasItem(FILE2));
	}

	@Test
	public void testDeleteFileNotInProject() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		refreshAndWait();

		SWTBotTreeItem folder = findWorkdirNode(tree, PROJ2, FOLDER);
		folder.getNode(FILE1).select();

		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("RepoViewDeleteFile.label"));

		SWTBotShell confirm = bot.shell(UIText.DeleteResourcesOperationUI_confirmActionTitle);
		confirm.bot().button(IDialogConstants.OK_LABEL).click();
		bot.waitUntil(shellCloses(confirm));
		TestUtil.joinJobs(JobFamilies.REPO_VIEW_REFRESH);

		folder = findWorkdirNode(tree, PROJ2, FOLDER);
		assertThat(folder.getNodes(), not(hasItem(FILE1)));
		assertThat(folder.getNodes(), hasItem(FILE2));
	}

	private SWTBotTreeItem findWorkdirNode(SWTBotTree tree, String... nodes) throws Exception {
		SWTBotTreeItem item = myRepoViewUtil.getWorkdirItem(tree, repositoryFile).expand();
		for (String node : nodes)
			item = item.getNode(node).expand();
		return item;
	}
}
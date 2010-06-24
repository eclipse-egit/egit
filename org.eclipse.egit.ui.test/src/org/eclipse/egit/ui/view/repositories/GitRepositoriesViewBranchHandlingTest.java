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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View (mainly branch operations)
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewBranchHandlingTest extends
		GitRepositoriesViewTestBase {

	private static File repositoryFile;

	private static File remoteRepositoryFile;

	private static File clonedRepositoryFile;

	@BeforeClass
	public static void beforeClass() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		remoteRepositoryFile = createRemoteRepository(repositoryFile);
		waitInUI();
		// now let's clone the remote repository
		final URIish uri = new URIish(remoteRepositoryFile.getPath());
		final File workdir = new File(testDirectory, "Cloned");

		CloneOperation op = new CloneOperation(uri, true, null, workdir,
				"refs/heads/master", "origin");
		op.run(null);

		clonedRepositoryFile = new File(workdir, Constants.DOT_GIT);
	}

	@Before
	public void before() throws Exception {
		clearView();
		deleteAllProjects();
	}

	/**
	 * Create/checkout/delete a local branch
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreateCheckoutDeleteLocalBranch() throws Exception {
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				repositoryFile);
		refreshAndWait();
		final SWTBotView view = getOrOpenView();
		SWTBotTreeItem localItem = getLocalBranchesItem(view.bot().tree(),
				repositoryFile);
		localItem.expand();
		assertEquals("Wrong number of children", 1, localItem.getNodes().size());

		assertEquals("master", localItem.getNodes().get(0));
		localItem.getNode(0).select();

		ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CreateBranchCommand"));

		SWTBotShell createPage = bot
				.shell(UIText.RepositoriesView_NewBranchTitle);
		createPage.activate();
		// getting text with label doesn't work
		createPage.bot().text(1).setText("newLocal");
		createPage.bot().checkBox(UIText.CreateBranchPage_CheckoutButton)
				.deselect();
		createPage.bot().button(IDialogConstants.FINISH_LABEL).click();
		getOrOpenView().toolbarButton("Refresh").click();
		refreshAndWait();

		localItem = getLocalBranchesItem(view.bot().tree(), repositoryFile);
		localItem.expand();
		assertEquals("Wrong number of children", 2, localItem.getNodes().size());

		localItem.getNode(0).select();
		try {
			ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
					.getPluginLocalizedValue("CheckoutCommand"));
		} catch (WidgetNotFoundException e1) {
			// expected
		}
		localItem.getNode(1).select();
		ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CheckoutCommand"));

		try {
			ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
					.getPluginLocalizedValue("CheckoutCommand"));
		} catch (WidgetNotFoundException e) {
			// expected
		}

		localItem.getNode(0).select();
		ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CheckoutCommand"));
		localItem.getNode(1).select();

		ContextMenuHelper.clickContextMenu(bot.tree(), myUtil
				.getPluginLocalizedValue("DeleteBranchCommand"));
		SWTBotShell confirmPopup = bot
				.shell(UIText.RepositoriesView_ConfirmDeleteTitle);
		confirmPopup.activate();
		confirmPopup.bot().button(IDialogConstants.OK_LABEL).click();
		refreshAndWait();
		localItem = getLocalBranchesItem(view.bot().tree(), repositoryFile);
		localItem.expand();
		assertEquals("Wrong number of children", 1, localItem.getNodes().size());
	}

	/**
	 * Checks for the remote branches and creates a local one based on the
	 * "stable" remote
	 *
	 * @throws Exception
	 */
	@Test
	public void testClonedRepository() throws Exception {
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				clonedRepositoryFile);
		refreshAndWait();

		SWTBotTree tree = getOrOpenView().bot().tree();

		SWTBotTreeItem item = getLocalBranchesItem(tree, clonedRepositoryFile)
				.expand();

		List<String> children = item.getNodes();
		assertEquals("Wrong number of local children", 1, children.size());

		item = getRemoteBranchesItem(tree, clonedRepositoryFile).expand();
		children = item.getNodes();
		assertEquals("Wrong number of children", 2, children.size());
		assertTrue("Missing remote branch", children.contains("origin/master"));
		assertTrue("Missing remote branch", children.contains("origin/stable"));
		item.getNode("origin/stable").select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("CreateBranchCommand"));
		SWTBotShell shell = bot.shell(UIText.RepositoriesView_NewBranchTitle);
		shell.activate();
		assertEquals("stable", shell.bot().text(1).getText());
		shell.bot().button(0).click();
		refreshAndWait();
		item = getLocalBranchesItem(tree, clonedRepositoryFile).expand();

		children = item.getNodes();
		assertEquals("Wrong number of local children", 2, children.size());
	}

	/**
	 * Tests checkout of remote branches and project label decoration
	 *
	 * @throws Exception
	 */
	@Test
	public void testCheckoutRemote() throws Exception {
		SWTBotPerspective perspective = null;
		try {
			perspective = bot.activePerspective();
			bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
			clearView();
			Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
					clonedRepositoryFile);
			shareProjects(clonedRepositoryFile);
			refreshAndWait();

			SWTBotTree tree = getOrOpenView().bot().tree();

			SWTBotTreeItem item = getLocalBranchesItem(tree,
					clonedRepositoryFile).expand();

			List<String> children = item.getNodes();
			assertEquals("Wrong number of local children", 2, children.size());

			// make sure to checkout master
			item.getNode(0).select();
			ContextMenuHelper.clickContextMenu(tree, myUtil
					.getPluginLocalizedValue("CheckoutCommand"));
			refreshAndWait();
			touchAndSubmit();
			refreshAndWait();

			item = getRemoteBranchesItem(tree, clonedRepositoryFile).expand();
			children = item.getNodes();
			assertEquals("Wrong number of remote children", 2, children.size());

			item.getNode("origin/stable").select();
			ContextMenuHelper.clickContextMenu(tree, myUtil
					.getPluginLocalizedValue("CheckoutCommand"));
			refreshAndWait();

			SWTBotTree projectExplorerTree = bot.viewById(
					"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
			SWTBotTreeItem projectItem = getProjectItem(projectExplorerTree, PROJ1).select();
			waitInUI();
			assertTrue("Wrong project label decoration", projectItem.getText()
					.contains("refs"));
			// TODO Bug 315166 prevents this from properly working, but then it
			// should go:
			// assertTrue(projectItem.getText().contains(
			// tree.selection().get(0, 0)));

			// now let's try to create a local branch from the remote one
			item = getRemoteBranchesItem(tree, clonedRepositoryFile).expand();
			item.getNode("origin/stable").select();
			ContextMenuHelper.clickContextMenu(tree, myUtil
					.getPluginLocalizedValue("CreateBranchCommand"));

			SWTBotShell createPage = bot
					.shell(UIText.RepositoriesView_NewBranchTitle);
			createPage.activate();
			assertEquals("Wrong suggested branch name", "stable", createPage
					.bot().text(1).getText());
			createPage.close();

		} finally {
			if (perspective != null)
				perspective.activate();
		}

	}
}

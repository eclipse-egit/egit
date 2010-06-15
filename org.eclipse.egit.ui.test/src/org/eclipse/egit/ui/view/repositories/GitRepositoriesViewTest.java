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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewTest {

	private static final String PRJ_NAME = "ImportProjectsTest";

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	private SWTBotView viewbot;

	private static IProject myProject;

	private final static TestUtil myUtil = TestUtil.getInstance();

	private static String viewName;

	private static String gitCategory;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// the show in context menu does not appear in the project explorer for
		// general projects
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		EGitTestCase.closeWelcomePage();
		myProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
				PRJ_NAME);
		if (myProject.exists())
			myProject.delete(true, null);
		myProject.create(null);
		myProject.open(null);

		IFolder folder = myProject.getFolder("folder");
		folder.create(false, true, null);
		folder.getFile("test.txt").create(
				new ByteArrayInputStream("Hello, world".getBytes("UTF-8")),
				false, null);

		File dirFile = myProject.getLocation().append(".git").toFile();
		Repository repo = new Repository(dirFile);
		repo.create();

		new ConnectProviderOperation(myProject, dirFile).execute(null);
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				dirFile);

		viewName = myUtil.getPluginLocalizedValue("GitRepositoriesView_name");
		gitCategory = myUtil.getPluginLocalizedValue("GitCategory_name");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		myProject.delete(true, null);
	}

	@Test
	public void testOpenView() throws Exception {
		getOrOpenView();
	}

	@Test
	@Ignore
	// TODO currently, this does not work if a refresh is currently running ->
	// fix RepositoriesView
	public void testShowIn() throws Exception {
		SWTBotTree tree = bot.viewById("org.eclipse.jdt.ui.PackageExplorer")
				.bot().tree();
		tree.getAllItems()[0].select();
		ContextMenuHelper.clickContextMenu(tree, "Show In", viewName);

		SWTBotTree viewerTree = getOrOpenView().bot().tree();

		TableCollection selection = viewerTree.selection();
		assertTrue("Selection should contain one eelement", selection
				.rowCount() == 1);
		String nodeText = selection.get(0).get(0);
		assertTrue("Node text should contain project name", nodeText
				.contains(myProject.getName()));

		tree.select(tree.getAllItems()[0].expand().getNode("folder").expand()
				.getNode("test.txt"));

		ContextMenuHelper.clickContextMenu(tree, "Show In", viewName);

		selection = viewerTree.selection();
		assertTrue("Selection should contain one eelement", selection
				.rowCount() == 1);
		nodeText = selection.get(0).get(0);
		assertTrue("Node text should contain file name", nodeText
				.contains("test.txt"));
	}

	@Test
	@Ignore
	// TODO this consistently fails with a "Widget disposed" SWT Exception
	// if run in the AllTests test suite, but consistently works
	// if this test class is run alone -> investigate
	public void testOpenFirstLevel() throws Exception {
		final SWTBotView view = getOrOpenView();
		final SWTBotTreeItem[] items = view.bot().tree().getAllItems();
		items[0].expand();
		SWTBotTreeItem[] children;

		children = items[0].getItems();
		assertEquals("Wrong number of children", 5, children.length);
	}

	@Test
	public void testHasRepo() throws Exception {
		final SWTBotView view = getOrOpenView();
		final SWTBotTreeItem[] items = view.bot().tree().getAllItems();
		boolean found = false;
		for (SWTBotTreeItem item : items) {
			if (item.getText().startsWith(PRJ_NAME)) {
				found = true;
				break;
			}
		}
		assertTrue("Tree should have item with correct text", found);
	}

	@Test
	public void testCopyPathToClipboard() throws Exception {
		final SWTBotView view = getOrOpenView();
		final SWTBotTreeItem[] items = view.bot().tree().getAllItems();
		items[0].select();
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				Clipboard clp = new Clipboard(Display.getCurrent());
				clp.clearContents();
				clp.setContents(new Object[] { "x" },
						new TextTransfer[] { TextTransfer.getInstance() });
				String value = (String) clp.getContents(TextTransfer
						.getInstance());
				assertEquals("Clipboard content should be x", "x", value);

				ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
						.getPluginLocalizedValue("CopyPathCommand"));

				value = (String) clp.getContents(TextTransfer.getInstance());
				assertTrue("Clipboard content should be a repository path",
						FileKey.isGitRepository(new File(value), FS.DETECTED));

				clp.dispose();
			}
		});

	}

	@Test
	public void testAddRepoButton() throws Exception {
		getOrOpenView().toolbarButton(
				myUtil.getPluginLocalizedValue("AddRepositoryCommand")).click();
		SWTBotShell shell = bot.shell(
				UIText.RepositorySearchDialog_AddGitRepositories).activate();
		shell.close();
	}

	@Test
	public void testCloneRepoButton() throws Exception {
		getOrOpenView().toolbarButton(
				myUtil.getPluginLocalizedValue("CloneRepositoryCommand"))
				.click();
		SWTBotShell shell = bot.shell(UIText.GitCloneWizard_title).activate();
		shell.close();
	}

	private SWTBotView getOrOpenView() throws Exception {
		if (viewbot == null) {
			bot.menu("Window").menu("Show View").menu("Other...").click();
			SWTBotShell shell = bot.shell("Show View").activate();
			shell.bot().tree().expandNode(gitCategory).getNode(viewName)
					.select();
			shell.bot().button(0).click();

			viewbot = bot.viewByTitle(viewName);

			assertNotNull("Repositories View should not be null", viewbot);
		}
		return viewbot;
	}

	@Test
	@Ignore
	public void testLinkWithSelection() throws Exception {
		// TODO implement
	}

	@Test
	@Ignore
	public void testCollapseAll() throws Exception {
		// TODO implement
	}
}


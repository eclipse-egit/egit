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

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.tree.ErrorNode;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

/**
 * SWTBot Tests for remotes handling
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewRemoteHandlingTest extends
		GitRepositoriesViewTestBase {

	private static File repositoryFile;

	private static File remoteRepositoryFile;

	@BeforeClass
	public static void beforeClass() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		remoteRepositoryFile = createRemoteRepository(repositoryFile);
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				repositoryFile);
	}

	/**
	 * Verify that remote configuration is shown correctly; also check error
	 * node display
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testExpandRemotes() throws Exception {
		removeRemotesConfig(repositoryFile);
		refreshAndWait();
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem remotesItem = getRemotesItem(tree, repositoryFile)
				.expand();
		assertEquals("Wrong number of remotes", 0, remotesItem.getNodes()
				.size());
		FileBasedConfig cfg = lookupRepository(repositoryFile).getConfig();
		String remoteUri = "file:///" + remoteRepositoryFile.getPath();

		cfg.setString("remote", "test", "url", remoteUri);
		cfg.setString("remote", "test", "fetch", "somejunk");
		cfg.setString("remote", "test2", "url", remoteUri);
		cfg.setString("remote", "test2", "fetch", "somejunk");
		cfg.setString("remote", "test2", "pushurl", remoteUri);
		cfg.setString("remote", "test2", "push", "somejunk");
		cfg.setString("remote", "test3", "pushurl", "somejunk");
		cfg.setString("remote", "test3", "push", "somejunk");
		cfg.save();
		cfg.load();
		refreshAndWait();
		remotesItem = getRemotesItem(tree, repositoryFile).expand();
		assertEquals("Wrong number of remotes", 3, remotesItem.getNodes()
				.size());

		remotesItem = getRemotesItem(tree, repositoryFile).expand();
		List<String> testnodes = remotesItem.getNode("test").expand()
				.getNodes();
		assertTrue(testnodes.size() == 1);
		List<String> test2nodes = remotesItem.getNode("test2").expand()
				.getNodes();
		assertTrue(test2nodes.size() == 2);
		// error node should be shown
		remotesItem.getNode("test3").expand().getNodes();
		assertTrue(remotesItem.getNode("test3").expand().getNodes().size() == 1);
		final SWTBotTreeItem errorItem = remotesItem.getNode("test3")
				.getNode(0);
		// check that we see an error node
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Object data = errorItem.widget.getData();
				assertTrue(data instanceof ErrorNode);
			}
		});

		removeRemotesConfig(repositoryFile);
		refreshAndWait();
		remotesItem = getRemotesItem(tree, repositoryFile).expand();
		assertEquals("Wrong number of remotes", 0, remotesItem.getNodes()
				.size());
	}

	/**
	 * Remote configuration scenarios
	 * @throws Exception
	 */
	@Test
	@Ignore // TODO does not compile currently
	public void testConfigureRemote() throws Exception {
//		removeRemotesConfig(repositoryFile);
//		refreshAndWait();
//		SWTBotTree tree = getOrOpenView().bot().tree();
//		SWTBotTreeItem remotesItem = getRemotesItem(tree, repositoryFile)
//				.expand();
//
//		remotesItem = getRemotesItem(tree, repositoryFile).expand();
//		remotesItem.select();
//		ContextMenuHelper.clickContextMenu(tree, myUtil
//				.getPluginLocalizedValue("NewRemoteCommand"));
//		SWTBotShell shell = bot
//				.shell(UIText.ConfigureRemoteWizard_WizardTitle_New);
//		shell.bot().textWithLabel(UIText.SelectRemoteNamePage_RemoteNameLabel)
//				.setText("testRemote");
//		// for some reason the label-based widget getters don't work
//		// configure fetch is 0
//		shell.bot().checkBox(0).select();
//		// configure push is 0
//		shell.bot().checkBox(1).select();
//		// next is 1
//		shell.bot().button(1).click();
//		// change is 0
//		shell.bot().button(0).click();
//		shell = bot.shell(UIText.SelectUriWiazrd_Title);
//		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
//		// finish is 1
//		shell.bot().button(1).click();
//		shell = bot.shell(UIText.ConfigureRemoteWizard_WizardTitle_New);
//		// next is 2
//		shell.bot().button(2).click();
//		// add all branches spec is 2
//		shell.bot().button(2).click();
//		// next is 7
//		shell.bot().button(7).click();
//		// the URIish-like path
//		String testString = new org.eclipse.jgit.transport.URIish("file:///"
//				+ remoteRepositoryFile.getPath()).toPrivateString();
//		assertEquals(testString, shell.bot().text().getText());
//		// add is 0
//		shell.bot().toolbarButton(0).click();
//		shell = bot.shell(UIText.SelectUriWiazrd_Title);
//		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
//		// finish is 1
//		shell.bot().button(1).click();
//		// duplicate
//		shell = bot.shell(UIText.ConfigureUriPage_DuplicateUriTitle);
//		shell.close();
//		shell = bot.shell(UIText.ConfigureRemoteWizard_WizardTitle_New);
//		shell.bot().button(1).click();
//		// add all branches spec is 3
//		shell.bot().button(3).click();
//		// finish i 9
//		shell.bot().button(9).click();
//		refreshAndWait();
//		// assert 2 children
//		SWTBotTreeItem item = getRemotesItem(tree, repositoryFile).expand()
//				.getNode("testRemote").expand();
//		List<String> children = item.getNodes();
//		assertTrue(children.size() == 2);
//		item.getNode(0).select();
//		ContextMenuHelper.clickContextMenu(tree, myUtil
//				.getPluginLocalizedValue("RemoveFetchCommand"));
//		refreshAndWait();
//		// assert 1 children
//		item = getRemotesItem(tree, repositoryFile).expand().getNode(
//				"testRemote").expand();
//		children = item.getNodes();
//		assertTrue(children.size() == 1);
//		item.getNode(0).select();
//		ContextMenuHelper.clickContextMenu(tree, myUtil
//				.getPluginLocalizedValue("RemovePushCommand"));
//		refreshAndWait();
//		// assert 0 children
//		item = getRemotesItem(tree, repositoryFile).expand().getNode(
//				"testRemote").expand();
//		children = item.getNodes();
//		assertTrue(children.size() == 0);
//
//		getRemotesItem(tree, repositoryFile).expand().getNode("testRemote")
//				.select();
//		String shellText = NLS.bind(
//				UIText.ConfigureRemoteWizard_WizardTitle_Change, "testRemote");
//
//		ContextMenuHelper.clickContextMenu(tree, myUtil
//				.getPluginLocalizedValue("ConfigureFetchCommand"));
//		shell = bot.shell(shellText);
//		// change is 0
//		shell.bot().button(0).click();
//		shell = bot.shell(UIText.SelectUriWiazrd_Title);
//		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
//		// finish is 1
//		shell.bot().button(1).click();
//		shell = bot.shell(shellText);
//		// next is 2
//		shell.bot().button(2).click();
//		// add all branches spec is 2
//		shell.bot().button(2).click();
//		// finish is 8
//		shell.bot().button(8).click();
//		refreshAndWait();
//		// assert 1 children
//		item = getRemotesItem(tree, repositoryFile).expand().getNode(
//				"testRemote").expand();
//		children = item.getNodes();
//		assertTrue(children.size() == 1);
//
//		ContextMenuHelper.clickContextMenu(tree, myUtil
//				.getPluginLocalizedValue("ConfigurePushCommand"));
//		shell = bot.shell(shellText);
//
//		shell.bot().toolbarButton(0).click();
//		shell = bot.shell(UIText.SelectUriWiazrd_Title);
//		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
//		// finish is 1
//		shell.bot().button(1).click();
//		shell = bot.shell(shellText);
//		shell.bot().button(1).click();
//		// add all branches spec is 3
//		shell.bot().button(3).click();
//		// finish i 9
//		shell.bot().button(9).click();
//		refreshAndWait();
//		// assert 2 children
//		item = getRemotesItem(tree, repositoryFile).expand().getNode(
//				"testRemote").expand();
//		children = item.getNodes();
//		assertTrue(children.size() == 2);
//		item.select();
//		ContextMenuHelper.clickContextMenu(tree, myUtil
//				.getPluginLocalizedValue("RemoveRemoteCommand"));
//		shell = bot.shell(UIText.RepositoriesView_ConfirmDeleteRemoteHeader);
//		shell.bot().button(1).click();
//
//		refreshAndWait();
//		// assert 2 children
//		item = getRemotesItem(tree, repositoryFile).expand().getNode(
//				"testRemote").expand();
//		children = item.getNodes();
//		assertTrue(children.size() == 2);
//
//		ContextMenuHelper.clickContextMenu(tree, myUtil
//				.getPluginLocalizedValue("RemoveRemoteCommand"));
//		bot.shell(UIText.RepositoriesView_ConfirmDeleteRemoteHeader).bot()
//				.button(0).click();
//		refreshAndWait();
//		assertTrue(getRemotesItem(tree, repositoryFile).getNodes().isEmpty());
	}

	private void removeRemotesConfig(File file) throws Exception {
		FileRepository repo = lookupRepository(file);
		FileBasedConfig config = repo.getConfig();
		for (String remote : config.getSubsections("remote"))
			config.unsetSection("remote", remote);
		config.save();
		waitInUI();
	}
}

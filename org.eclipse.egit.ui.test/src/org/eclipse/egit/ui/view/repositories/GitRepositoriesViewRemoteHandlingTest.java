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

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.ErrorNode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExpandRemotes() throws Exception {
		removeRemotesConfig(repositoryFile);
		refreshAndWait();
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem remotesItem = myRepoViewUtil.getRemotesItem(tree,
				repositoryFile).expand();
		assertEquals("Wrong number of remotes", 0, remotesItem.getNodes()
				.size());
		StoredConfig cfg = lookupRepository(repositoryFile).getConfig();
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
		remotesItem = myRepoViewUtil.getRemotesItem(tree, repositoryFile)
				.expand();
		assertEquals("Wrong number of remotes", 3, remotesItem.getNodes()
				.size());

		remotesItem = myRepoViewUtil.getRemotesItem(tree, repositoryFile)
				.expand();
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
		
		// test the properties view on remote
		remotesItem.getNode("test").select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("OpenPropertiesCommand"));
		waitInUI();
		assertEquals("org.eclipse.ui.views.PropertySheet", bot.activeView()
				.getReference().getId());

		removeRemotesConfig(repositoryFile);
		refreshAndWait();
		remotesItem = myRepoViewUtil.getRemotesItem(tree, repositoryFile)
				.expand();
		assertEquals("Wrong number of remotes", 0, remotesItem.getNodes()
				.size());
	}

	/**
	 * Remote configuration scenarios
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConfigureRemote() throws Exception {
		removeRemotesConfig(repositoryFile);
		refreshAndWait();
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem remotesItem = myRepoViewUtil.getRemotesItem(tree,
				repositoryFile).expand();

		remotesItem = myRepoViewUtil.getRemotesItem(tree, repositoryFile)
				.expand();
		remotesItem.select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("NewRemoteCommand"));
		SWTBotShell shell = bot
				.shell(UIText.ConfigureRemoteWizard_WizardTitle_New);
		shell.bot().textWithLabel(UIText.SelectRemoteNamePage_RemoteNameLabel)
				.setText("testRemote");
		// select configure fetch
		shell.bot().checkBox(UIText.SelectRemoteNamePage_ConfigureFetch_button)
				.select();
		// select configure push
		shell.bot().checkBox(UIText.SelectRemoteNamePage_ConfigurePush_button)
				.select();
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		// Click change
		shell.bot().button(UIText.ConfigureUriPage_Change_button).click();
		shell = bot.shell(UIText.SelectUriWiazrd_Title);
		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		// now we have the fetch URI
		shell = bot.shell(UIText.ConfigureRemoteWizard_WizardTitle_New);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		// add all branches
		shell.bot().button(UIText.RefSpecPanel_predefinedAll).click();
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		// the URIish-like path
		String testString = new org.eclipse.jgit.transport.URIish("file:///"
				+ remoteRepositoryFile.getPath()).toPrivateString();
		assertEquals(testString, shell.bot().text().getText());
		// let's try to add the same URI as push
		shell.bot().button(UIText.ConfigureUriPage_Add_button).click();
		shell = bot.shell(UIText.SelectUriWiazrd_Title);
		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		// we get a "duplicate URI" popup
		shell = bot.shell(UIText.ConfigureUriPage_DuplicateUriTitle);
		shell.close();
		shell = bot.shell(UIText.ConfigureRemoteWizard_WizardTitle_New);
		// we continue without adding a special push URI
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		// add all branches
		shell.bot().button(UIText.RefSpecPanel_predefinedAll).click();
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();
		// assert 2 children
		SWTBotTreeItem item = myRepoViewUtil.getRemotesItem(tree,
				repositoryFile).expand().getNode("testRemote").expand();
		List<String> children = item.getNodes();
		assertTrue(children.size() == 2);
		item.getNode(0).select();
		// we remove the fetch, the URI is copied into push
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RemoveFetchCommand"));
		refreshAndWait();
		// assert 1 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertTrue(children.size() == 1);
		item.getNode(0).select();
		// now we also remove the push
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RemovePushCommand"));
		refreshAndWait();
		// assert 0 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertTrue(children.size() == 0);

		myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand().getNode(
				"testRemote").select();
		String shellText = NLS.bind(
				UIText.ConfigureRemoteWizard_WizardTitle_Change, "testRemote");

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ConfigureFetchCommand"));
		shell = bot.shell(shellText);
		// change is 0
		shell.bot().button(UIText.ConfigureUriPage_Change_button).click();
		shell = bot.shell(UIText.SelectUriWiazrd_Title);
		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
		// finish is 1
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		shell = bot.shell(shellText);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		// all branches
		shell.bot().button(UIText.RefSpecPanel_predefinedAll).click();
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();
		// assert 1 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertTrue(children.size() == 1);

		// we remove the fetch again
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertTrue(children.size() == 1);
		item.getNode(0).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RemoveFetchCommand"));
		refreshAndWait();

		myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand().getNode(
				"testRemote").select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ConfigurePushCommand"));
		shell = bot.shell(shellText);
		shell.bot().button(UIText.ConfigureUriPage_Add_button).click();
		shell = bot.shell(UIText.SelectUriWiazrd_Title);
		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		shell = bot.shell(shellText);
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		// all branches
		shell.bot().button(UIText.RefSpecPanel_predefinedAll).click();
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();
		// assert 2 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertTrue(children.size() == 1);
		item.select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RepoViewRemoveRemote.label"));
		shell = bot.shell(UIText.RepositoriesView_ConfirmDeleteRemoteHeader);
		// Cancel
		shell.bot().button(IDialogConstants.CANCEL_LABEL).click();

		refreshAndWait();
		// assert 2 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertTrue(children.size() == 1);

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RepoViewRemoveRemote.label"));
		// OK
		bot.shell(UIText.RepositoriesView_ConfirmDeleteRemoteHeader).bot()
				.button(IDialogConstants.OK_LABEL).click();
		refreshAndWait();
		assertTrue(myRepoViewUtil.getRemotesItem(tree, repositoryFile)
				.getNodes().isEmpty());
	}

	private void removeRemotesConfig(File file) throws Exception {
		Repository repo = lookupRepository(file);
		StoredConfig config = repo.getConfig();
		for (String remote : config.getSubsections("remote"))
			config.unsetSection("remote", remote);
		config.save();
		waitInUI();
	}
}

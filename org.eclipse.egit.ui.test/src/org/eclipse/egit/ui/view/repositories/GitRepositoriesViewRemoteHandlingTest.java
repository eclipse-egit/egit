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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
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
		assertEquals(2, testnodes.size());
		List<String> test2nodes = remotesItem.getNode("test2").expand()
				.getNodes();
		assertEquals(2, test2nodes.size());
		// error node should be shown
		remotesItem.getNode("test3").expand().getNodes();
		assertEquals(1, remotesItem.getNode("test3").expand().getNodes().size());

		// test the properties view on remote
		remotesItem.getNode("test").select();
		ContextMenuHelper.clickContextMenuSync(tree,
				myUtil.getPluginLocalizedValue("ShowIn"),
				"Properties");
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
		SWTBotShell shell = bot.shell(UIText.NewRemoteDialog_WindowTitle);
		shell.bot().textWithLabel(UIText.NewRemoteDialog_NameLabel).setText(
				"testRemote");
		// configure fetch first
		shell.bot().radio(UIText.NewRemoteDialog_FetchRadio).click();
		shell.bot().button(IDialogConstants.OK_LABEL).click();

		// configure fetch dialog
		shell = bot.shell(UIText.SimpleConfigureFetchDialog_WindowTitle);
		// change uri
		shell.bot().button(UIText.SimpleConfigureFetchDialog_ChangeUriButton)
				.click();
		shell = bot.shell(UIText.SelectUriWiazrd_Title);
		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		// now we have the fetch URI
		// back to dialog
		shell = bot.shell(UIText.SimpleConfigureFetchDialog_WindowTitle);
		shell.bot().button(UIText.SimpleConfigureFetchDialog_AddRefSpecButton)
				.click();
		shell = bot.shell(UIText.SimpleFetchRefSpecWizard_WizardTitle);
		shell.bot().textWithLabel(UIText.FetchSourcePage_SourceLabel).setText(
				"refs/heads/*");
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		shell.bot().textWithLabel(UIText.FetchDestinationPage_DestinationLabel)
				.setText("refs/remotes/testRemote/*");
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		// back to dialog
		shell = bot.shell(UIText.SimpleConfigureFetchDialog_WindowTitle);
		// save
		shell.bot().button(UIText.SimpleConfigureFetchDialog_SaveButton)
				.click();

		refreshAndWait();
		// assert 1 children
		SWTBotTreeItem item = myRepoViewUtil.getRemotesItem(tree,
				repositoryFile).expand().getNode("testRemote").expand();
		List<String> children = item.getNodes();
		assertEquals(2, children.size());
		item.select();
		// now we add push
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ConfigurePushCommand"));

		shell = bot.shell(UIText.SimpleConfigurePushDialog_WindowTitle);
		shell.bot()
				.button(UIText.SimpleConfigurePushDialog_AddRefSpecButton, 1)
				.click();

		// add push spec
		shell = bot.shell(UIText.RefSpecDialog_WindowTitle);

		shell.bot().textWithLabel(UIText.RefSpecDialog_SourceBranchPushLabel)
				.setText("HEAD");
		shell.bot().textWithLabel(UIText.RefSpecDialog_DestinationPushLabel)
				.setText("refs/for/master");
		final Text text = shell.bot().textWithLabel(
				UIText.RefSpecDialog_DestinationPushLabel).widget;
		shell.display.syncExec(new Runnable() {

			public void run() {
				text.setFocus();
				text.notifyListeners(SWT.Modify, new Event());
			}
		});
		shell.bot().button(IDialogConstants.OK_LABEL).click();
		shell = bot.shell(UIText.SimpleConfigurePushDialog_WindowTitle);
		shell.bot().button(UIText.SimpleConfigurePushDialog_SaveButton).click();

		refreshAndWait();
		// assert 2 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertEquals(2, children.size());
		item.getNode(0).select();
		// we remove the fetch, the URI is copied into push
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RemoveFetchCommand"));
		refreshAndWait();
		// assert 1 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertEquals(1, children.size());
		item.getNode(0).select();
		// now we also remove the push
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RemovePushCommand"));
		refreshAndWait();
		// assert 0 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertEquals(0, children.size());

		myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand().getNode(
				"testRemote").select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ConfigureFetchCommand"));

		String shellText = UIText.SimpleConfigureFetchDialog_WindowTitle;
		shell = bot.shell(shellText);
		// change uri
		shell.bot().button(UIText.SimpleConfigureFetchDialog_ChangeUriButton)
				.click();
		shell = bot.shell(UIText.SelectUriWiazrd_Title);
		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		// back to dialog
		shell = bot.shell(shellText);
		// add refSpec
		shell.bot().button(UIText.SimpleConfigureFetchDialog_AddRefSpecButton)
				.click();
		shell = bot.shell(UIText.SimpleFetchRefSpecWizard_WizardTitle);
		shell.bot().textWithLabel(UIText.FetchSourcePage_SourceLabel).setText(
				"refs/heads/*");
		shell.bot().button(IDialogConstants.NEXT_LABEL).click();
		shell.bot().textWithLabel(UIText.FetchDestinationPage_DestinationLabel)
				.setText("refs/remotes/testRemote/*");
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		// back to dialog
		shell = bot.shell(shellText);
		// save
		shell.bot().button(UIText.SimpleConfigureFetchDialog_SaveButton)
				.click();
		refreshAndWait();
		// assert 1 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertEquals(2, children.size());

		// we remove the fetch again
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertEquals(2, children.size());
		item.getNode(0).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RemoveFetchCommand"));
		refreshAndWait();

		myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand().getNode(
				"testRemote").select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ConfigurePushCommand"));

		shellText = UIText.SimpleConfigurePushDialog_WindowTitle;
		shell = bot.shell(shellText);
		shell.bot().button(UIText.SimpleConfigurePushDialog_AddPushUriButton)
				.click();

		// back to dialog
		shell = bot.shell(shellText);
		shell = bot.shell(UIText.SelectUriWiazrd_Title);
		shell.bot().text().setText("file:///" + remoteRepositoryFile.getPath());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		shell = bot.shell(shellText);
		// Add is on two buttons
		shell.bot()
				.button(UIText.SimpleConfigurePushDialog_AddRefSpecButton, 1)
				.click();
		// add push spec
		shell = bot.shell(UIText.RefSpecDialog_WindowTitle);

		shell.bot().textWithLabel(UIText.RefSpecDialog_SourceBranchPushLabel)
				.setText("HEAD");
		shell.bot().textWithLabel(UIText.RefSpecDialog_DestinationPushLabel)
				.setText("refs/for/master");
		final Text text2 = shell.bot().textWithLabel(
				UIText.RefSpecDialog_DestinationPushLabel).widget;
		shell.display.syncExec(new Runnable() {

			public void run() {
				// focus for update of other fields
				text2.setFocus();
				text2.notifyListeners(SWT.Modify, new Event());
			}
		});

		shell.bot().button(IDialogConstants.OK_LABEL).click();

		// back to dialog
		shell = bot.shell(shellText);
		shell.bot().button(UIText.SimpleConfigurePushDialog_SaveButton).click();
		refreshAndWait();
		// assert 2 children
		item = myRepoViewUtil.getRemotesItem(tree, repositoryFile).expand()
				.getNode("testRemote").expand();
		children = item.getNodes();
		assertEquals(1, children.size());
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
		assertEquals(1, children.size());

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

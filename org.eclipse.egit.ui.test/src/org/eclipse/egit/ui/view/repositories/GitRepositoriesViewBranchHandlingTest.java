/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 499482
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IPageLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View (mainly branch operations)
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewBranchHandlingTest extends
		GitRepositoriesViewTestBase {

	private File repositoryFile;

	private File remoteRepositoryFile;

	private File clonedRepositoryFile;

	@Before
	public void setUp() throws Exception {
		clearView();
		setVerboseBranchMode(false);
		repositoryFile = createProjectAndCommitToRepository();
		remoteRepositoryFile = createRemoteRepository(repositoryFile);
		// now let's clone the remote repository
		final URIish uri = new URIish(remoteRepositoryFile.getPath());
		final File workdir = new File(getTestDirectory(), "Cloned");

		CloneOperation op = new CloneOperation(uri, true, null, workdir,
				"refs/heads/master", "origin", 0);
		op.run(null);

		clonedRepositoryFile = new File(workdir, Constants.DOT_GIT);

		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();
		repositoryUtil.addConfiguredRepository(repositoryFile);
		repositoryUtil.addConfiguredRepository(remoteRepositoryFile);
		repositoryUtil.addConfiguredRepository(clonedRepositoryFile);
	}

	@Test
	public void testCreateCheckoutDeleteLocalBranch() throws Exception {
		final SWTBotView view = getOrOpenView();
		SWTBotTreeItem localItem = myRepoViewUtil.getLocalBranchesItem(view
				.bot().tree(), repositoryFile);
		TestUtil.expandAndWait(localItem);
		assertEquals("Wrong number of children", 1, localItem.getNodes().size());

		assertEquals("master", localItem.getNodes().get(0));
		localItem.getNode(0).select();

		ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CreateBranchCommand"));

		SWTBotShell createPage = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		createPage.activate();
		// getting text with label doesn't work
		createPage.bot().textWithId("BranchName").setText("newLocal");
		createPage.bot().checkBox(UIText.CreateBranchPage_CheckoutButton)
				.deselect();
		createPage.bot().button(IDialogConstants.FINISH_LABEL).click();
		refreshAndWait();

		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				repositoryFile);
		TestUtil.expandAndWait(localItem);
		assertEquals("Wrong number of children", 2, localItem.getNodes().size());

		localItem.getNode(0).select();
		assertCheckoutNotAvailable(view);
		localItem.getNode(1).select();
		ContextMenuHelper.clickContextMenuSync(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CheckoutCommand"));
		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		assertCheckoutNotAvailable(view);

		localItem.getNode(0).select();
		ContextMenuHelper.clickContextMenuSync(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CheckoutCommand"));
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		refreshAndWait();
		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				repositoryFile);
		localItem.getNode(1).select();
		ContextMenuHelper.clickContextMenuSync(view.bot().tree(), myUtil
				.getPluginLocalizedValue("RepoViewDeleteBranch.label"));
		refreshAndWait();
		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				repositoryFile);
		TestUtil.expandAndWait(localItem);
		assertEquals("Wrong number of children", 1, localItem.getNodes().size());
	}

	private void assertCheckoutNotAvailable(final SWTBotView view) {
		assertFalse("Checkout context menu item should not exist",
				ContextMenuHelper.contextMenuItemExists(view.bot().tree(),
						myUtil.getPluginLocalizedValue("CheckoutCommand")));
	}

	@Test
	public void testCreateDeleteLocalBranchWithUnmerged() throws Exception {
		final SWTBotView view = getOrOpenView();
		SWTBotTreeItem localItem = myRepoViewUtil.getLocalBranchesItem(view
				.bot().tree(), repositoryFile);
		TestUtil.expandAndWait(localItem);
		assertEquals("Wrong number of children", 1, localItem.getNodes().size());

		assertEquals("master", localItem.getNodes().get(0));
		localItem.getNode(0).select();

		ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CreateBranchCommand"));

		SWTBotShell createPage = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		createPage.activate();
		// getting text with label doesn't work
		createPage.bot().textWithId("BranchName").setText("newLocal");
		createPage.bot().checkBox(UIText.CreateBranchPage_CheckoutButton)
				.select();
		createPage.bot().button(IDialogConstants.FINISH_LABEL).click();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		refreshAndWait();

		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				repositoryFile);
		TestUtil.expandAndWait(localItem);
		assertEquals("Wrong number of children", 2, localItem.getNodes().size());

		touchAndSubmit("Some more changes");

		localItem.getNode(1).select();
		assertCheckoutNotAvailable(view);
		localItem.getNode(0).select();
		ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
				.getPluginLocalizedValue("CheckoutCommand"));
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		refreshAndWait();
		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				repositoryFile);
		localItem.getNode(1).select();
		ContextMenuHelper.clickContextMenu(view.bot().tree(), myUtil
				.getPluginLocalizedValue("RepoViewDeleteBranch.label"));
		SWTBotShell confirmPopup = bot
				.shell(UIText.UnmergedBranchDialog_Title);
		confirmPopup.activate();
		confirmPopup.bot().button(IDialogConstants.OK_LABEL).click();
		refreshAndWait();
		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				repositoryFile);
		TestUtil.expandAndWait(localItem);
		assertEquals("Wrong number of children", 1, localItem.getNodes().size());
	}

	@Test
	public void testClonedRepository() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();

		SWTBotTreeItem item = TestUtil.expandAndWait(myRepoViewUtil
				.getLocalBranchesItem(tree, clonedRepositoryFile));

		List<String> children = item.getNodes();
		assertEquals("Wrong number of local children", 1, children.size());

		item = TestUtil.expandAndWait(myRepoViewUtil.getRemoteBranchesItem(tree,
				clonedRepositoryFile));
		children = item.getNodes();
		assertEquals("Wrong number of children", 2, children.size());
		assertTrue("Missing remote branch", children.contains("origin/master"));
		assertTrue("Missing remote branch", children.contains("origin/stable"));
		item.getNode("origin/stable").select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("CreateBranchCommand"));
		SWTBotShell shell = bot.shell(UIText.CreateBranchWizard_NewBranchTitle);
		shell.activate();
		assertEquals("stable", shell.bot().textWithId("BranchName").getText());
		shell.bot().button(IDialogConstants.FINISH_LABEL).click();
		bot.waitUntil(Conditions.shellCloses(shell));
		refreshAndWait();
		item = TestUtil.expandAndWait(myRepoViewUtil.getLocalBranchesItem(tree,
				clonedRepositoryFile));

		children = item.getNodes();
		assertEquals("Wrong number of local children", 2, children.size());
	}

	@Test
	public void testCheckoutRemote() throws Exception {
		Repository repo = lookupRepository(clonedRepositoryFile);
		BranchOperation bop = new BranchOperation(repo, "refs/heads/master");
		bop.execute(null);

		assertEquals("master", repo.getBranch());
		SWTBotTree tree = getOrOpenView().bot().tree();

		SWTBotTreeItem item = myRepoViewUtil.getLocalBranchesItem(tree,
				clonedRepositoryFile).expand();

		touchAndSubmit(null);
		refreshAndWait();

		item = TestUtil.expandAndWait(myRepoViewUtil.getRemoteBranchesItem(tree,
				clonedRepositoryFile));
		List<String> children = item.getNodes();
		assertEquals("Wrong number of remote children", 2, children.size());

		item.getNode("origin/stable").select();
		ContextMenuHelper.clickContextMenuSync(tree,
				myUtil.getPluginLocalizedValue("CheckoutCommand"));
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		refreshAndWait();

		GitLightweightDecorator.refresh();

		assertTrue("Branch should not be symbolic",
				ObjectId.isId(lookupRepository(clonedRepositoryFile)
						.getBranch()));

		// now let's try to create a local branch from the remote one
		item = myRepoViewUtil.getRemoteBranchesItem(tree, clonedRepositoryFile);
		TestUtil.expandAndWait(item).getNode("origin/stable").select();
		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("CreateBranchCommand"));

		SWTBotShell createPage = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		createPage.activate();
		assertEquals("Wrong suggested branch name", "stable", createPage.bot()
				.textWithId("BranchName").getText());
		createPage.close();
		// checkout master again

		item = myRepoViewUtil.getLocalBranchesItem(tree, clonedRepositoryFile);
		TestUtil.expandAndWait(item).getNode("master").select();
		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("CheckoutCommand"));
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		refreshAndWait();
	}

	@Test
	public void testRenameBranch() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();

		SWTBotTreeItem item = TestUtil.expandAndWait(myRepoViewUtil
				.getLocalBranchesItem(tree, clonedRepositoryFile));

		item.getNode("master").select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RepoViewRenameBranch.label"));
		refreshAndWait();

		SWTBotShell renameDialog = bot
				.shell(UIText.BranchRenameDialog_WindowTitle);
		SWTBotText newBranchNameText = renameDialog.bot().textWithLabel(UIText.BranchRenameDialog_NewNameLabel);
		newBranchNameText.setText("invalid~name");

		renameDialog.bot().text(" " + // the text is now in the error message, and the MessageAreaDialog seems to add a space
				NLS.bind(UIText.ValidationUtils_InvalidRefNameMessage,
						"refs/heads/invalid~name"));
		assertFalse(renameDialog.bot().button(IDialogConstants.OK_LABEL)
				.isEnabled());
		newBranchNameText.setText("newmaster");
		renameDialog.bot().button(IDialogConstants.OK_LABEL).click();

		refreshAndWait();

		item = TestUtil.expandAndWait(myRepoViewUtil.getLocalBranchesItem(tree,
				clonedRepositoryFile));
		assertEquals("newmaster", item.getNode(0).select().getText());

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RepoViewRenameBranch.label"));
		refreshAndWait();

		renameDialog = bot.shell(UIText.BranchRenameDialog_WindowTitle);
		newBranchNameText = renameDialog.bot().text(0);

		newBranchNameText.setText("master");
		renameDialog.bot().button(IDialogConstants.OK_LABEL).click();

		refreshAndWait();

		item = TestUtil.expandAndWait(myRepoViewUtil.getLocalBranchesItem(tree,
				clonedRepositoryFile));
		assertEquals("master", item.getNode(0).select().getText());
	}

	@Test
	public void testMergeOnRepo() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();

		myRepoViewUtil.getRootItem(tree, clonedRepositoryFile).select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("RepoViewMerge.label"));

		String title = NLS.bind(
				UIText.MergeTargetSelectionDialog_TitleMergeWithBranch,
				FileRepositoryBuilder.create(clonedRepositoryFile).getBranch());

		SWTBotShell mergeDialog = bot.shell(title);
		// TODO do some merge here
		mergeDialog.close();
	}

	@Test
	public void testBranchConfiguration() throws Exception {
		Repository repo = lookupRepository(clonedRepositoryFile);
		try (Git git = new Git(repo)) {
			git.branchCreate().setName("configTest")
					.setStartPoint("refs/remotes/origin/master")
					.setUpstreamMode(SetupUpstreamMode.TRACK).call();
		}
		BranchRebaseMode rebase = repo.getConfig().getEnum(
				BranchRebaseMode.values(),
				ConfigConstants.CONFIG_BRANCH_SECTION, "configTest",
				ConfigConstants.CONFIG_KEY_REBASE, BranchRebaseMode.NONE);
		assertEquals(BranchRebaseMode.NONE, rebase);

		SWTBotView view = getOrOpenView();

		SWTBotTreeItem localItem = myRepoViewUtil.getLocalBranchesItem(view
				.bot().tree(), clonedRepositoryFile);
		TestUtil.expandAndWait(localItem).getNode("configTest").select();

		ContextMenuHelper.clickContextMenuSync(view.bot().tree(),
				myUtil.getPluginLocalizedValue("ShowIn"),
				"Properties");

		SWTBotView propsView = bot.viewById(IPageLayout.ID_PROP_SHEET);
		SWTBotTreeItem rootItem = propsView
				.bot()
				.tree()
				.getTreeItem(
						UIText.BranchPropertySource_UpstreamConfigurationCategory);
		SWTBotTreeItem rebaseItem = TestUtil.expandAndWait(rootItem)
				.getNode(UIText.BranchPropertySource_RebaseDescriptor);
		assertEquals(UIText.BranchPropertySource_ValueNotSet,
				rebaseItem.cell(1));

		SWTBotTreeItem remoteItem = rootItem
				.getNode(UIText.BranchPropertySource_RemoteDescriptor);
		assertEquals("origin", remoteItem.cell(1));

		SWTBotTreeItem upstreamItem = rootItem
				.getNode(UIText.BranchPropertySource_UpstreamBranchDescriptor);
		assertEquals("refs/heads/master", upstreamItem.cell(1));

		view = getOrOpenView();

		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				clonedRepositoryFile);
		TestUtil.expandAndWait(localItem).getNode("configTest").select();

		ContextMenuHelper.clickContextMenu(view.bot().tree(),
				myUtil.getPluginLocalizedValue("ConfigurBranchCommand.label"));

		SWTBotShell configureBranchDialog = bot
				.shell(UIText.BranchConfigurationDialog_BranchConfigurationTitle);
		assertEquals(MessageFormat.format(
				UIText.BranchConfigurationDialog_EditBranchConfigMessage,
				"configTest"), configureBranchDialog.bot().text().getText());
		assertEquals(
				"refs/heads/master",
				configureBranchDialog
						.bot()
						.comboBoxWithLabel(
								UIText.BranchConfigurationDialog_UpstreamBranchLabel)
						.getText());
		assertEquals(
				"origin",
				configureBranchDialog
						.bot()
						.comboBoxWithLabel(
								UIText.BranchConfigurationDialog_RemoteLabel)
						.getText());
		assertEquals(UIText.BranchRebaseMode_None,
				configureBranchDialog.bot()
						.comboBoxWithLabel(
								UIText.BranchRebaseModeCombo_RebaseModeLabel)
						.getText());

		configureBranchDialog.bot()
				.comboBoxWithLabel(
						UIText.BranchRebaseModeCombo_RebaseModeLabel)
				.setSelection(0);
		// add a listener to wait for the configuration changed event
		final AtomicBoolean changed = new AtomicBoolean();
		ConfigChangedListener listener =
		new ConfigChangedListener() {
			@Override
			public void onConfigChanged(ConfigChangedEvent event) {
				changed.set(true);
			}
		};
		ListenerHandle handle = repo.getConfig().addChangeListener(listener);
		// only now click ok
		configureBranchDialog.bot().button("OK").click();

		// cleanup behind ourselves
		handle.remove();
		if (!changed.get())
			fail("We should have received a config change event");

		refreshAndWait(); // Repo view updates itself after config change.
		rebase = repo.getConfig().getEnum(BranchRebaseMode.values(),
				ConfigConstants.CONFIG_BRANCH_SECTION, "configTest",
				ConfigConstants.CONFIG_KEY_REBASE, BranchRebaseMode.NONE);
		assertEquals(BranchRebaseMode.REBASE, rebase);

		localItem = myRepoViewUtil.getLocalBranchesItem(view.bot().tree(),
				clonedRepositoryFile);
		TestUtil.expandAndWait(localItem).getNode("configTest").select();

		ContextMenuHelper.clickContextMenu(view.bot().tree(),
				myUtil.getPluginLocalizedValue("ShowIn"),
				"Properties");

		propsView = bot.viewById(IPageLayout.ID_PROP_SHEET);
		rootItem = propsView
				.bot()
				.tree()
				.getTreeItem(
						UIText.BranchPropertySource_UpstreamConfigurationCategory);
		rebaseItem = TestUtil.expandAndWait(rootItem)
				.getNode(UIText.BranchPropertySource_RebaseDescriptor);
		assertEquals("true", rebaseItem.cell(1));
	}
}

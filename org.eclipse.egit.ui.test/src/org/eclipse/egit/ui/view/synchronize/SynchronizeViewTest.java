/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.UIText.SynchronizeWithAction_localRepoName;
import static org.eclipse.egit.ui.UIText.SynchronizeWithAction_tagsName;
import static org.eclipse.egit.ui.test.ContextMenuHelper.clickContextMenu;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.MASTER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.internal.ui.synchronize.RefreshParticipantJob;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class SynchronizeViewTest extends LocalRepositoryTestCase {

	private static final String INITIAL_TAG = "initial-tag";

	@Test
	public void shouldReturnNoChanges() throws Exception {
		// given
		resetRepository(PROJ1);
		changeFilesInProject();
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		launchSynchronization(SynchronizeWithAction_localRepoName, HEAD,
				SynchronizeWithAction_localRepoName, MASTER, false);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(0, syncViewTree.getAllItems().length);
	}

	// this test fails when is run inside eclipse with Maven POM editor
	@Test
	public void shouldReturnListOfChanges() throws Exception {
		// given
		resetRepository(PROJ1);
		changeFilesInProject();
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		bot.shell("Synchronize repository: " + REPO1 + File.separator + ".git")
				.activate();

		// include local changes are enabled by default
		bot.comboBox(2)
				.setSelection(UIText.SynchronizeWithAction_localRepoName);
		bot.comboBox(3).setSelection(MASTER);

		// fire action
		bot.button(IDialogConstants.OK_LABEL).click();

		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		// wait for synchronization process finish
		waitUntilTreeHasNodeWithText(syncViewTree,
				UIText.GitModelWorkingTree_workingTree);

		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertEquals(UIText.GitModelWorkingTree_workingTree,
				syncItems[0].getText());
	}

	@Test
	public void shouldCompareBranchAgainstTag() throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "v0.0");
		makeChangesAndCommit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		launchSynchronization(SynchronizeWithAction_tagsName, "v0.0",
				SynchronizeWithAction_localRepoName, HEAD, false);

		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		// wait for synchronization process finish
		waitUntilTreeHasNodeWithText(syncViewTree, "test commit");

		// then
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldCompareTagAgainstTag() throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "v0.1");
		makeChangesAndCommit(PROJ1);
		createTag(PROJ1, "v0.2");
		makeChangesAndCommit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		launchSynchronization(SynchronizeWithAction_tagsName, "v0.1",
				SynchronizeWithAction_tagsName, "v0.2", false);

		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		// wait for synchronization process finish
		waitUntilTreeHasNodeWithText(syncViewTree, "test commit");

		// then
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test public void shouldOpenCompareEditorInGitChangeSet() throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "compare1");
		changeFilesInProject();
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		launchSynchronization(null, null, SynchronizeWithAction_tagsName,
				"compare1", true);

		// then
		SWTBot compare = getCompareEditorForFileInGitChangeSet(FILE1, true);
		assertNotNull(compare);
	}

	@Test public void shouldOpenCompareEditorInWorkspaceModel()
			throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "compare2");
		changeFilesInProject();
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		launchSynchronization(null, null, SynchronizeWithAction_tagsName,
				"compare2", true);

		// then
		SWTBot compare = getCompareEditorForFileInWorkspaceModel(true).bot();
		assertNotNull(compare);
	}

	@Test public void shouldListFileDeletedChange() throws Exception {
		// given
		resetRepository(PROJ1);
		createTag(PROJ1, "base");
		deleteFileAndCommit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// when
		launchSynchronization(null, null, SynchronizeWithAction_tagsName,
				"base", true);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
		SWTBotTreeItem commitTree = syncViewTree.getAllItems()[0];
		commitTree.expand();
		SWTBotTreeItem projectTree = commitTree.getItems()[0];
		projectTree.expand();
		assertEquals(1, projectTree.getItems().length);
		SWTBotTreeItem folderTree = projectTree.getItems()[0];
		folderTree.expand();
		assertEquals(1, folderTree.getItems().length);
		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals("test.txt", fileTree.getText());
	}

	@Test public void shouldSynchronizeInEmptyRepository() throws Exception {
		// given
		createEmptyRepository();
		showDialog("EmptyProject", "Team", "Synchronize...");

		// when
		launchSynchronization("EmptyRepository", null, null, null, null, true);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
		SWTBotTreeItem commitTree = syncViewTree.getAllItems()[0];
		commitTree.expand();
		SWTBotTreeItem projectTree = commitTree.getItems()[0];
		projectTree.expand();
		assertEquals(2, projectTree.getItems().length);
		SWTBotTreeItem folderTree = projectTree.getItems()[0];
		folderTree.expand();
		assertEquals(2, folderTree.getItems().length);
		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals("test.txt", fileTree.getText());
		fileTree = folderTree.getItems()[1];
		assertEquals("test2.txt", fileTree.getText());
	}

	@Test public void shouldExchangeCompareEditorSidesBetweenIncomingAndOutgoingChangesInGitChangeSet()
			throws Exception {
		// given
		String tagName = "exchangeCompareSidesInGitChangeSet";
		resetRepository(PROJ1);
		createTag(PROJ1, tagName);
		changeFilesInProject();
		commit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// compare HEAD against tag
		launchSynchronization(SynchronizeWithAction_localRepoName, HEAD,
				SynchronizeWithAction_tagsName, tagName, false);
		SWTBot outgoingCompare = getCompareEditorForFileInGitChangeSet(FILE1,
				false);
		// save left value from compare editor
		String outgoingLeft = outgoingCompare.styledText(0).getText();
		// save right value from compare editor
		String outgoingRight = outgoingCompare.styledText(1).getText();

		// when
		// compare tag against HEAD
		showDialog(PROJ1, "Team", "Synchronize...");
		launchSynchronization(SynchronizeWithAction_tagsName, tagName,
				SynchronizeWithAction_localRepoName, HEAD, false);

		// then
		SWTBot incomingComp = getCompareEditorForFileInGitChangeSet(FILE1,
				false);
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingComp.styledText(1).getText()));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingComp.styledText(0).getText()));
	}

	@Test public void shouldExchangeCompareEditorSidesBetweenIncomingAndOutgoingChangesInWorkspaceModel()
			throws Exception {
		// given
		String tagName = "exchangeCompareSidesInWorkspace";
		resetRepository(PROJ1);
		createTag(PROJ1, tagName);
		changeFilesInProject();
		commit(PROJ1);
		showDialog(PROJ1, "Team", "Synchronize...");

		// compare HEAD against tag
		launchSynchronization(SynchronizeWithAction_localRepoName, HEAD,
				SynchronizeWithAction_tagsName, tagName, false);
		SWTBotEditor compEditor = getCompareEditorForFileInWorkspaceModel(false);
		SWTBot outgoingCompare = compEditor.bot();
		// save left value from compare editor
		String outgoingLeft = outgoingCompare.styledText(0).getText();
		// save right value from compare editor
		String outgoingRight = outgoingCompare.styledText(1).getText();

		// when
		// compare tag against HEAD
		showDialog(PROJ1, "Team", "Synchronize...");
		launchSynchronization(SynchronizeWithAction_tagsName, tagName,
				SynchronizeWithAction_localRepoName, HEAD, false);

		// then
		SWTBot incomingComp = getCompareEditorForFileInWorkspaceModel(false)
				.bot();
		String incomingLeft = incomingComp.styledText(0).getText();
		String incomingRight = incomingComp.styledText(1).getText();
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingRight));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingLeft));
	}

	private void waitUntilTreeHasNodeWithText(final SWTBotTree tree,
			final String text) {
		bot.waitUntil(new ICondition() {

			public boolean test() throws Exception {
				for (SWTBotTreeItem item : tree.getAllItems())
					if (item.getText().contains(text))
						return true;
				return false;
			}

			public void init(SWTBot bot2) {
				// empty
			}

			public String getFailureMessage() {
				return null;
			}
		}, 10000);
	}

	// this test always fails with cause:
	// Timeout after: 5000 ms.: Could not find context menu with text:
	// Synchronize
	@Ignore
	@Test
	public void shouldLaunchSynchronizationFromGitRepositories()
			throws Exception {
		// given
		bot.menu("Window").menu("Show View").menu("Other...").click();
		bot.shell("Show View").bot().tree().expandNode("Git").getNode(
				"Git Repositories").doubleClick();

		SWTBotTree repositoriesTree = bot.viewByTitle("Git Repositories").bot()
				.tree();
		SWTBotTreeItem egitRoot = repositoriesTree.getAllItems()[0];
		egitRoot.expand();
		egitRoot.collapse();
		egitRoot.expand();
		SWTBotTreeItem remoteBranch = egitRoot.expandNode("Branches")
				.expandNode("Remote Branches");
		SWTBotTreeItem branchNode = remoteBranch.getNode("origin/stable-0.7");
		branchNode.select();
		branchNode.contextMenu("Synchronize").click();

		// when

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(8, syncViewTree.getAllItems().length);
	}

	@Before
	public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
	}

	@BeforeClass
	public static void setupEnvironment() throws Exception {
		// disable perspective synchronize selection
		new Eclipse().openPreferencePage(null);
		bot.tree().getTreeItem("Team").expand().select();
		SWTBotRadio syncPerspectiveCheck = bot.radio("Never");
		if (!syncPerspectiveCheck.isSelected())
			syncPerspectiveCheck.click();

		bot.button(IDialogConstants.OK_LABEL).click();

		File repositoryFile = createProjectAndCommitToRepository();
		createChildRepository(repositoryFile);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);

		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();

		createTag(PROJ1, INITIAL_TAG);
	}

	@AfterClass
	public static void restoreEnvironmentSetup() throws Exception {
		new Eclipse().reset();
	}

	private void changeFilesInProject() throws Exception {
		SWTBot packageExlBot = bot.viewByTitle("Package Explorer").bot();
		SWTBotTreeItem coreTreeItem = selectProject(PROJ1, packageExlBot.tree());
		SWTBotTreeItem rootNode = coreTreeItem.expand().getNode(0)
				.expand().select();
		rootNode.getNode(0).select().doubleClick();

		SWTBotEditor corePomEditor = bot.editorByTitle(FILE1);
		corePomEditor.toTextEditor()
				.insertText("<!-- EGit jUnit test case -->");
		corePomEditor.saveAndClose();

		rootNode.getNode(1).select().doubleClick();
		SWTBotEditor uiPomEditor = bot.editorByTitle(FILE2);
		uiPomEditor.toTextEditor().insertText("<!-- EGit jUnit test case -->");
		uiPomEditor.saveAndClose();
		coreTreeItem.collapse();
	}

	private void resetRepository(String projectName) throws Exception {
		showDialog(projectName, "Team", "Reset...");

		bot.shell(UIText.ResetCommand_WizardTitle).bot().activeShell();

		SWTBotTreeItem tagsNode = bot.tree().getTreeItem("Tags");
		tagsNode.expand();
		tagsNode.getNode(INITIAL_TAG).select();

		bot.radio(UIText.ResetTargetSelectionDialog_ResetTypeHardButton)
				.click();
		bot.button(UIText.ResetTargetSelectionDialog_ResetButton).click();

		bot.shell(UIText.ResetTargetSelectionDialog_ResetQuestion).bot()
				.activeShell();
		bot.button("Yes").click();
		TestUtil.joinJobs(JobFamilies.RESET);
	}

	private static void createTag(String projectName, String tagName)
			throws Exception {
		showDialog(projectName, "Team", "Tag...");

		bot.shell("Create new tag").bot().activeShell();
		bot.text(0).setFocus();
		bot.text(0).setText(tagName);
		bot.styledText(0).setFocus();
		bot.styledText(0).setText(tagName);
		bot.button(IDialogConstants.OK_LABEL).click();
		TestUtil.joinJobs(JobFamilies.TAG);
	}

	private void makeChangesAndCommit(String projectName) throws Exception {
		changeFilesInProject();
		Thread.sleep(1000); // wait 1 s to get different time stamps
							// TODO can be removed when commit is based on DirCache
		commit(projectName);
	}

	private void deleteFileAndCommit(String projectName) throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt")).delete(true, null);

		commit(projectName);
	}

	private void commit(String projectName) throws InterruptedException {
		showDialog(projectName, "Team", UIText.CommitAction_commit);

		bot.shell(UIText.CommitDialog_CommitChanges).bot().activeShell();
		bot.styledText(0).setText("test commit");
		bot.button(UIText.CommitDialog_SelectAll).click();
		bot.button(UIText.CommitDialog_Commit).click();
		TestUtil.joinJobs(JobFamilies.COMMIT);
	}

	private static void showDialog(String projectName, String... cmd) {
		SWTBot packageExplorerBot = bot.viewByTitle("Package Explorer").bot();
		packageExplorerBot.activeShell();
		SWTBotTree tree = packageExplorerBot.tree();

		// EGit decorates the project node shown in the package explorer. The
		// '>' decorator indicates that there are uncommitted changes present in
		// the project. Also the repository and branch name are added as a
		// suffix ('[<repo name> <branch name>]' suffix). To bypass this
		// decoration we use here this loop.
		selectProject(projectName, tree);

		clickContextMenu(tree, cmd);
	}

	private static SWTBotTreeItem selectProject(String projectName,
			SWTBotTree tree) {
		for (SWTBotTreeItem item : tree.getAllItems()) {
			if (item.getText().contains(projectName)) {
				item.select();
				return item;
			}
		}

		throw new RuntimeException("Poject with name " + projectName +
				" was not found in given tree");
	}

	private void launchSynchronization(String srcRepo, String srcRef,
			String dstRepo, String dstRef, boolean includeLocal) {
		launchSynchronization(REPO1, srcRepo, srcRef, dstRepo, dstRef,
				includeLocal);
	}

	private void launchSynchronization(String repo, String srcRepo,
			String srcRef, String dstRepo, String dstRef, boolean includeLocal) {
		bot.shell("Synchronize repository: " + repo + File.separator + ".git")
				.activate();

		if (!includeLocal)
			bot.checkBox(
					UIText.SelectSynchronizeResourceDialog_includeUncommitedChanges)
					.click();

		if (srcRepo != null)
			bot.comboBox(0)
					.setSelection(srcRepo);
		if (srcRef != null)
			bot.comboBox(1).setSelection(srcRef);

		if (dstRepo != null)
			bot.comboBox(2)
					.setSelection(dstRepo);
		if (dstRef != null)
			bot.comboBox(3).setSelection(dstRef);

		// register synchronization finish hook
		SynchronizeFinishHook sfh = new SynchronizeFinishHook();
		Job.getJobManager().addJobChangeListener(sfh);

		// fire action
		bot.button(IDialogConstants.OK_LABEL).click();

		try {
			bot.waitUntil(sfh, 120000);
		} finally {
			Job.getJobManager().removeJobChangeListener(sfh);
		}
	}

	private static class SynchronizeFinishHook extends JobChangeAdapter
			implements ICondition {
		private boolean state = false;

		@SuppressWarnings("restriction") public void done(IJobChangeEvent event) {
			if (event.getJob() instanceof RefreshParticipantJob)
				state = true;
		}

		public boolean test() throws Exception {
			return state;
		}

		public void init(SWTBot swtBot) {
			// unused
		}

		public String getFailureMessage() {
			// unused
			return null;
		}

	}

	private SWTBot setPresentationModel(String model) throws Exception {
		SWTBotView syncView = bot.viewByTitle("Synchronize");
		SWTBotToolbarDropDownButton dropDown = syncView
				.toolbarDropDownButton("Show File System Resources");
		dropDown.menuItem(model).click();
		// hide drop down
		dropDown.pressShortcut(KeyStroke.getInstance("ESC"));

		return syncView.bot();
	}

	// based on LocalRepositoryTestCase#createProjectAndCommitToRepository(String)
	private void createEmptyRepository() throws Exception {
		File gitDir = new File(new File(getTestDirectory(), "EmptyRepository"),
				Constants.DOT_GIT);
		gitDir.mkdir();
		Repository myRepository = new FileRepository(gitDir);
		myRepository.create();

		// we need to commit into master first
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("EmptyProject");

		if (firstProject.exists())
			firstProject.delete(true, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription("EmptyProject");
		desc.setLocation(new Path(new File(myRepository.getWorkTree(),
				"EmptyProject").getPath()));
		firstProject.create(desc, null);
		firstProject.open(null);

		IFolder folder = firstProject.getFolder(FOLDER);
		folder.create(false, true, null);
		IFile textFile = folder.getFile(FILE1);
		textFile.create(new ByteArrayInputStream("Hello, world"
				.getBytes(firstProject.getDefaultCharset())), false, null);
		IFile textFile2 = folder.getFile(FILE2);
		textFile2.create(new ByteArrayInputStream("Some more content"
				.getBytes(firstProject.getDefaultCharset())), false, null);

		new ConnectProviderOperation(firstProject, gitDir).execute(null);
	}

	private SWTBot getCompareEditorForFileInGitChangeSet(String fileName,
			boolean waitForWorkingTree) {
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		if (waitForWorkingTree)
			// wait for tree showing node "<working tree>"
			syncViewTree.getTreeItem(UIText.GitModelWorkingTree_workingTree);
		// expand all nodes
		syncViewTree.getAllItems()[0].collapse().doubleClick();
		// try to open compare editor for fileName
		syncViewTree.getAllItems()[0].getItems()[0].getNode(FOLDER)
				.getNode(fileName).doubleClick();

		return bot.editorByTitle(fileName).bot();
	}

	private SWTBotEditor getCompareEditorForFileInWorkspaceModel(
			boolean waitForProject)
			throws Exception {
		SWTBotTree syncViewTree = setPresentationModel("Workspace").tree();
		// try to open compare editor for FILE1
		if (waitForProject)
			TestUtil.waitUntilTreeHasNodeWithText(bot, syncViewTree, "> " +
					PROJ1, 10000);
		syncViewTree.getAllItems()[0].expand().getItems()[0].expand()
				.getItems()[0].doubleClick();

		return bot.editorByTitle(FILE1);
	}

}

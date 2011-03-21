/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.UIText.CommitAction_commit;
import static org.eclipse.egit.ui.UIText.CommitDialog_Commit;
import static org.eclipse.egit.ui.UIText.CommitDialog_CommitChanges;
import static org.eclipse.egit.ui.UIText.CommitDialog_SelectAll;
import static org.eclipse.egit.ui.UIText.GitModelWorkingTree_workingTree;
import static org.eclipse.egit.ui.UIText.SynchronizeWithAction_localRepoName;
import static org.eclipse.egit.ui.UIText.SynchronizeWithAction_tagsName;
import static org.eclipse.egit.ui.test.ContextMenuHelper.clickContextMenu;
import static org.eclipse.egit.ui.test.TestUtil.waitUntilTreeHasNodeContainsText;
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.core.op.ResetOperation.ResetType;
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
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class SynchronizeViewTest extends LocalRepositoryTestCase {

	private static final String INITIAL_TAG = "initial-tag";

	private static final String TEST_COMMIT_MSG = "test commit";

	private static final String EMPTY_PROJECT = "EmptyProject";

	private static final String EMPTY_REPOSITORY = "EmptyRepository";

	private static File repositoryFile;

	@Test
	public void shouldReturnNoChanges() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();

		// when
		launchSynchronization(SynchronizeWithAction_localRepoName, HEAD,
				SynchronizeWithAction_localRepoName, MASTER, false);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(0, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldReturnListOfChanges() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();

		// when
		launchSynchronization(null, null, SynchronizeWithAction_localRepoName,
				HEAD, true);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem[] syncItems = syncViewTree.getAllItems();
		assertEquals(GitModelWorkingTree_workingTree, syncItems[0].getText());
	}

	@Test
	public void shouldCompareBranchAgainstTag() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		makeChangesAndCommit(PROJ1);

		// when
		launchSynchronization(SynchronizeWithAction_tagsName, INITIAL_TAG,
				SynchronizeWithAction_localRepoName, HEAD, false);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test
	public void shouldCompareTagAgainstTag() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		makeChangesAndCommit(PROJ1);
		createTag(PROJ1, "v0.1");

		// when
		launchSynchronization(SynchronizeWithAction_tagsName, INITIAL_TAG,
				SynchronizeWithAction_tagsName, "v0.1", false);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);
	}

	@Test public void shouldOpenCompareEditorInGitChangeSet() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();

		// when
		launchSynchronization(null, null, SynchronizeWithAction_tagsName,
				INITIAL_TAG, true);

		// then
		SWTBot compare = getCompareEditorForFileInGitChangeSet(FILE1, true)
				.bot();
		assertNotNull(compare);
	}

	@Test public void shouldOpenCompareEditorInWorkspaceModel()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		changeFilesInProject();

		// when
		launchSynchronization(null, null, SynchronizeWithAction_tagsName,
				INITIAL_TAG, true);

		// then
		SWTBot compare = getCompareEditorForFileInWorkspaceModel().bot();
		assertNotNull(compare);
	}

	@Test public void shouldListFileDeletedChange() throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		deleteFileAndCommit(PROJ1);

		// when
		launchSynchronization(null, null, SynchronizeWithAction_tagsName,
				INITIAL_TAG, true);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		assertEquals(1, syncViewTree.getAllItems().length);

		SWTBotTreeItem commitTree = waitForNodeWithText(syncViewTree,
				TEST_COMMIT_MSG);
		SWTBotTreeItem projectTree = waitForNodeWithText(commitTree, PROJ1);
		assertEquals(1, projectTree.getItems().length);

		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		assertEquals(1, folderTree.getItems().length);

		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals("test.txt", fileTree.getText());
	}

	@Test public void shouldSynchronizeInEmptyRepository() throws Exception {
		// given
		createEmptyRepository();

		// when
		launchSynchronization(EMPTY_REPOSITORY, EMPTY_PROJECT, null, null,
				null, null, true);

		// then
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		SWTBotTreeItem commitTree = waitForNodeWithText(syncViewTree,
				GitModelWorkingTree_workingTree);
		assertEquals(1, syncViewTree.getAllItems().length);
		SWTBotTreeItem projectTree = waitForNodeWithText(commitTree,
				EMPTY_PROJECT);
		assertEquals(2, projectTree.getItems().length);

		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		assertEquals(2, folderTree.getItems().length);

		SWTBotTreeItem fileTree = folderTree.getItems()[0];
		assertEquals(FILE1, fileTree.getText());
		fileTree = folderTree.getItems()[1];
		assertEquals(FILE2, fileTree.getText());
	}

	@Test public void shouldExchangeCompareEditorSidesBetweenIncomingAndOutgoingChangesInGitChangeSet()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		makeChangesAndCommit(PROJ1);

		// compare HEAD against tag
		launchSynchronization(SynchronizeWithAction_localRepoName, HEAD,
				SynchronizeWithAction_tagsName, INITIAL_TAG, false);
		SWTBotEditor outgoingCompare = getCompareEditorForFileInGitChangeSet(
				FILE1, false);
		SWTBot outgoingCompareBot = outgoingCompare.bot();
		// save left value from compare editor
		String outgoingLeft = outgoingCompareBot.styledText(0).getText();
		// save right value from compare editor
		String outgoingRight = outgoingCompareBot.styledText(1).getText();
		outgoingCompare.close();

		// when
		// compare tag against HEAD
		launchSynchronization(SynchronizeWithAction_tagsName, INITIAL_TAG,
				SynchronizeWithAction_localRepoName, HEAD, false);

		// then
		SWTBot incomingComp = getCompareEditorForFileInGitChangeSet(
				FILE1, false).bot();
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingComp.styledText(1).getText()));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingComp.styledText(0).getText()));
	}

	@Test public void shouldExchangeCompareEditorSidesBetweenIncomingAndOutgoingChangesInWorkspaceModel()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		makeChangesAndCommit(PROJ1);

		// compare HEAD against tag
		launchSynchronization(SynchronizeWithAction_localRepoName, HEAD,
				SynchronizeWithAction_tagsName, INITIAL_TAG, false);
		SWTBotEditor compEditor = getCompareEditorForFileInWorkspaceModel();
		SWTBot outgoingCompare = compEditor.bot();
		// save left value from compare editor
		String outgoingLeft = outgoingCompare.styledText(0).getText();
		// save right value from compare editor
		String outgoingRight = outgoingCompare.styledText(1).getText();
		compEditor.close();

		// when
		// compare tag against HEAD
		launchSynchronization(SynchronizeWithAction_tagsName, INITIAL_TAG,
				SynchronizeWithAction_localRepoName, HEAD, false);

		// then
		SWTBot incomingComp = getCompareEditorForFileInWorkspaceModel()
				.bot();
		String incomingLeft = incomingComp.styledText(0).getText();
		String incomingRight = incomingComp.styledText(1).getText();
		// right side from compare editor should be equal with left
		assertThat(outgoingLeft, equalTo(incomingRight));
		// left side from compare editor should be equal with right
		assertThat(outgoingRight, equalTo(incomingLeft));
	}

	@Test public void shouldNotShowIgnoredFilesInGitChangeSetModel()
			throws Exception {
		// given
		resetRepositoryToCreateInitialTag();
		String ignoredName = "to-be-ignored.txt";

		IProject proj = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);

		IFile ignoredFile = proj.getFile(ignoredName);
		ignoredFile.create(new ByteArrayInputStream("content of ignored file"
				.getBytes(proj.getDefaultCharset())), false, null);

		IFile gitignore = proj.getFile(".gitignore");
		gitignore.create(
				new ByteArrayInputStream(ignoredName.getBytes(proj
						.getDefaultCharset())), false, null);
		proj.refreshLocal(IResource.DEPTH_INFINITE, null);

		// when
		launchSynchronization(SynchronizeWithAction_tagsName, INITIAL_TAG,
				SynchronizeWithAction_localRepoName, HEAD, true);

		// then
		// asserts for Git Change Set model
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();
		syncViewTree.expandNode(UIText.GitModelWorkingTree_workingTree);
		assertEquals(1, syncViewTree.getAllItems().length);
		SWTBotTreeItem proj1Node = syncViewTree.getAllItems()[0];
		proj1Node.getItems()[0].expand();
		assertEquals(1, proj1Node.getItems()[0].getItems().length);

		// asserts for Workspace model
		syncViewTree = setPresentationModel("Workspace").tree();
		SWTBotTreeItem projectTree = waitForNodeWithText(syncViewTree, PROJ1);
		projectTree.expand();
		assertEquals(1, projectTree.getItems().length);
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
		bot.comboBox(0).setSelection("None");

		bot.comboBox().setSelection("None");

		bot.button(IDialogConstants.OK_LABEL).click();

		repositoryFile = createProjectAndCommitToRepository();
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

	private void resetRepositoryToCreateInitialTag() throws Exception {
		ResetOperation rop = new ResetOperation(
				lookupRepository(repositoryFile), Constants.R_TAGS +
						INITIAL_TAG, ResetType.HARD);
		rop.execute(new NullProgressMonitor());
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
		showDialog(projectName, "Team", CommitAction_commit);

		bot.shell(CommitDialog_CommitChanges).bot().activeShell();
		bot.styledText(0).setText(TEST_COMMIT_MSG);
		bot.button(CommitDialog_SelectAll).click();
		bot.button(CommitDialog_Commit).click();
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
		launchSynchronization(REPO1, PROJ1, srcRepo, srcRef, dstRepo, dstRef,
				includeLocal);
	}

	private void launchSynchronization(String repo, String projectName,
			String srcRepo, String srcRef, String dstRepo, String dstRef,
			boolean includeLocal) {
		showDialog(projectName, "Team", "Synchronize...");

		bot.shell("Synchronize repository: " + repo + File.separator + ".git")
				.activate();

		if (!includeLocal)
			bot.checkBox(
					UIText.SelectSynchronizeResourceDialog_includeUncommitedChanges)
					.click();

		if (!includeLocal && srcRepo != null)
			bot.comboBox(0)
					.setSelection(srcRepo);
		if (!includeLocal && srcRef != null)
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

		public void done(IJobChangeEvent event) {
			if (event.getJob().belongsTo(
					ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION))
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
		File gitDir = new File(new File(getTestDirectory(), EMPTY_REPOSITORY),
				Constants.DOT_GIT);
		gitDir.mkdir();
		Repository myRepository = new FileRepository(gitDir);
		myRepository.create();

		// we need to commit into master first
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(EMPTY_PROJECT);

		if (firstProject.exists())
			firstProject.delete(true, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(EMPTY_PROJECT);
		desc.setLocation(new Path(new File(myRepository.getWorkTree(),
				EMPTY_PROJECT).getPath()));
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

	private SWTBotEditor getCompareEditorForFileInGitChangeSet(String fileName,
			boolean includeLocalChanges) {
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		SWTBotTreeItem rootTree;
		if (includeLocalChanges)
			rootTree = waitForNodeWithText(syncViewTree,
					GitModelWorkingTree_workingTree);
		else
			rootTree = waitForNodeWithText(syncViewTree, TEST_COMMIT_MSG);

		SWTBotTreeItem projNode = waitForNodeWithText(rootTree, PROJ1);
		SWTBotTreeItem folderNode = waitForNodeWithText(projNode, FOLDER);
		waitForNodeWithText(folderNode, fileName).doubleClick();

		SWTBotEditor editor = bot.editorByTitle(fileName);
		editor.toTextEditor().setFocus();

		return editor;
	}

	private SWTBotTreeItem waitForNodeWithText(SWTBotTree tree, String name) {
		waitUntilTreeHasNodeContainsText(bot, tree, name, 10000);
		return getTreeItemContainingText(tree.getAllItems(), name).expand();
	}

	private SWTBotTreeItem waitForNodeWithText(SWTBotTreeItem tree, String name) {
		waitUntilTreeHasNodeContainsText(bot, tree, name, 15000);
		return getTreeItemContainingText(tree.getItems(), name).expand();
	}

	private SWTBotTreeItem getTreeItemContainingText(SWTBotTreeItem[] items,
			String text) {
		for (SWTBotTreeItem item : items)
			if (item.getText().contains(text))
				return item;

		throw new WidgetNotFoundException(
					"Tree item elment containing text: test commit was not found");
	}

	private SWTBotEditor getCompareEditorForFileInWorkspaceModel()
			throws Exception {
		SWTBotTree syncViewTree = setPresentationModel("Workspace").tree();
		SWTBotTreeItem projectTree = waitForNodeWithText(syncViewTree, PROJ1);
		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		waitForNodeWithText(folderTree, FILE1).doubleClick();

		SWTBotEditor editor = bot.editorByTitle(FILE1);
		editor.toTextEditor().setFocus();

		return editor;
	}

}

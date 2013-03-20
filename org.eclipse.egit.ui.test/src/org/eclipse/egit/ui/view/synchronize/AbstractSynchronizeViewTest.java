/*******************************************************************************
 * Copyright (C) 2010-2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.internal.UIText.CommitAction_commit;
import static org.eclipse.egit.ui.internal.UIText.CommitDialog_Commit;
import static org.eclipse.egit.ui.internal.UIText.CommitDialog_CommitChanges;
import static org.eclipse.egit.ui.internal.UIText.CommitDialog_SelectAll;
import static org.eclipse.egit.ui.test.ContextMenuHelper.clickContextMenu;
import static org.eclipse.egit.ui.test.TestUtil.waitUntilTreeHasNodeContainsText;
import static org.eclipse.jface.dialogs.MessageDialogWithToggle.NEVER;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.eclipse.team.internal.ui.IPreferenceIds.SYNCHRONIZING_COMPLETE_PERSPECTIVE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.op.CommitOperation;
import org.eclipse.egit.core.internal.op.ConnectProviderOperation;
import org.eclipse.egit.core.internal.project.RepositoryMapping;
import org.eclipse.egit.core.internal.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIPreferences;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

@SuppressWarnings("restriction")
public abstract class AbstractSynchronizeViewTest extends
		LocalRepositoryTestCase {

	protected static final String INITIAL_TAG = R_TAGS + "initial-tag";

	protected static final String TEST_COMMIT_MSG = "test commit";

	protected static final String EMPTY_PROJECT = "EmptyProject";

	protected static final String EMPTY_REPOSITORY = "EmptyRepository";

	protected File repositoryFile;

	protected File childRepositoryFile;

	@Before public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
	}

	@After
	public void closeSynchronizeView() {
		SWTBotView syncView = bot.viewByTitle("Synchronize");
		syncView.close();
	}

	@Before
	public void setupRepository() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		createAndCommitDotGitignore();

		childRepositoryFile = createChildRepository(repositoryFile);

		createTag(INITIAL_TAG);

		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);
	}

	@After
	public void deleteRepository() throws Exception {
		deleteAllProjects();
		shutDownRepositories();
		FileUtils.delete(repositoryFile.getParentFile(), FileUtils.RECURSIVE | FileUtils.RETRY);
		FileUtils.delete(childRepositoryFile.getParentFile(), FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	@BeforeClass public static void setupEnvironment() throws Exception {
		// disable perspective synchronize selection
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(
				SYNCHRONIZING_COMPLETE_PERSPECTIVE, NEVER);
		Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH, false);

		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
	}

	protected void changeFilesInProject() throws Exception {
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

	protected void createTag(String tagName)
			throws Exception {
		new Git(lookupRepository(repositoryFile)).tag().setName(tagName)
				.setMessage(tagName).call();
	}

	protected void makeChangesAndCommit(String projectName) throws Exception {
		changeFilesInProject();
		commit(projectName);
	}

	protected void deleteFileAndCommit(String projectName) throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt")).delete(true, null);
		commit(projectName);
	}

	protected void launchSynchronization(String srcRef, String dstRef,
			boolean includeLocal) throws IOException {
		launchSynchronization(PROJ1, srcRef, dstRef, includeLocal);
	}

	protected void launchSynchronization(String projectName, String srcRef,
			String dstRef, boolean includeLocal) throws IOException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		Repository repo = RepositoryMapping.getMapping(project).getRepository();

		GitSynchronizeData data = new GitSynchronizeData(repo, srcRef, dstRef,
				includeLocal);

		JobJoiner jobJoiner = JobJoiner.startListening(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, 60,
				TimeUnit.SECONDS);
		GitModelSynchronize.launch(data, new IResource[] { project });
		jobJoiner.join();
	}

	protected void setEnabledModelProvider(String modelProviderId) {
		ITeamContentProviderManager contentProviderManager = TeamUI.getTeamContentProviderManager();
		ITeamContentProviderDescriptor descriptor = contentProviderManager.getDescriptor(modelProviderId);
		contentProviderManager.setEnabledDescriptors(new ITeamContentProviderDescriptor[] { descriptor });
	}

	// based on LocalRepositoryTestCase#createProjectAndCommitToRepository(String)
	protected void createEmptyRepository() throws Exception {
		File gitDir = new File(new File(getTestDirectory(), EMPTY_REPOSITORY),
				Constants.DOT_GIT);
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

	protected SWTBotTreeItem waitForNodeWithText(SWTBotTree tree, String name) {
		waitUntilTreeHasNodeContainsText(bot, tree, name, 10000);
		return getTreeItemContainingText(tree.getAllItems(), name).expand();
	}

	protected SWTBotTreeItem waitForNodeWithText(SWTBotTreeItem tree,
			String name) {
		waitUntilTreeHasNodeContainsText(bot, tree, name, 15000);
		return getTreeItemContainingText(tree.getItems(), name).expand();
	}

	private static void createAndCommitDotGitignore() throws CoreException,
			UnsupportedEncodingException {
		IProject secondPoject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ2);

		IFile gitignore = secondPoject.getFile(".gitignore");
		gitignore.create(
				new ByteArrayInputStream("/.project\n".getBytes(secondPoject
						.getDefaultCharset())), false, null);

		IFile[] commitables = new IFile[] { gitignore };
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));

		CommitOperation op = new CommitOperation(commitables,
				untracked, TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER,
				"Add .gitignore file");
		op.execute(null);
	}

	protected void commit(String projectName) throws InterruptedException {
		showDialog(projectName, "Team", CommitAction_commit);

		SWTBot shellBot = bot.shell(CommitDialog_CommitChanges).bot();
		shellBot.styledText(0).setText(TEST_COMMIT_MSG);
		shellBot.toolbarButtonWithTooltip(CommitDialog_SelectAll).click();
		shellBot.button(CommitDialog_Commit).click();
		TestUtil.joinJobs(JobFamilies.COMMIT);
	}

	protected SWTBotEditor getCompareEditor(SWTBotTreeItem projectNode,
			final String fileName) {
		SWTBotTreeItem folderNode = waitForNodeWithText(projectNode, FOLDER);
		waitForNodeWithText(folderNode, fileName).doubleClick();

		SWTBotEditor editor = bot
				.editor(new CompareEditorTitleMatcher(fileName));
		// Ensure that both StyledText widgets are enabled
		SWTBotStyledText styledText = editor.toTextEditor().getStyledText();
		bot.waitUntil(Conditions.widgetIsEnabled(styledText));
		return editor;
	}

	private static void showDialog(String projectName, String... cmd) {
		SWTBot packageExplorerBot = bot.viewByTitle("Package Explorer").bot();
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
		for (SWTBotTreeItem item : tree.getAllItems())
			if (item.getText().contains(projectName)) {
				item.select();
				return item;
			}

		throw new RuntimeException("Project with name " + projectName +
				" was not found in given tree");
	}

	private SWTBotTreeItem getTreeItemContainingText(SWTBotTreeItem[] items,
			String text) {
		List<String> existingItems = new ArrayList<String>();
		for (SWTBotTreeItem item : items) {
			if (item.getText().contains(text))
				return item;
			existingItems.add(item.getText());
		}

		throw new WidgetNotFoundException(
				"Tree item element containing text \"" + text
						+ "\" was not found. Existing tree items:\n"
						+ StringUtils.join(existingItems, "\n"));
	}
}

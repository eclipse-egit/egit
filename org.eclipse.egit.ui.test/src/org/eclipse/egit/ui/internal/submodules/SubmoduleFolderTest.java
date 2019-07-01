/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.submodules;

import static org.eclipse.egit.ui.JobFamilies.ADD_TO_INDEX;
import static org.eclipse.egit.ui.JobFamilies.GENERATE_HISTORY;
import static org.eclipse.egit.ui.JobFamilies.REMOVE_FROM_INDEX;
import static org.eclipse.egit.ui.JobFamilies.REPO_VIEW_REFRESH;
import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.ProjectRecord;
import org.eclipse.egit.ui.internal.clone.ProjectUtils;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.resources.IResourceState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SubmoduleFolderTest extends LocalRepositoryTestCase {

	private static final String SUBFOLDER = "sub";

	private static final String CHILD = "child";

	private static final String CHILDPROJECT = "ChildProject";

	private static final TestUtil UTIL = new TestUtil();

	private Repository parentRepository;

	private Repository childRepository;

	private Repository subRepository;

	private IProject parentProject;

	private IProject childProject;

	private IFolder childFolder;

	private File parentRepositoryGitDir;

	private File childRepositoryGitDir;

	private File subRepositoryGitDir;

	@Before
	public void setUp() throws Exception {
		parentRepositoryGitDir = createProjectAndCommitToRepository();
		childRepositoryGitDir = createProjectAndCommitToRepository(CHILDREPO,
				CHILDPROJECT);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(parentRepositoryGitDir);
		parentRepository = lookupRepository(parentRepositoryGitDir);
		childRepository = lookupRepository(childRepositoryGitDir);
		parentProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		IFolder folder = parentProject.getFolder(FOLDER);
		IFolder subfolder = folder.getFolder(SUBFOLDER);
		subfolder.create(false, true, null);
		assertTrue(subfolder.exists());
		IFile someFile = subfolder.getFile("dummy.txt");
		touch(PROJ1, someFile.getProjectRelativePath().toOSString(),
				"Dummy content");
		addAndCommit(someFile, "Commit sub/dummy.txt");
		childFolder = subfolder.getFolder(CHILD);
		Git.wrap(parentRepository).submoduleAdd()
				.setPath(childFolder.getFullPath().toPortableString())
				.setURI(childRepository.getDirectory().toURI()
						.toString())
				.call();
		TestRepository parentRepo = new TestRepository(parentRepository);
		Git.wrap(parentRepository).add().addFilepattern(".").call();
		parentRepo.commit("Commit submodule");
		assertTrue(SubmoduleWalk.containsGitModulesFile(parentRepository));
		parentProject.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue(childFolder.exists());
		// Let's get rid of the child project imported directly from the child
		// repository.
		childProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(CHILDPROJECT);
		childProject.delete(false, true, null);
		// Re-import it from the parent repo's submodule!
		IFile projectFile = childFolder.getFolder(CHILDPROJECT)
				.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue(projectFile.exists());
		ProjectRecord pr = new ProjectRecord(
				projectFile.getLocation().toFile());
		ProjectUtils.createProjects(Collections.singleton(pr), null, null);
		assertTrue(childProject.isOpen());
		// Now we have a parent repo in a state as if we had recursively
		// cloned some remote repo with a submodule and then imported all
		// projects. Look up the submodule repository instance through the
		// repository cache, so that we get the same instance that EGit
		// uses.
		subRepository = SubmoduleWalk.getSubmoduleRepository(
				childFolder.getParent().getLocation().toFile(), CHILD);
		assertNotNull(subRepository);
		subRepositoryGitDir = subRepository.getDirectory();
		subRepository.close();
		subRepository = lookupRepository(subRepositoryGitDir);
		assertNotNull(subRepository);
	}

	@After
	public void removeConfiguredRepositories() {
		if (parentRepositoryGitDir != null) {
			Activator.getDefault().getRepositoryUtil()
					.removeDir(parentRepositoryGitDir);
		}
		if (childRepositoryGitDir != null) {
			Activator.getDefault().getRepositoryUtil()
					.removeDir(childRepositoryGitDir);
		}
		childRepository = null;
		parentRepository = null;
		subRepository = null;
	}

	@Test
	public void testChildProjectMapsToSubRepo() {
		RepositoryMapping mapping = RepositoryMapping.getMapping(childProject);
		assertNotNull("Child project should have a mapping", mapping);
		assertEquals(subRepository, mapping.getRepository());
	}

	@Test
	public void testChildFolderMapsToSubRepo() {
		RepositoryMapping mapping = RepositoryMapping.getMapping(childFolder);
		assertNotNull("Child folder should have a mapping", mapping);
		assertEquals(subRepository, mapping.getRepository());
	}

	@Test
	public void testParentFolderMapsToParentRepo() {
		RepositoryMapping mapping = RepositoryMapping
				.getMapping(childFolder.getParent());
		assertNotNull("Child folder's parent should have a mapping", mapping);
		assertEquals(parentRepository, mapping.getRepository());
	}

	/**
	 * Tests AddToIndex and RemoveFromIndex commands on a file from a submodule
	 * folder. Verifies the execution of the command by testing the state of the
	 * file in the index diff after it has been executed. Additionally verifies
	 * that decorations do get updated.
	 *
	 * @throws Exception
	 */
	@Test
	public void testStageUnstageInSubRepo() throws Exception {
		IFolder childProjectFolder = childFolder.getFolder(CHILDPROJECT);
		IFolder folder = childProjectFolder.getFolder(FOLDER);
		IFile file = folder.getFile(FILE1);
		touch(PROJ1, file.getProjectRelativePath().toOSString(), "Modified");
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem node = TestUtil.navigateTo(projectExplorerTree,
				file.getFullPath().segments());
		TestUtil.waitForDecorations();
		assertTrue(node.getText().startsWith("> " + file.getName()));
		node.select();
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				util.getPluginLocalizedValue("AddToIndexAction_label"));
		TestUtil.joinJobs(ADD_TO_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		IndexDiffCacheEntry cache = Activator.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(subRepository);
		IResourceState state = ResourceStateFactory.getInstance()
				.get(cache.getIndexDiff(), file);
		assertTrue("File should be staged", state.isStaged());
		TestUtil.waitForDecorations();
		assertFalse(node.getText().startsWith("> "));
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				util.getPluginLocalizedValue("RemoveFromIndexAction_label"));
		TestUtil.joinJobs(REMOVE_FROM_INDEX);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		state = ResourceStateFactory.getInstance().get(cache.getIndexDiff(),
				file);
		assertFalse("File should not be staged", state.isStaged());
		assertTrue("File should be dirty", state.isDirty());
		TestUtil.waitForDecorations();
		assertTrue(node.getText().startsWith("> " + file.getName()));
	}

	/**
	 * Tests that the Team->Switch To... menu item has content by clicking on
	 * "New Branch..." and then closing the resulting "Create Branch" dialog.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSwitchToMenu() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem node = TestUtil.navigateTo(projectExplorerTree,
				childFolder.getFullPath().segments());
		TestUtil.waitForDecorations();
		node.select();
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				util.getPluginLocalizedValue("SwitchToMenu.label"),
				UIText.SwitchToMenu_NewBranchMenuLabel);

		SWTBotShell shell = bot.shell(UIText.CreateBranchWizard_NewBranchTitle);
		shell.close();
	}

	@Test
	public void testRepoViewFollowSelection() throws Exception {
		SWTBotView view = TestUtil.showView(RepositoriesView.VIEW_ID);
		TestUtil.joinJobs(REPO_VIEW_REFRESH);
		view.toolbarButton(
				UTIL.getPluginLocalizedValue("LinkWithSelectionCommand"))
				.click();
		try {
			SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
			SWTBotTreeItem node = TestUtil.navigateTo(projectExplorerTree,
					childFolder.getFullPath().segments());
			node.select();
			TestUtil.waitForDecorations();
			TestUtil.joinJobs(REPO_VIEW_REFRESH);
			SWTBotTree tree = view.bot().tree();
			int[] numberOfSelected = { 0 };
			boolean[] parentFound = { false };
			view.getWidget().getDisplay().syncExec(() -> {
				Tree t = tree.widget;
				TreeItem[] selected = t.getSelection();
				numberOfSelected[0] = selected.length;
				if (selected.length == 1) {
					TreeItem root = null;
					TreeItem parent = selected[0].getParentItem();
					String parentRepoName = parentRepositoryGitDir
							.getParentFile().getName();
					while (parent != null) {
						root = parent;
						parent = parent.getParentItem();
					}
					if (root != null
							&& root.getText().startsWith(parentRepoName)) {
						parentFound[0] = true;
					}
				}
			});
			assertEquals("One node selected", 1, numberOfSelected[0]);
			assertTrue("Selected node not under parent repository",
					parentFound[0]);
		} finally {
			// Reset "follow selection"
			view.toolbarButton(
					UTIL.getPluginLocalizedValue("LinkWithSelectionCommand"))
					.click();
		}
	}

	/**
	 * Tests that a CompareWithHeadAction on a file from a submodule folder does
	 * open the right compare editor, comparing against the version from the
	 * submodule (as opposed to the version from the parent repo).
	 *
	 * @throws Exception
	 */
	@Test
	public void compareWithHeadInSubmoduleFolder() throws Exception {
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=446344#c11
		// If the compare editor's title does not contain the HEAD id of
		// the subrepo, then either no compare editor got opened, or
		// it was opened using the parent repo.
		IFolder childProjectFolder = childFolder.getFolder(CHILDPROJECT);
		IFolder folder = childProjectFolder.getFolder(FOLDER);
		IFile file = folder.getFile(FILE1);
		touch(PROJ1, file.getProjectRelativePath().toOSString(), "Modified");
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem node = TestUtil.navigateTo(projectExplorerTree,
				file.getFullPath().segments());
		node.select();
		Ref headRef = subRepository.findRef(Constants.HEAD);
		final String headId = Utils.getShortObjectId(headRef.getObjectId());
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree,
				"Compare With",
				util.getPluginLocalizedValue("CompareWithHeadAction_label"));
		bot.waitUntil(waitForEditor(new BaseMatcher<IEditorReference>() {

			@Override
			public boolean matches(Object item) {
				return (item instanceof IEditorReference)
						&& ((IEditorReference) item).getTitle()
								.contains(headId);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Wait for editor containing " + headId);
			}
		}), 5000);
	}

	@Test
	public void testDisconnect() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util
				.getPluginLocalizedValue("DisconnectAction_label");
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				menuString);
		TestUtil.waitForJobs(500, 5000);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		ResourcesPlugin.getWorkspace().getRoot()
				.refreshLocal(IResource.DEPTH_INFINITE, null);
		// Access the session property directly: RepositoryMapping.getMapping()
		// checks whether the project is shared with git.
		Object mapping = childFolder.getSessionProperty(new QualifiedName(
				GitProjectData.class.getName(), "RepositoryMapping"));
		assertNull("Should have no RepositoryMapping", mapping);
	}

	@Test
	public void testDecoration() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem node = TestUtil.navigateTo(projectExplorerTree,
				childFolder.getFullPath().segments());
		TestUtil.waitForDecorations();
		assertTrue("Folder should have repo/branch decoration",
				node.getText().contains("[master"));
		TestUtil.expandAndWait(node);
		node = TestUtil.getChildNode(node, CHILDPROJECT);
		TestUtil.waitForDecorations();
		assertFalse("Folder should not have repo/branch decoration",
				node.getText().contains("["));
		node = TestUtil.navigateTo(projectExplorerTree, CHILDPROJECT);
		TestUtil.waitForDecorations();
		assertTrue("Project should have subrepo/branch decoration",
				node.getText().contains("[child"));
	}

	/**
	 * Tests that unrelated changes to the configured repositories do not
	 * prematurely remove submodules from the cache.
	 */
	@Test
	public void testRepoRemoval() {
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(childRepositoryGitDir);
		assertTrue("Should still have the subrepo in the cache",
				containsRepo(Activator.getDefault().getRepositoryCache()
						.getAllRepositories(), subRepository));
		assertTrue("Should have changed the preference", Activator.getDefault()
				.getRepositoryUtil().removeDir(childRepositoryGitDir));
		assertTrue("Should still have the subrepo in the cache",
				containsRepo(Activator.getDefault().getRepositoryCache()
						.getAllRepositories(), subRepository));
	}

	@SuppressWarnings("restriction")
	@Test
	public void testHistoryFromProjectExplorerIsFromSubRepository()
			throws Exception {
		// Open history view
		SWTBotView historyBot = TestUtil.showHistoryView();
		IViewPart viewPart = historyBot.getViewReference().getView(false);
		assertTrue(
				viewPart instanceof org.eclipse.team.internal.ui.history.GenericHistoryView);
		// Set link with selection
		((org.eclipse.team.internal.ui.history.GenericHistoryView) viewPart)
				.setLinkingEnabled(true);
		// Select PROJ1 (has 4 commits)
		TestUtil.navigateTo(TestUtil.getExplorerTree(), PROJ1).select();
		assertRowCountInHistory(PROJ1, 4);
		// Select the child folder (from the submodule; has 2 commits)
		TestUtil.navigateTo(TestUtil.getExplorerTree(),
				childFolder.getFullPath().segments()).select();
		assertRowCountInHistory(childFolder.getFullPath() + " from submodule",
				2);
	}

	private boolean containsRepo(Repository[] repositories, Repository needle) {
		for (Repository repo : repositories) {
			if (needle.equals(repo)) {
				return true;
			}
		}
		return false;
	}

	private void assertRowCountInHistory(String msg, int expected)
			throws Exception {
		SWTBotView historyBot = TestUtil.showHistoryView();
		Job.getJobManager().join(GENERATE_HISTORY, null);
		historyBot.getWidget().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				// Joins UI update triggered by GenerateHistoryJob
			}
		});
		assertEquals(msg + " should show " + expected + " commits", expected,
				historyBot.bot().table().rowCount());
	}

}

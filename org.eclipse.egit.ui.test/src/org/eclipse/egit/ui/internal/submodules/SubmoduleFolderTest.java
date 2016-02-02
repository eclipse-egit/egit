package org.eclipse.egit.ui.internal.submodules;

import static org.eclipse.egit.ui.JobFamilies.GENERATE_HISTORY;
import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.clone.ProjectRecord;
import org.eclipse.egit.ui.internal.clone.ProjectUtils;
import org.eclipse.egit.ui.internal.resources.IResourceState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SubmoduleFolderTest extends LocalRepositoryTestCase {

	private static final String SUBFOLDER = "sub";

	private static final String CHILD = "child";

	private static final String CHILDPROJECT = "ChildProject";

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
		parentRepo.trackAllFiles(parentProject);
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

	@Test
	public void testStageUnstageInSubRepo() throws Exception {
		IFolder childProjectFolder = childFolder.getFolder(CHILDPROJECT);
		IFolder folder = childProjectFolder.getFolder(FOLDER);
		IFile file = folder.getFile(FILE1);
		touch(PROJ1, file.getProjectRelativePath().toOSString(), "Modified");
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem node = TestUtil.navigateTo(projectExplorerTree,
				file.getFullPath().segments());
		node.select();
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				util.getPluginLocalizedValue("AddToIndexAction_label"));
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		IndexDiffCacheEntry cache = Activator.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(subRepository);
		IResourceState state = ResourceStateFactory.getInstance()
				.get(cache.getIndexDiff(), file);
		assertTrue("File should be staged", state.isStaged());
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				util.getPluginLocalizedValue("RemoveFromIndexAction_label"));
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		state = ResourceStateFactory.getInstance().get(cache.getIndexDiff(),
				file);
		assertFalse("File should not be staged", state.isStaged());
		assertTrue("File should be dirty", state.isDirty());
	}

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
		final String headId = headRef.getObjectId().abbreviate(6).name();
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

	@SuppressWarnings("restriction")
	@Test
	public void testHistoryFromProjectExplorerIsFromSubRepository()
			throws Exception {
		// Open history view
		final SWTBotView historyBot = TestUtil.showHistoryView();
		IViewPart viewPart = historyBot.getViewReference().getView(false);
		assertTrue(
				viewPart instanceof org.eclipse.team.internal.ui.history.GenericHistoryView);
		// Set link with selection
		((org.eclipse.team.internal.ui.history.GenericHistoryView) viewPart)
				.setLinkingEnabled(true);
		// Select PROJ1 (has 3 commits)
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		TestUtil.navigateTo(projectExplorerTree, PROJ1).select();
		Job.getJobManager().join(GENERATE_HISTORY, null);
		// Join UI update triggered by GenerateHistoryJob
		projectExplorerTree.widget.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				assertEquals("GeneralProject should show 3 commits", 3,
						historyBot.bot().table().rowCount());
			}
		});
		// Select the child folder (from the submodule; has 2 commits)
		TestUtil.navigateTo(projectExplorerTree,
				childFolder.getFullPath().segments()).select();
		Job.getJobManager().join(GENERATE_HISTORY, null);
		// join UI update triggered by GenerateHistoryJob
		projectExplorerTree.widget.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				assertEquals(
						"child folder from submodule should show 2 commits", 2,
						historyBot.bot().table().rowCount());
			}
		});
	}

}

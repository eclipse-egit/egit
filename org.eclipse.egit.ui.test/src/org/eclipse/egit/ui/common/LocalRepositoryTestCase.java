/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.rebase.RebaseInteractiveView;
import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Base class for testing with local (file-system based) repositories
 * <p>
 * This supports single repository scenarios as well as "remote" scenarios with
 * two local repositories that are interconnected with each other via the
 * "origin" configuration.
 * <p>
 * The repositories are created under a directory named "LocalRepositoriesTests"
 * under the user home directory and everything is deleted recursively in
 * {@link #afterClassBase()}.
 * <p>
 * {@link #createProjectAndCommitToRepository()} creates a couple of projects
 * and adds them to a local repository named {@link #REPO1}.
 * <p>
 * {@link #createRemoteRepository(File)} creates a bare repository based on the
 * File of another repository. The original repository will be configured with
 * three remote specifications that can be used to push to the bare repository
 * with slightly different set-ups (combinations of urls and specs): fetch,
 * push, both, and mixed
 * <p>
 * A typical code sequence for setting up these two repositories could look
 * like:
 *
 * <pre>
 *  private File localRepo;
 *  private File remoteRepo;
 * ...
 * {@literal @}Before
 *  public void initRepos() throws Exception {
 *     localRepo = repositoryFile = createProjectAndCommitToRepository();
 *     remtoeRepo =remoteRepositoryFile = createRemoteRepository(repositoryFile);
 *  }
 * </pre>
 * <p>
 * {@link #createChildRepository(File)} creates a "child" repository based on
 * the File another repository; the child will be cloned from the original
 * repository and a "origin" remote spec will be set-up automatically
 */
public abstract class LocalRepositoryTestCase extends EGitTestCase {

	private static int testMethodNumber = 0;

	// the temporary directory
	private File testDirectory;

	protected static final String REPO1 = "FirstRepository";

	protected static final String REPO2 = "RemoteRepository";

	protected static final String CHILDREPO = "ChildRepository";

	/** A general project containing FOLDER containing FILE1 and FILE2 */
	protected static final String PROJ1 = "GeneralProject";

	/** A folder obtained by checking in a project without .project */
	protected static final String PROJ2 = "ProjectWithoutDotProject";

	protected static final String FOLDER = "folder";

	protected static final String FILE1 = "test.txt";

	protected static final String FILE1_PATH = PROJ1 + "/" + FOLDER + "/"
			+ FILE1;

	protected static final String FILE2 = "test2.txt";

	protected final static TestUtils testUtils = new TestUtils();

	private static final String[] VIEWS_TO_CLOSE = { //
			RebaseInteractiveView.VIEW_ID, //
			ISynchronizeView.VIEW_ID, //
			IHistoryView.VIEW_ID, //
			CompareTreeView.ID, //
			ReflogView.VIEW_ID, //
			StagingView.VIEW_ID, //
			RepositoriesView.VIEW_ID, //
			"org.eclipse.search.ui.views.SearchView", //
			"org.eclipse.ui.views.PropertySheet"
	};

	@Rule
	public TestName testName = new TestName();

	public File getTestDirectory() {
		return testDirectory;
	}

	protected static void closeGitViews() {
		for (String viewId : VIEWS_TO_CLOSE) {
			TestUtil.hideView(viewId);
		}
	}

	@Before
	public void initNewTestDirectory() throws Exception {
		testMethodNumber++;
		// create standalone temporary directory
		testDirectory = testUtils.createTempDir("LocalRepositoriesTests"
				+ testMethodNumber + '_' + testName.getMethodName());
		if (testDirectory.exists())
			FileUtils.delete(testDirectory, FileUtils.RECURSIVE
					| FileUtils.RETRY);
		if (!testDirectory.exists())
			FileUtils.mkdir(testDirectory, true);
		// we don't want to clone into <user_home> but into our test directory
		File repoRoot = new File(testDirectory, "RepositoryRoot");
		if (!repoRoot.exists())
			FileUtils.mkdir(repoRoot, true);
		// make sure the default directory for Repos is not the user home
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode(Activator.getPluginId());
		p.put(GitCorePreferences.core_defaultRepositoryDir, repoRoot.getPath());

		File configFile = File.createTempFile("gitconfigtest", "config");
		MockSystemReader mockSystemReader = new MockSystemReader() {
			@Override
			public FileBasedConfig openUserConfig(Config parent, FS fs) {
				return new FileBasedConfig(parent, configFile, fs);
			}
		};
		// unset git user properties
		mockSystemReader.setProperty(Constants.GIT_AUTHOR_NAME_KEY, null);
		mockSystemReader.setProperty(Constants.GIT_AUTHOR_EMAIL_KEY, null);
		mockSystemReader.setProperty(Constants.GIT_COMMITTER_NAME_KEY, null);
		mockSystemReader.setProperty(Constants.GIT_COMMITTER_EMAIL_KEY, null);
		configFile.deleteOnExit();
		SystemReader.setInstance(mockSystemReader);
		mockSystemReader.setProperty(Constants.GIT_CEILING_DIRECTORIES_KEY,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
						.getParentFile().getAbsoluteFile().toString());
		FileBasedConfig userConfig = mockSystemReader.openUserConfig(null,
				FS.DETECTED);
		// We have to set autoDetach to false for tests, because tests expect to
		// be able to clean up by recursively removing the repository, and
		// background GC might be in the middle of writing or deleting files,
		// which would disrupt this.
		userConfig.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null,
				ConfigConstants.CONFIG_KEY_AUTODETACH, false);
		userConfig.save();
	}

	@After
	public void resetWorkspace() throws Exception {
		TestUtil.processUIEvents();
		// close all editors/dialogs
		new Eclipse().reset();
		closeGitViews();
		TestUtil.processUIEvents();
		// cleanup
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			project.delete(false, false, null);
		}
		shutDownRepositories();
		TestUtil.waitForJobs(50, 5000);
	}

	@BeforeClass
	public static void beforeClassBase() throws Exception {
		// suppress auto-ignoring and auto-sharing to avoid interference
		IEclipsePreferences corePrefs = InstanceScope.INSTANCE
				.getNode(org.eclipse.egit.core.Activator.getPluginId());
		corePrefs.putBoolean(
				GitCorePreferences.core_autoIgnoreDerivedResources, false);
		corePrefs.putBoolean(GitCorePreferences.core_autoShareProjects, false);
		// suppress the configuration dialog
		org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.SHOW_INITIAL_CONFIG_DIALOG, false);
		// suppress the detached head warning dialog
		org.eclipse.egit.ui.Activator
				.getDefault()
				.getPreferenceStore()
				.setValue(UIPreferences.SHOW_DETACHED_HEAD_WARNING,
						false);
		closeGitViews();
	}

	@AfterClass
	public static void afterClassBase() throws Exception {
		File tempDir = testUtils.getBaseTempDir();
		if (tempDir.toString().startsWith("/home") && tempDir.exists()) {
			// see bug 440182: if test has left opened file streams on NFS
			// mounted directories "delete" will fail because the directory
			// would contain "stolen NFS file handles"
			// (something like .nfs* files)
			// so the "first round" of delete can ignore failures.
			FileUtils.delete(tempDir, FileUtils.IGNORE_ERRORS
					| FileUtils.RECURSIVE | FileUtils.RETRY);
		}
		testUtils.deleteTempDirs();
	}

	protected static void shutDownRepositories() throws Exception {
		RepositoryCache cache = Activator.getDefault().getRepositoryCache();
		for(Repository repository:cache.getAllRepositories())
			repository.close();
		cache.clear();
		IEclipsePreferences prefs = Activator.getDefault().getRepositoryUtil()
				.getPreferences();
		synchronized (prefs) {
			prefs.put(RepositoryUtil.PREFS_DIRECTORIES, "");
			prefs.put(RepositoryUtil.PREFS_DIRECTORIES_REL, "");
			prefs.flush();
		}
	}

	protected static void deleteAllProjects() throws Exception {
		for (IProject prj : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			if (prj.getName().equals(PROJ1)) {
				prj.delete(false, false, null);
			} else if (prj.getName().equals(PROJ2)) {
				// delete the .project on disk
				File dotProject = prj.getLocation().append(".project").toFile();
				prj.delete(false, false, null);
				FileUtils.delete(dotProject, FileUtils.RETRY);
			}
		}
		TestUtil.waitForJobs(50, 5000);
	}

	protected File createProjectAndCommitToRepository() throws Exception {
		return createProjectAndCommitToRepository(REPO1);
	}

	protected File createProjectAndCommitToRepository(String repoName)
			throws Exception {
		return createProjectAndCommitToRepository(repoName, PROJ1, PROJ2);
	}

	protected File createProjectAndCommitToRepository(String repoName,
			String projectName) throws Exception {
		return createProjectAndCommitToRepository(repoName, projectName, null);
	}

	protected File createProjectAndCommitToRepository(String repoName,
			String project1Name, String project2Name) throws Exception {

		Repository myRepository = createLocalTestRepository(repoName);
		File gitDir = myRepository.getDirectory();

		// we need to commit into master first
		IProject firstProject = createStandardTestProjectInRepository(
				myRepository, project1Name);

		try {
			new ConnectProviderOperation(firstProject, gitDir).execute(null);
		} catch (Exception e) {
			Activator.logError("Failed to connect project to repository", e);
		}
		assertConnected(firstProject);

		IProject secondProject = null;
		if (project2Name != null) {
			secondProject = createStandardTestProjectInRepository(myRepository,
					project2Name);

			// TODO we should be able to hide the .project
			// IFile gitignore = secondPoject.getFile(".gitignore");
			// gitignore.create(new ByteArrayInputStream("/.project\n"
			// .getBytes(firstProject.getDefaultCharset())), false, null);

			try {
				new ConnectProviderOperation(secondProject, gitDir)
						.execute(null);
			} catch (Exception e) {
				Activator.logError("Failed to connect project to repository",
						e);
			}
			assertConnected(secondProject);
		}

		IFile dotProject = firstProject.getFile(".project");
		assertTrue(".project is not accessible: " + dotProject,
				dotProject.isAccessible());
		IFolder folder = firstProject.getFolder(FOLDER);
		IFile textFile = folder.getFile(FILE1);
		IFile textFile2 = folder.getFile(FILE2);
		IFile[] commitables = null;
		if (secondProject != null) {
			folder = secondProject.getFolder(FOLDER);
			IFile secondtextFile = folder.getFile(FILE1);
			IFile secondtextFile2 = folder.getFile(FILE2);

			commitables = new IFile[] { dotProject, textFile, textFile2,
					secondtextFile, secondtextFile2 };
		} else {
			commitables = new IFile[] { dotProject, textFile, textFile2 };
		}
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		// commit to stable
		CommitOperation op = new CommitOperation(commitables,
				untracked, TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER,
				"Initial commit");
		op.execute(null);

		// now create a stable branch (from master)
		createStableBranch(myRepository);
		// and check in some stuff into master again
		String newContent = "Touched at " + System.currentTimeMillis();
		IFile file = touch(firstProject.getName(), FOLDER + '/' + FILE1,
				newContent);
		addAndCommit(file, newContent);

		// Make sure cache entry is already listening for changes
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		cache.getIndexDiffCacheEntry(lookupRepository(gitDir));

		return gitDir;
	}

	protected Repository createLocalTestRepository(String repoName)
			throws IOException {
		File gitDir = new File(new File(testDirectory, repoName),
				Constants.DOT_GIT);
		Repository myRepository = new RepositoryBuilder().setGitDir(gitDir)
				.build();
		myRepository.create();
		return myRepository;
	}

	protected IProject createStandardTestProjectInRepository(
			Repository repository, String name) throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(name);

		if (project.exists()) {
			project.delete(true, null);
			TestUtil.waitForJobs(100, 5000);
		}
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(name);
		desc.setLocation(
				new Path(new File(repository.getWorkTree(), name).getPath()));
		project.create(desc, null);
		project.open(null);
		TestUtil.waitForJobs(50, 5000);

		assertTrue("Project is not accessible: " + project,
				project.isAccessible());

		IFolder folder = project.getFolder(FOLDER);
		folder.create(false, true, null);
		IFile textFile = folder.getFile(FILE1);
		textFile.create(
				new ByteArrayInputStream(
						"Hello, world".getBytes(project.getDefaultCharset())),
				false, null);
		IFile textFile2 = folder.getFile(FILE2);
		textFile2.create(new ByteArrayInputStream(
				"Some more content".getBytes(project.getDefaultCharset())),
				false, null);
		return project;
	}

	protected RepositoryMapping assertConnected(IProject project) {
		RepositoryProvider provider = RepositoryProvider.getProvider(project,
				GitProvider.ID);
		if (provider == null) {
			TestUtil.waitForJobs(5000, 10000);
			assertTrue("Project not shared with git: " + project,
					ResourceUtil.isSharedWithGit(project));
			TestUtil.waitForJobs(1000, 10000);
			provider = RepositoryProvider.getProvider(project);
		}
		assertTrue("Project is not accessible: " + project,
				project.isAccessible());
		assertNotNull("GitProvider not mapped to: " + project, provider);

		GitProjectData data = ((GitProvider) provider).getData();
		if (data == null) {
			TestUtil.waitForJobs(100, 5000);
			data = ((GitProvider) provider).getData();
		}
		assertNotNull("GitProjectData is null for: " + project, data);

		RepositoryMapping mapping = data.getRepositoryMapping(project);
		if (mapping == null) {
			TestUtil.waitForJobs(100, 5000);
			mapping = data.getRepositoryMapping(project);
		}
		assertNotNull("RepositoryMapping is null for: " + project, mapping);
		return mapping;
	}

	protected File createRemoteRepository(File repositoryDir)
			throws Exception {
		Repository myRepository = lookupRepository(repositoryDir);
		File gitDir = new File(testDirectory, REPO2);
		Repository myRemoteRepository = FileRepositoryBuilder.create(gitDir);
		myRemoteRepository.create(true);
		// double-check that this is bare
		assertTrue(myRemoteRepository.isBare());

		createStableBranch(myRepository);

		// now we configure a pure push destination
		myRepository.getConfig().setString("remote", "push", "pushurl",
				"file:///" + myRemoteRepository.getDirectory().getPath());
		myRepository.getConfig().setString("remote", "push", "push",
				"+refs/heads/*:refs/heads/*");

		// and a pure fetch destination
		myRepository.getConfig().setString("remote", "fetch", "url",
				"file:///" + myRemoteRepository.getDirectory().getPath());
		myRepository.getConfig().setString("remote", "fetch", "fetch",
				"+refs/heads/*:refs/heads/*");

		// a destination with both fetch and push urls and specs
		myRepository.getConfig().setString("remote", "both", "pushurl",
				"file:///" + myRemoteRepository.getDirectory().getPath());
		myRepository.getConfig().setString("remote", "both", "push",
				"+refs/heads/*:refs/heads/*");
		myRepository.getConfig().setString("remote", "both", "url",
				"file:///" + myRemoteRepository.getDirectory().getPath());
		myRepository.getConfig().setString("remote", "both", "fetch",
				"+refs/heads/*:refs/heads/*");

		// a destination with only a fetch url and push and fetch specs
		myRepository.getConfig().setString("remote", "mixed", "push",
				"+refs/heads/*:refs/heads/*");
		myRepository.getConfig().setString("remote", "mixed", "url",
				"file:///" + myRemoteRepository.getDirectory().getPath());
		myRepository.getConfig().setString("remote", "mixed", "fetch",
				"+refs/heads/*:refs/heads/*");

		myRepository.getConfig().save();
		// and push
		PushOperationUI pa = new PushOperationUI(myRepository, "push", false);
		pa.execute(null);

		try {
			// delete the stable branch again
			RefUpdate op = myRepository.updateRef("refs/heads/stable");
			op.setRefLogMessage("branch deleted", //$NON-NLS-1$
					false);
			// we set the force update in order
			// to avoid having this rejected
			// due to minor issues
			op.setForceUpdate(true);
			op.delete();
		} catch (IOException ioe) {
			throw new InvocationTargetException(ioe);
		}
		return myRemoteRepository.getDirectory();
	}

	protected File createChildRepository(File repositoryDir)
			throws Exception {
		Repository myRepository = lookupRepository(repositoryDir);
		URIish uri = new URIish("file:///" + myRepository.getDirectory());
		File workdir = new File(testDirectory, CHILDREPO);
		CloneOperation clop = new CloneOperation(uri, true, null, workdir,
				"refs/heads/master", "origin", 0);
		clop.run(null);
		return new File(workdir, Constants.DOT_GIT);
	}

	protected static void createStableBranch(Repository myRepository)
			throws IOException {
		// let's create a stable branch temporarily so
		// that we push two branches to remote
		String newRefName = "refs/heads/stable";
		createBranch(myRepository, newRefName);
	}

	protected static void createBranch(Repository myRepository,
			String newRefName) throws IOException {
		RefUpdate updateRef = myRepository.updateRef(newRefName);
		Ref sourceBranch = myRepository.exactRef("refs/heads/master");
		ObjectId startAt = sourceBranch.getObjectId();
		String startBranch = Repository.shortenRefName(sourceBranch.getName());
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
		TestUtil.waitForJobs(50, 5000);
	}

	protected void assertClickOpens(SWTBotTree tree, String menu, String window) {
		ContextMenuHelper.clickContextMenu(tree, menu);
		SWTBotShell shell = bot.shell(window);
		shell.activate();
		shell.bot().button(IDialogConstants.CANCEL_LABEL).click();
		shell.close();
	}

	/**
	 *  This method should only be used in exceptional cases.
	 *  Try to avoid using it e.g. by joining execution jobs
	 *  instead of waiting a given amount of time {@link TestUtil#joinJobs(Object)}
	 * @throws InterruptedException
	 */
	protected static void waitInUI() throws InterruptedException {
		TestUtil.processUIEvents(1000);
	}

	protected void shareProjects(File repositoryDir) throws Exception {
		Repository myRepository = lookupRepository(repositoryDir);
		FilenameFilter projectFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.equals(".project");
			}
		};
		for (File file : myRepository.getWorkTree().listFiles()) {
			if (file.isDirectory()) {
				if (file.list(projectFilter).length > 0) {
					IProjectDescription desc = ResourcesPlugin.getWorkspace()
							.newProjectDescription(file.getName());
					desc.setLocation(new Path(file.getPath()));
					IProject prj = ResourcesPlugin.getWorkspace().getRoot()
							.getProject(file.getName());
					prj.create(desc, null);
					prj.open(null);
					try {
						new ConnectProviderOperation(prj,
								myRepository.getDirectory()).execute(null);
					} catch (Exception e) {
						Activator.logError(
								"Failed to connect project to repository", e);
					}
					assertConnected(prj);
				}
			}
		}
		TestUtil.waitForJobs(50, 5000);
	}

	@SuppressWarnings("boxing")
	protected void assertProjectExistence(String projectName, boolean existence) {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				projectName);
		assertEquals("Project existence " + projectName, prj.exists(),
				existence);
	}

	protected static Repository lookupRepository(File directory)
			throws Exception {
		return org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(directory);
	}

	/**
	 * Modify with a random content and commit.
	 *
	 * @param commitMessage
	 *            may be null
	 * @throws Exception
	 */
	protected static void touchAndSubmit(String commitMessage) throws Exception {
		String newContent = "Touched at " + System.currentTimeMillis();
		touchAndSubmit(newContent, commitMessage);
	}

	/**
	 * Modify with the given content and commit.
	 *
	 * @param newContent
	 *            new file content
	 * @param commitMessage
	 *            may be null
	 * @throws Exception
	 */
	protected static void touchAndSubmit(String newContent, String commitMessage)
			throws Exception {
		IFile file = touch(newContent);

		IFile[] commitables = new IFile[] { file };
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		String message = commitMessage;
		if (message == null)
			message = newContent;
		CommitOperation op = new CommitOperation(commitables,
				untracked, TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER,
				message);
		op.execute(null);
		TestUtil.waitForJobs(50, 5000);
	}

	/**
	 * Modify with the given content.
	 *
	 * @param newContent
	 *            new file content
	 * @return the modified file
	 * @throws Exception
	 */
	protected static IFile touch(final String newContent) throws Exception {
		return touch(PROJ1, "folder/test.txt", newContent);
	}

	/**
	 * Modify the specified file with the given content.
	 *
	 * @param projectName
	 *            project name
	 * @param filePath
	 *            file path under the given project
	 * @param newContent
	 *            new file content
	 * @return the modified file
	 * @throws Exception
	 */
	protected static IFile touch(String projectName, String filePath,
			String newContent) throws Exception {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		if (!prj.isAccessible())
			throw new IllegalStateException("No project to touch");
		IFile file = prj.getFile(new Path(filePath));
		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				newContent.getBytes(prj.getDefaultCharset()));
		if (!file.exists())
			file.create(inputStream, 0, null);
		else
			file.setContents(inputStream, 0, null);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		return file;
	}

	protected static void stage(IFile file) throws Exception {
		ArrayList<IFile> unstaged = new ArrayList<IFile>();
		unstaged.addAll(Arrays.asList(new IFile[] { file }));
		AddToIndexOperation op = new AddToIndexOperation(unstaged);
		op.execute(null);
	}

	protected static void addAndCommit(IFile file, String commitMessage)
			throws Exception {
		IProject prj = file.getProject();
		if (!prj.isAccessible())
			throw new IllegalStateException("No project to touch");
		IFile[] commitables = new IFile[] { file };
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		CommitOperation op = new CommitOperation(commitables,
				untracked, TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER,
				commitMessage);
		op.execute(null);
		TestUtil.waitForJobs(50, 5000);
	}

	protected static void setTestFileContent(String newContent)
			throws Exception {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				PROJ1);
		if (!prj.isAccessible())
			throw new IllegalStateException("No project found");
		IFile file = prj.getFile(new Path("folder/test.txt"));
		file.refreshLocal(0, null);
		file.setContents(new ByteArrayInputStream(newContent.getBytes(prj
				.getDefaultCharset())), 0, null);
	}

	protected String getTestFileContent() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt"));
		if (file.exists()) {
			byte[] bytes = IO.readFully(file.getLocation().toFile());
			return new String(bytes, file.getCharset());
		} else
			return "";
	}

	/**
	 * @param projectExplorerTree
	 * @param project
	 *            name of a project
	 * @return the project item pertaining to the project
	 */
	protected SWTBotTreeItem getProjectItem(SWTBotTree projectExplorerTree,
			String project) {
		return new TestUtil().getProjectItems(projectExplorerTree, project)[0];
	}

	protected void pressAltAndChar(SWTBotShell shell, char charToPress) {
		Display display = Display.getDefault();
		Event evt = new Event();
		// Alt down
		evt.type = SWT.KeyDown;
		evt.item = shell.widget;
		evt.keyCode = SWT.ALT;
		display.post(evt);
		// G down
		evt.keyCode = 0;
		evt.character = charToPress;
		display.post(evt);
		// G up
		evt.type = SWT.KeyUp;
		display.post(evt);
		// Alt up
		evt.keyCode = SWT.ALT;
		evt.character = ' ';
		display.post(evt);
	}

	protected static Collection<Ref> getRemoteRefs(URIish uri) throws Exception {
		final Repository db = FileRepositoryBuilder.create(new File("/tmp")); //$NON-NLS-1$
		int timeout = 20;
		ListRemoteOperation listRemoteOp = new ListRemoteOperation(db, uri,
				timeout);
		listRemoteOp.run(null);
		return listRemoteOp.getRemoteRefs();
	}
}

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
package org.eclipse.egit.ui.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;

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
 * {@literal @}BeforeClass
 *  public static void initRepos() throws Exception {
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

	// the temporary directory
	private static File testDirectory;

	protected static final String REPO1 = "FirstRepository";

	protected static final String REPO2 = "RemoteRepository";

	protected static final String CHILDREPO = "ChildRepository";

	/** A general project containing FOLDER containing FILE1 and FILE2 */
	protected static final String PROJ1 = "GeneralProject";

	/** A folder obtained by checking in a project without .project */
	protected static final String PROJ2 = "ProjectWithoutDotProject";

	protected static final String FILE1 = "test.txt";

	protected static final String FILE2 = "test2.txt";

	protected static final String FOLDER = "folder";

	public static File getTestDirectory() {
		return testDirectory;
	}
	
	@BeforeClass
	public static void beforeClassBase() throws Exception {
		deleteAllProjects();
		// create our temporary directory in the user space
		File userHome = FS.DETECTED.userHome();
		testDirectory = new File(userHome, "LocalRepositoriesTests");
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
		org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.DEFAULT_REPO_DIR, repoRoot.getPath());
		// suppress the configuration dialog
		org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore()
				.setValue(UIPreferences.SHOW_INITIAL_CONFIG_DIALOG, false);
		// suppress the detached head warning dialog
		org.eclipse.egit.ui.Activator
				.getDefault()
				.getPreferenceStore()
				.setValue(UIPreferences.SHOW_DETACHED_HEAD_WARNING,
						false);
	}

	@AfterClass
	public static void afterClassBase() throws Exception {
		// close all editors/dialogs
		new Eclipse().reset();
		// cleanup
		deleteAllProjects();
		shutDownRepositories();
		FileUtils.delete(testDirectory, FileUtils.RECURSIVE | FileUtils.RETRY);
		Activator.getDefault().getRepositoryCache().clear();
	}

	private static void shutDownRepositories() {
		RepositoryCache cache = Activator.getDefault().getRepositoryCache();
		for(Repository repository:cache.getAllRepositories())
			repository.close();
		cache.clear();
	}

	protected static void deleteAllProjects() throws Exception {
		for (IProject prj : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects())
			if (prj.getName().equals(PROJ1))
				prj.delete(false, false, null);
			else if (prj.getName().equals(PROJ2)) {
				// delete the .project on disk
				FileUtils.delete(EFS.getStore(
						prj.getFile(".project").getLocationURI()).toLocalFile(
						EFS.NONE, null), FileUtils.RETRY);
				prj.delete(false, false, null);
			}

	}

	public static File createProjectAndCommitToRepository() throws Exception {
		return createProjectAndCommitToRepository(REPO1);
	}

	protected static File createProjectAndCommitToRepository(String repoName)
			throws Exception {

		File gitDir = new File(new File(testDirectory, repoName),
				Constants.DOT_GIT);
		Repository myRepository = new FileRepository(gitDir);
		myRepository.create();

		// we need to commit into master first
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);

		if (firstProject.exists())
			firstProject.delete(true, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(PROJ1);
		desc.setLocation(new Path(new File(myRepository.getWorkTree(), PROJ1)
				.getPath()));
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

		IProject secondPoject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ2);

		if (secondPoject.exists())
			secondPoject.delete(true, null);

		desc = ResourcesPlugin.getWorkspace().newProjectDescription(PROJ2);
		desc.setLocation(new Path(new File(myRepository.getWorkTree(), PROJ2)
				.getPath()));
		secondPoject.create(desc, null);
		secondPoject.open(null);

		IFolder secondfolder = secondPoject.getFolder(FOLDER);
		secondfolder.create(false, true, null);
		IFile secondtextFile = secondfolder.getFile(FILE1);
		secondtextFile.create(new ByteArrayInputStream("Hello, world"
				.getBytes(firstProject.getDefaultCharset())), false, null);
		IFile secondtextFile2 = secondfolder.getFile(FILE2);
		secondtextFile2.create(new ByteArrayInputStream("Some more content"
				.getBytes(firstProject.getDefaultCharset())), false, null);
		// TODO we should be able to hide the .project
		// IFile gitignore = secondPoject.getFile(".gitignore");
		// gitignore.create(new ByteArrayInputStream("/.project\n"
		// .getBytes(firstProject.getDefaultCharset())), false, null);

		new ConnectProviderOperation(secondPoject, gitDir).execute(null);

		IFile[] commitables = new IFile[] { firstProject.getFile(".project"),
				textFile, textFile2, secondtextFile, secondtextFile2 };
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		// commit to stable
		CommitOperation op = new CommitOperation(commitables,
				new ArrayList<IFile>(), untracked, TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER, "Initial commit");
		op.execute(null);

		// now create a stable branch (from master)
		createStableBranch(myRepository);
		// and check in some stuff into master again
		touchAndSubmit(null);
		return gitDir;
	}

	protected static File createRemoteRepository(File repositoryDir)
			throws Exception {
		FileRepository myRepository = lookupRepository(repositoryDir);
		File gitDir = new File(testDirectory, REPO2);
		Repository myRemoteRepository = new FileRepository(gitDir);
		myRemoteRepository.create();
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
		RemoteConfig config = new RemoteConfig(myRepository.getConfig(), "push");
		PushOperationUI pa = new PushOperationUI(myRepository, config, 0, false);
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

	protected static File createChildRepository(File repositoryDir)
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
		RefUpdate updateRef = myRepository.updateRef(newRefName);
		Ref sourceBranch = myRepository.getRef("refs/heads/master");
		ObjectId startAt = sourceBranch.getObjectId();
		String startBranch = Repository.shortenRefName(sourceBranch.getName());
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
	}

	/**
	 *  This method should only be used in exceptional cases.
	 *  Try to avoid using it e.g. by joining execution jobs
	 *  instead of waiting a given amount of time {@link TestUtil#joinJobs(Object)}
	 * @throws InterruptedException
	 */
	protected static void waitInUI() throws InterruptedException {
		Thread.sleep(1000);
	}

	protected void shareProjects(File repositoryDir) throws Exception {
		Repository myRepository = lookupRepository(repositoryDir);
		FilenameFilter projectFilter = new FilenameFilter() {
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

					new ConnectProviderOperation(prj, myRepository
							.getDirectory()).execute(null);
				}
			}
		}
	}

	@SuppressWarnings("boxing")
	protected void assertProjectExistence(String projectName, boolean existence) {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				projectName);
		assertEquals("Project existence " + projectName, prj.exists(),
				existence);
	}

	protected static FileRepository lookupRepository(File directory)
			throws Exception {
		return (FileRepository) org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(directory);
	}

	/**
	 * @param commitMessage
	 *            may be null
	 * @throws Exception
	 */
	protected static void touchAndSubmit(String commitMessage) throws Exception {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				PROJ1);
		if (!prj.isAccessible())
			throw new IllegalStateException("No project to touch");
		IFile file = prj.getFile(new Path("folder/test.txt"));
		String newContent = "Touched at " + System.currentTimeMillis();
		file.setContents(new ByteArrayInputStream(newContent.getBytes(prj
				.getDefaultCharset())), 0, null);

		IFile[] commitables = new IFile[] { file };
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		String message = commitMessage;
		if (message == null)
			message = newContent;
		// TODO: remove after replacing GitIndex in CommitOperation
		waitInUI();
		CommitOperation op = new CommitOperation(commitables,
				new ArrayList<IFile>(), untracked, TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER, message);
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
				new ArrayList<IFile>(), untracked, TestUtil.TESTAUTHOR,
				TestUtil.TESTCOMMITTER, commitMessage);
		op.execute(null);
	}

	protected static void setTestFileContent(String newContent)
			throws Exception {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				PROJ1);
		if (!prj.isAccessible())
			throw new IllegalStateException("No project found");
		IFile file = prj.getFile(new Path("folder/test.txt"));
		file.setContents(new ByteArrayInputStream(newContent.getBytes(prj
				.getDefaultCharset())), 0, null);
	}

	protected String getTestFileContent() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt"));
		if (file.exists()) {
			byte[] bytes = IO.readFully(file.getLocation().toFile());
			return new String(bytes, file.getCharset());
		} else {
			return "";
		}
	}

	/**
	 * @param projectExplorerTree
	 * @param project
	 *            name of a project
	 * @return the project item pertaining to the project
	 */
	protected SWTBotTreeItem getProjectItem(SWTBotTree projectExplorerTree,
			String project) {
		for (SWTBotTreeItem item : projectExplorerTree.getAllItems()) {
			String itemText = item.getText();
			StringTokenizer tok = new StringTokenizer(itemText, " ");
			String name = tok.nextToken();
			// may be a dirty marker
			if (name.equals(">"))
				name = tok.nextToken();
			if (project.equals(name))
				return item;
		}
		return null;
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
		final Repository db = new FileRepository(new File("/tmp")); //$NON-NLS-1$
		int timeout = 20;
		ListRemoteOperation listRemoteOp = new ListRemoteOperation(db, uri,
				timeout);
		listRemoteOp.run(null);
		return listRemoteOp.getRemoteRefs();
	}
}

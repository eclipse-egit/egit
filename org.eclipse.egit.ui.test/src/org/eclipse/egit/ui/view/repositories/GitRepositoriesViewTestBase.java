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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.RepositoryUtil;
import org.eclipse.egit.ui.internal.push.PushConfiguredRemoteAction;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalBranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteBranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.RemotesNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.SymbolicRefsNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Collection of utility methods for Git Repositories View tests
 */
public abstract class GitRepositoriesViewTestBase {

	// static because it is used in @BeforeClass
	protected static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	protected static final RepositoriesViewLabelProvider labelProvider = new RepositoriesViewLabelProvider();

	// test utilities
	protected static final TestUtil myUtil = TestUtil.getInstance();

	// the "Git Repositories View" bot
	private SWTBotView viewbot;

	// the human-readable view name
	protected static String viewName;

	// the temporary directory
	protected static File testDirectory;

	// the human readable Git category
	private static String gitCategory;

	protected static final String REPO1 = "FirstRepository";

	private static final String REPO2 = "RemoteRepository";

	/** A general project containing FOLDER containing FILE1 and FILE2 */
	protected static final String PROJ1 = "GeneralProject";

	/** A folder obtained by checking in a project without .project */
	protected static final String PROJ2 = "ProjectWithoutDotProject";

	protected static final String FILE1 = "test.txt";

	protected static final String FILE2 = "test2.txt";

	protected static final String FOLDER = "folder";

	static {
		viewName = myUtil.getPluginLocalizedValue("GitRepositoriesView_name");
		gitCategory = myUtil.getPluginLocalizedValue("GitCategory_name");
	}

	@BeforeClass
	public static void beforeClassBase() throws Exception {
		// close the welcome page
		try {
			bot.viewByTitle("Welcome").close();
		} catch (WidgetNotFoundException e) {
			// somebody else probably closed it, lets not feel bad about it.
		}
		clearView();
		deleteAllProjects();
		// create our temporary directory in the user space
		File userHome = FS.DETECTED.userHome();
		testDirectory = new File(userHome, "GitRepositoriesTest");
		if (testDirectory.exists())
			deleteRecursive(testDirectory);
		testDirectory.mkdir();
	}

	@AfterClass
	public static void afterClassBase() throws Exception {
		// close all editors/dialogs
		new Eclipse().reset();
		// cleanup
		clearView();
		deleteAllProjects();
		deleteRecursive(testDirectory);
	}

	private static void deleteRecursive(File dirOrFile) {
		if (dirOrFile.isDirectory()) {
			for (File file : dirOrFile.listFiles()) {
				deleteRecursive(file);
			}
		}
		boolean deleted = dirOrFile.delete();
		if (!deleted) {
			dirOrFile.deleteOnExit();
		}
	}

	protected static void deleteAllProjects() throws CoreException {
		for (IProject prj : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects())
			if (prj.getName().equals(PROJ1))
				prj.delete(false, false, null);
			else if (prj.getName().equals(PROJ2)) {
				// delete the .project on disk
				EFS.getStore(prj.getFile(".project").getLocationURI())
						.toLocalFile(EFS.NONE, null).delete();
				prj.delete(false, false, null);
			}

	}

	/**
	 * remove all configured repositories from the view
	 */
	protected static void clearView() {
		new InstanceScope().getNode(Activator.getPluginId()).remove(
				RepositoryUtil.PREFS_DIRECTORIES);
	}

	protected static File createProjectAndCommitToRepository() throws Exception {

		File gitDir = new File(new File(testDirectory, REPO1),
				Constants.DOT_GIT);
		gitDir.mkdir();
		Repository myRepository = new Repository(gitDir);
		myRepository.create();

		// we need to commit into master first
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);

		if (firstProject.exists())
			firstProject.delete(true, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(PROJ1);
		desc.setLocation(new Path(new File(myRepository.getWorkDir(), PROJ1)
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
		desc.setLocation(new Path(new File(myRepository.getWorkDir(), PROJ2)
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

		new ConnectProviderOperation(secondPoject, gitDir).execute(null);

		IFile[] commitables = new IFile[] { firstProject.getFile(".project"),
				textFile, textFile2, secondtextFile, secondtextFile2 };
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		// commit to stable
		CommitOperation op = new CommitOperation(commitables,
				new ArrayList<IFile>(), untracked,
				"Test Author <test.author@test.com>",
				"Test Committer <test.commiter@test.com>", "Initial commit");
		op.execute(null);

		// now create a stable branch (from master)
		createStableBranch(myRepository);
		// and check in some stuff into master again
		touchAndSubmit();
		return gitDir;
	}

	protected static File createRemoteRepository(File repositoryDir)
			throws Exception {
		Repository myRepository = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(repositoryDir);
		File gitDir = new File(testDirectory, REPO2);
		Repository myRemoteRepository = new Repository(gitDir);
		myRemoteRepository.create();

		createStableBranch(myRepository);

		// now we configure the push destination
		myRepository.getConfig().setString("remote", "push", "pushurl",
				"file:///" + myRemoteRepository.getDirectory().getPath());
		myRepository.getConfig().setString("remote", "push", "push",
				"+refs/heads/*:refs/heads/*");
		// for some reason, this seems to be required
		myRepository.getConfig().setString(ConfigConstants.CONFIG_CORE_SECTION,
				null, ConfigConstants.CONFIG_KEY_REPO_FORMAT_VERSION, "0");

		myRepository.getConfig().save();
		// and push
		PushConfiguredRemoteAction pa = new PushConfiguredRemoteAction(
				myRepository, "push");

		pa.run(null, false);

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

	protected static void createStableBranch(Repository myRepository)
			throws IOException {
		// let's create a stable branch temporarily so
		// that we push two branches to remote
		String newRefName = "refs/heads/stable";
		RefUpdate updateRef = myRepository.updateRef(newRefName);
		Ref sourceBranch = myRepository.getRef("refs/heads/master");
		ObjectId startAt = sourceBranch.getObjectId();
		String startBranch = myRepository
				.shortenRefName(sourceBranch.getName());
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
	}

	@After
	public void afterBase() {
		new Eclipse().reset();
	}

	protected SWTBotView getOrOpenView() throws Exception {
		if (viewbot == null) {
			bot.menu("Window").menu("Show View").menu("Other...").click();
			SWTBotShell shell = bot.shell("Show View").activate();
			shell.bot().tree().expandNode(gitCategory).getNode(viewName)
					.select();
			shell.bot().button(IDialogConstants.OK_LABEL).click();

			viewbot = bot.viewByTitle(viewName);

			assertNotNull("Repositories View should not be null", viewbot);
		} else
			viewbot.setFocus();
		return viewbot;
	}

	protected void assertClickOpens(SWTBotTree tree, String menu, String window)
			throws InterruptedException {
		ContextMenuHelper.clickContextMenu(tree, menu);
		SWTBotShell shell = bot.shell(window);
		shell.activate();
		waitInUI();
		shell.bot().button(IDialogConstants.CANCEL_LABEL).click();
		shell.close();
	}

	protected void assertHasRepo(File repositoryDir) throws Exception {
		final SWTBotTree tree = getOrOpenView().bot().tree();
		final SWTBotTreeItem[] items = tree.getAllItems();
		boolean found = false;
		for (SWTBotTreeItem item : items) {
			if (item.getText().startsWith(
					repositoryDir.getParentFile().getName())) {
				found = true;
				break;
			}
		}
		assertTrue("Tree should have item with correct text", found);
	}

	protected void assertEmpty() throws Exception {
		final SWTBotView view = getOrOpenView();
		final SWTBotTreeItem[] items = view.bot().tree().getAllItems();
		assertTrue("Tree should have no items", items.length == 0);
	}

	protected void refreshAndWait() throws Exception {
		RepositoriesView view = (RepositoriesView) getOrOpenView()
				.getReference().getPart(false);
		Job refreshJob = view.refresh();
		refreshJob.join();
	}

	protected static void waitInUI() throws InterruptedException {
		Thread.sleep(1000);
	}

	protected void shareProjects(File repositoryDir) throws Exception {
		Repository myRepository = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(repositoryDir);
		FilenameFilter projectFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.equals(".project");
			}
		};
		for (File file : myRepository.getWorkDir().listFiles()) {
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

	protected static Repository lookupRepository(File directory)
			throws Exception {
		return org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(directory);
	}

	protected static void touchAndSubmit() throws Exception {
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
		CommitOperation op = new CommitOperation(commitables,
				new ArrayList<IFile>(), untracked,
				"Test Author <test.author@test.com>",
				"Test Committer <test.commiter@test.com>", newContent);
		op.execute(null);
	}

	protected SWTBotTreeItem getLocalBranchesItem(SWTBotTree tree, File repo)
			throws Exception {
		Repository repository = lookupRepository(repo);
		RepositoryNode root = new RepositoryNode(null, repository);
		BranchesNode branches = new BranchesNode(root, repository);
		LocalBranchesNode localBranches = new LocalBranchesNode(branches,
				repository);

		String rootText = labelProvider.getText(root);
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem branchesItem = rootItem.expand().getNode(
				labelProvider.getText(branches));
		SWTBotTreeItem localItem = branchesItem.expand().getNode(
				labelProvider.getText(localBranches));
		return localItem;
	}

	protected SWTBotTreeItem getRemoteBranchesItem(SWTBotTree tree,
			File repositoryFile) throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		BranchesNode branches = new BranchesNode(root, repository);
		RemoteBranchesNode remoteBranches = new RemoteBranchesNode(branches,
				repository);

		String rootText = labelProvider.getText(root);
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem branchesItem = rootItem.expand().getNode(
				labelProvider.getText(branches));
		SWTBotTreeItem remoteItem = branchesItem.expand().getNode(
				labelProvider.getText(remoteBranches));
		return remoteItem;
	}

	protected SWTBotTreeItem getWorkdirItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);

		WorkingDirNode workdir = new WorkingDirNode(root, repository);

		String rootText = labelProvider.getText(root);
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem workdirItem = rootItem.expand().getNode(
				labelProvider.getText(workdir));
		return workdirItem;
	}

	protected SWTBotTreeItem getRootItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		String rootText = labelProvider.getText(root);
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		return rootItem;
	}

	protected SWTBotTreeItem getSymbolicRefsItem(SWTBotTree tree,
			File repositoryFile) throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		SymbolicRefsNode symrefsnode = new SymbolicRefsNode(root, repository);
		SWTBotTreeItem rootItem = tree.getTreeItem(labelProvider.getText(root))
				.expand();
		SWTBotTreeItem symrefsitem = rootItem.getNode(labelProvider
				.getText(symrefsnode));
		return symrefsitem;
	}

	protected SWTBotTreeItem getRemotesItem(SWTBotTree tree, File repositoryFile)
			throws Exception {
		Repository repository = lookupRepository(repositoryFile);
		RepositoryNode root = new RepositoryNode(null, repository);
		RemotesNode remotes = new RemotesNode(root, repository);

		String rootText = labelProvider.getText(root);
		SWTBotTreeItem rootItem = tree.getTreeItem(rootText);
		SWTBotTreeItem remotesItem = rootItem.expand().getNode(
				labelProvider.getText(remotes));
		return remotesItem;
	}

	protected String getTestFileContent() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt"));
		if (file.exists()) {
			byte[] bytes = new byte[0];
			InputStream is = null;
			try {
				is = file.getContents();
				bytes = new byte[is.available()];
				is.read(bytes);
			} finally {
				if (is != null)
					is.close();
			}
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

	/**
	 * Activates the item by "pressing" ALT + the character after '&'
	 * @param shell
	 * @param itemWithShortcut
	 */
	protected void activateItemByKeyboard(SWTBotShell shell,
			String itemWithShortcut) {
		int index = itemWithShortcut.indexOf('&');
		if (index >= 0 && index < itemWithShortcut.length())
			pressAltAndChar(shell, itemWithShortcut.charAt(index + 1));
	}

}

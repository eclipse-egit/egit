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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;

/**
 * Collection of utility methods for Git Repositories View tests
 */
public abstract class GitRepositoriesViewTestBase extends
		LocalRepositoryTestCase {

	protected static final RepositoriesViewLabelProvider labelProvider = new RepositoriesViewLabelProvider();

	// test utilities
	protected static final TestUtil myUtil = new TestUtil();

	// the human-readable view name
	protected final static String viewName = myUtil
			.getPluginLocalizedValue("GitRepositoriesView_name");

	protected static final GitRepositoriesViewTestUtils myRepoViewUtil = new GitRepositoriesViewTestUtils();

	// the "Git Repositories View" bot
	private SWTBotView viewbot;

	/**
	 * remove all configured repositories from the view
	 */
	protected static void clearView() {
		new InstanceScope().getNode(Activator.getPluginId()).remove(
				RepositoryUtil.PREFS_DIRECTORIES);
	}

	protected static File createProjectAndCommitToRepository() throws Exception {

		File gitDir = new File(new File(getTestDirectory(), REPO1),
				Constants.DOT_GIT);
		Repository myRepository = lookupRepository(gitDir);
		myRepository.create();

		// TODO Bug: for some reason, this seems to be required
		myRepository.getConfig().setString(ConfigConstants.CONFIG_CORE_SECTION,
				null, ConfigConstants.CONFIG_KEY_REPO_FORMAT_VERSION, "0");

		myRepository.getConfig().save();

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
		touchAndSubmit(null);
		return gitDir;
	}

	protected static File createRemoteRepository(File repositoryDir)
			throws Exception {
		Repository myRepository = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().lookupRepository(repositoryDir);
		File gitDir = new File(getTestDirectory(), REPO2);
		Repository myRemoteRepository = lookupRepository(gitDir);
		myRemoteRepository.create();

		createStableBranch(myRepository);

		// now we configure the push destination
		myRepository.getConfig().setString("remote", "push", "pushurl",
				"file:///" + myRemoteRepository.getDirectory().getPath());
		myRepository.getConfig().setString("remote", "push", "push",
				"+refs/heads/*:refs/heads/*");
		// TODO Bug: for some reason, this seems to be required
		myRepository.getConfig().setString(ConfigConstants.CONFIG_CORE_SECTION,
				null, ConfigConstants.CONFIG_KEY_REPO_FORMAT_VERSION, "0");

		myRepository.getConfig().save();
		// and push
		PushOperationUI pa = new PushOperationUI(myRepository, "push", 0, false);
		pa.execute(null);
		TestUtil.joinJobs(JobFamilies.PUSH);
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
		String startBranch = Repository.shortenRefName(sourceBranch.getName());
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
			viewbot = myRepoViewUtil.openRepositoriesView(bot);
		} else
			viewbot.setFocus();
		return viewbot;
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
		view.refresh();
		TestUtil.joinJobs(JobFamilies.REPO_VIEW_REFRESH);
	}

	@SuppressWarnings("boxing")
	protected void assertProjectExistence(String projectName, boolean existence) {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(
				projectName);
		assertEquals("Project existence " + projectName, prj.exists(),
				existence);
	}
}

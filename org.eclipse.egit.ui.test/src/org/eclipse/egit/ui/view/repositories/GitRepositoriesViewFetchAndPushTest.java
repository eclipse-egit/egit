/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View (mainly fetch and push)
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewFetchAndPushTest extends
		GitRepositoriesViewTestBase {

	private File repositoryFile;

	private File remoteRepositoryFile;

	private File clonedRepositoryFile;

	private File clonedRepositoryFile2;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		remoteRepositoryFile = createRemoteRepository(repositoryFile);
		// now let's clone the remote repository
		URIish uri = new URIish("file:///" + remoteRepositoryFile.getPath());
		File workdir = new File(getTestDirectory(), "ClonedRepo");

		CloneOperation op = new CloneOperation(uri, true, null, workdir,
				"refs/heads/master", "origin", 0);
		op.run(null);

		clonedRepositoryFile = new File(workdir, Constants.DOT_GIT);

		// now let's clone the remote repository
		uri = new URIish(remoteRepositoryFile.getPath());
		workdir = new File(getTestDirectory(), "RepoClone2");

		op = new CloneOperation(uri, true, null, workdir, "refs/heads/master",
				"origin", 0);
		op.run(null);

		clonedRepositoryFile2 = new File(workdir, Constants.DOT_GIT);

		clearView();
		deleteAllProjects();
	}

	@Test
	public void testPushToOriginPushNode() throws Exception {
		testPushToOrigin(false);
	}

	@Test
	public void testPushToOriginRemoteNode() throws Exception {
		testPushToOrigin(true);
	}

	private void testPushToOrigin(boolean useRemote) throws Exception {
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				clonedRepositoryFile);
		shareProjects(clonedRepositoryFile);


		Repository repository = lookupRepository(clonedRepositoryFile);
		// add the configuration for push
		repository.getConfig().setString("remote", "origin", "push",
				"refs/heads/*:refs/remotes/origin/*");
		repository.getConfig().save();

		// make sure to have a "new" branch name so that the
		// dialog will return with a corresponding message
		SWTBotView repoView = getOrOpenView();
		String currentBranch = repository.getBranch();
		try (Git git = new Git(repository)) {
			git.branchRename().setOldName(currentBranch)
					.setNewName("" + System.currentTimeMillis()).call();
		}

		Job.getJobManager().join(JobFamilies.REPO_VIEW_REFRESH, null);
		TestUtil.waitForDecorations();
		SWTBotTree tree = repoView.bot().tree();
		tree.select(0);

		selectNode(tree, useRemote, false);

		runPush(tree);

		String destinationString = clonedRepositoryFile.getParentFile()
				.getName()
				+ " - " + "origin";
		String dialogTitle = NLS.bind(UIText.PushResultDialog_title,
				destinationString);

		// first time: expect new branch
		bot.waitUntil(Conditions.shellIsActive(dialogTitle));
		SWTBotShell confirmed = bot.shell(dialogTitle);
		SWTBotTreeItem[] treeItems = confirmed.bot().tree().getAllItems();
		boolean newBranch = false;
		for (SWTBotTreeItem item : treeItems) {
			newBranch = item.getText().contains(
					UIText.PushResultTable_statusOkNewBranch);
			if (newBranch)
				break;
		}
		confirmed.close();
		assertTrue("New branch expected", newBranch);
		// second time: expect up to date
		selectNode(tree, useRemote, false);

		runPush(tree);

		bot.waitUntil(Conditions.shellIsActive(dialogTitle));
		confirmed = bot.shell(dialogTitle);
		treeItems = confirmed.bot().tree().getAllItems();
		boolean uptodate = false;
		for (SWTBotTreeItem item : treeItems) {
			uptodate = item.getText().contains(
					UIText.PushResultTable_statusUpToDate);
			if (uptodate)
				break;
		}
		confirmed.close();
		assertTrue("Up to date expected", uptodate);
		// touch and run again: expect new branch
		String objectIdBefore = repository.exactRef(repository.getFullBranch())
				.getLeaf().getObjectId().name();
		objectIdBefore = objectIdBefore.substring(0, 7);
		touchAndSubmit(null);

		SWTBotTree updatedTree = getOrOpenView().bot().tree();
		updatedTree.select(0);
		selectNode(updatedTree, useRemote, false);

		runPush(updatedTree);

		bot.waitUntil(Conditions.shellIsActive(dialogTitle));
		confirmed = bot.shell(dialogTitle);
		treeItems = confirmed.bot().tree().getAllItems();
		newBranch = false;
		for (SWTBotTreeItem item : treeItems) {
			newBranch = item.getText().contains(objectIdBefore);
			if (newBranch)
				break;
		}
		confirmed.close();
		assertTrue("New branch expected", newBranch);
	}

	@Test
	public void testNoHeadSimplePushDisabled() throws Exception {
		Repository emptyRepo = createLocalTestRepository("empty");
		File gitDir = emptyRepo.getDirectory();
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(gitDir);
		GitRepositoriesViewTestUtils viewUtil = new GitRepositoriesViewTestUtils();
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem repoItem = viewUtil.getRootItem(tree, gitDir);
		repoItem.select();
		boolean enabled = ContextMenuHelper.isContextMenuItemEnabled(tree,
				NLS.bind(UIText.PushMenu_PushBranch, "master"));
		assertFalse("Push branch should be disabled if there is no HEAD",
				enabled);
		enabled = ContextMenuHelper.isContextMenuItemEnabled(tree,
				util.getPluginLocalizedValue("PushToUpstreamCommand.label"));
		assertFalse("Push to upstream should be disabled if there is no HEAD",
				enabled);
	}

	@Test
	public void testFetchFromOriginFetchNode() throws Exception {
		testFetchFromOrigin(false);
	}

	@Test
	public void testFetchFromOriginRemoteNode() throws Exception {
		testFetchFromOrigin(true);
	}

	private void testFetchFromOrigin(boolean useRemote) throws Exception {

		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				clonedRepositoryFile);
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				clonedRepositoryFile2);

		Repository repository = lookupRepository(clonedRepositoryFile2);
		// add the configuration for push from cloned2
		repository.getConfig().setString("remote", "origin", "push",
				"refs/heads/*:refs/heads/*");
		repository.getConfig().save();

		SWTBotTree tree = getOrOpenView().bot().tree();

		String destinationString = clonedRepositoryFile.getParentFile()
				.getName()
				+ " - " + "origin";
		String dialogTitle = NLS.bind(UIText.FetchResultDialog_title,
				destinationString);

		selectNode(tree, useRemote, true);
		runFetch(tree);

		bot.waitUntil(Conditions.shellIsActive(dialogTitle));
		SWTBotShell confirm = bot.shell(dialogTitle);
		assertEquals("Wrong result tree row count", 0, confirm.bot().tree()
				.rowCount());
		confirm.close();

		deleteAllProjects();
		shareProjects(clonedRepositoryFile2);
		String objid = repository.exactRef("refs/heads/master").getTarget()
				.getObjectId().name();
		objid = objid.substring(0, 7);
		touchAndSubmit(null);
		// push from other repository
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.PUSH, 60,
				TimeUnit.SECONDS);
		PushOperationUI op = new PushOperationUI(repository, "origin", false);
		op.start();
		TestUtil.openJobResultDialog(jobJoiner.join());

		String pushdialogTitle = NLS.bind(UIText.PushResultDialog_title,
				op.getDestinationString());

		bot.shell(pushdialogTitle).close();

		deleteAllProjects();

		refreshAndWait();

		selectNode(tree, useRemote, true);
		runFetch(tree);

		bot.waitUntil(Conditions.shellIsActive(dialogTitle));
		confirm = bot.shell(dialogTitle);
		SWTBotTreeItem[] treeItems = confirm.bot().tree().getAllItems();
		boolean found = false;
		for (SWTBotTreeItem item : treeItems) {
			found = item.getText().contains(objid);
			if (found)
				break;
		}
		assertTrue(found);
		confirm.close();

		selectNode(tree, useRemote, true);
		runFetch(tree);

		confirm = bot.shell(dialogTitle);
		assertEquals("Wrong result tree row count", 0, confirm.bot().tree()
				.rowCount());
	}

	private void selectNode(SWTBotTree tree, boolean useRemote, boolean fetchMode)
			throws Exception {
		SWTBotTreeItem remotesNode = myRepoViewUtil.getRemotesItem(tree,
				clonedRepositoryFile);
		SWTBotTreeItem originNode = TestUtil
				.getChildNode(TestUtil.expandAndWait(remotesNode), "origin");
		if (useRemote) {
			originNode.select();
		} else {
			TestUtil.expandAndWait(originNode).getNode(fetchMode ? 0 : 1)
					.select();
		}
	}

	private void runPush(SWTBotTree tree) {
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.PUSH, 60, TimeUnit.SECONDS);
		ContextMenuHelper.clickContextMenuSync(tree, myUtil
				.getPluginLocalizedValue("SimplePushCommand"));
		TestUtil.openJobResultDialog(jobJoiner.join());
	}

	private void runFetch(SWTBotTree tree) {
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.FETCH, 60, TimeUnit.SECONDS);
		ContextMenuHelper.clickContextMenuSync(tree, myUtil
				.getPluginLocalizedValue("SimpleFetchCommand"));
		TestUtil.openJobResultDialog(jobJoiner.join());
	}
}

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

import java.io.File;

import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View (mainly fetch and push)
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewFetchAndPushTest extends
		GitRepositoriesViewTestBase {

	private static File repositoryFile;

	private static File remoteRepositoryFile;

	private static File clonedRepositoryFile;

	private static File clonedRepositoryFile2;

	@BeforeClass
	public static void beforeClass() throws Exception {
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
		workdir = new File(getTestDirectory(), "ClonedRepo2");

		op = new CloneOperation(uri, true, null, workdir, "refs/heads/master",
				"origin", 0);
		op.run(null);

		clonedRepositoryFile2 = new File(workdir, Constants.DOT_GIT);
	}

	@Before
	public void before() throws Exception {
		clearView();
		deleteAllProjects();
	}

	@Test
	public void testPushToOrigin() throws Exception {
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				clonedRepositoryFile);
		shareProjects(clonedRepositoryFile);
		SWTBotTree tree = getOrOpenView().bot().tree();
		tree.select(0);

		Repository repository = lookupRepository(clonedRepositoryFile);
		// add the configuration for push
		repository.getConfig().setString("remote", "origin", "push",
				"refs/heads/*:refs/remotes/origin/*");
		repository.getConfig().save();

		myRepoViewUtil.getRemotesItem(tree, clonedRepositoryFile).expand().getNode(
				"origin").expand().getNode(1).select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("SimplePushCommand"));

		String destinationString = clonedRepositoryFile.getParentFile()
				.getName()
				+ " - " + "origin";
		String dialogTitle = NLS.bind(UIText.ResultDialog_title,
				destinationString);

		// first time: expect new branch
		TestUtil.joinJobs(JobFamilies.PUSH);
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
		myRepoViewUtil.getRemotesItem(tree, clonedRepositoryFile).expand().getNode(
				"origin").expand().getNode(1).select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("SimplePushCommand"));

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
		String objectIdBefore = repository.getRef("refs/heads/master")
				.getLeaf().getObjectId().name();
		objectIdBefore = objectIdBefore.substring(0, 7);
		touchAndSubmit(null);

		myRepoViewUtil.getRemotesItem(tree, clonedRepositoryFile).expand().getNode(
				"origin").expand().getNode(1).select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("SimplePushCommand"));

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
	public void testFetchFromOrigin() throws Exception {

		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				clonedRepositoryFile);
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				clonedRepositoryFile2);

		FileRepository repository = lookupRepository(clonedRepositoryFile2);
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

		myRepoViewUtil.getRemotesItem(tree, clonedRepositoryFile).expand().getNode(
				"origin").expand().getNode(0).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("SimpleFetchCommand"));

		SWTBotShell confirm = bot.shell(dialogTitle);
		assertEquals("Wrong result tree row count", 0, confirm.bot().tree()
				.rowCount());
		confirm.close();

		deleteAllProjects();
		shareProjects(clonedRepositoryFile2);
		String objid = repository.getRef("refs/heads/master").getTarget()
				.getObjectId().name();
		objid = objid.substring(0, 7);
		touchAndSubmit(null);
		// push from other repository
		PushOperationUI op =new PushOperationUI(repository, "origin", 0, false);
		op.start();

		String pushdialogTitle = NLS.bind(UIText.ResultDialog_title,
				op.getDestinationString());

		bot.shell(pushdialogTitle).close();

		deleteAllProjects();

		refreshAndWait();

		myRepoViewUtil.getRemotesItem(tree, clonedRepositoryFile).expand().getNode(
				"origin").expand().getNode(0).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("SimpleFetchCommand"));

		TestUtil.joinJobs(JobFamilies.FETCH);
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

		myRepoViewUtil.getRemotesItem(tree, clonedRepositoryFile).expand().getNode(
				"origin").expand().getNode(0).select();
		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("SimpleFetchCommand"));

		confirm = bot.shell(dialogTitle);
		assertEquals("Wrong result tree row count", 0, confirm.bot().tree()
				.rowCount());
	}
}

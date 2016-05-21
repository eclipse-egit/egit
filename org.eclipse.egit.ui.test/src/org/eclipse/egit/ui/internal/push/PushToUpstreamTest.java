/*******************************************************************************
 * Copyright (c) 2014 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for "Push to Upstream" action.
 */
public class PushToUpstreamTest extends LocalRepositoryTestCase {

	private Repository repository;
	private Repository remoteRepository;

	@Before
	public void createRepositories() throws Exception {
		File repositoryFile = createProjectAndCommitToRepository();
		File remoteRepositoryFile = createRemoteRepository(repositoryFile);
		repository = lookupRepository(repositoryFile);
		remoteRepository = lookupRepository(remoteRepositoryFile);
	}

	@Test
	public void pushWithoutConfig() throws Exception {
		checkoutNewLocalBranch("foo");
		assertPushToUpstreamDisabled();
	}

	@Test
	public void pushWithExistingUpstreamConfiguration() throws Exception {
		checkoutNewLocalBranch("bar");
		// Existing configuration
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_REMOTE, "fetch");
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/bar");

		pushToUpstream();
		assertBranchPushed("bar", remoteRepository);
	}

	@Test
	public void pushWithDefaultRemoteWithPushRefSpecs() throws Exception {
		checkoutNewLocalBranch("baz");
		repository.getConfig().setString("remote", "origin", "pushurl",
				repository.getConfig().getString("remote", "push", "pushurl"));
		repository.getConfig().setString("remote", "origin", "push",
				"refs/heads/*:refs/heads/*");

		pushToUpstream();
		assertBranchPushed("baz", remoteRepository);
	}

	private void checkoutNewLocalBranch(String branchName)
			throws Exception {
		CreateLocalBranchOperation createBranch = new CreateLocalBranchOperation(
				repository, branchName, repository.findRef("master"),
				UpstreamConfig.NONE);
		createBranch.execute(null);
		BranchOperation checkout = new BranchOperation(repository, branchName);
		checkout.execute(null);
	}

	private void assertBranchPushed(String branchName, Repository remoteRepo)
			throws Exception {
		ObjectId pushed = remoteRepo.resolve(branchName);
		assertNotNull("Expected '" + branchName
				+ "' to resolve to non-null ObjectId on remote repository",
				pushed);
		ObjectId local = repository.resolve(branchName);
		assertEquals(
				"Expected local branch to be the same as branch on remote after pushing",
				local, pushed);
	}

	private SWTBotTree selectProject() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		return projectExplorerTree;
	}

	private void pushToUpstream() {
		SWTBotTree project = selectProject();
		ContextMenuHelper
				.clickContextMenu(project, getPushToUpstreamMenuPath());

		SWTBotShell resultDialog = TestUtil
				.botForShellStartingWith("Push Results");
		resultDialog.close();
	}

	private void assertPushToUpstreamDisabled() {
		SWTBotTree project = selectProject();
		boolean enabled = ContextMenuHelper.isContextMenuItemEnabled(project,
				getPushToUpstreamMenuPath());
		assertFalse("Expected Push to Upstream to be disabled", enabled);
	}

	private String[] getPushToUpstreamMenuPath() {
		return new String[] { "Team",
				util.getPluginLocalizedValue("PushToUpstreamCommand.label") };
	}
}

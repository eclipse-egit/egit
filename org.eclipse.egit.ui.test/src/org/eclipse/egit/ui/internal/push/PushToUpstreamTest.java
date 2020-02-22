/*******************************************************************************
 * Copyright (c) 2014, 2016 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
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
		String remoteName = "fetch";
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/bar");

		pushToUpstream(remoteName);
		assertBranchPushed("bar", remoteRepository);
	}

	@Test
	public void pushWithDefaultRemoteWithPushRefSpecs() throws Exception {
		checkoutNewLocalBranch("baz");
		String remoteName = "origin";
		repository.getConfig().setString("remote", remoteName, "pushurl",
				repository.getConfig().getString("remote", "push", "pushurl"));
		repository.getConfig().setString("remote", remoteName, "push",
				"refs/heads/*:refs/heads/*");

		pushToUpstream(remoteName);
		assertBranchPushed("baz", remoteRepository);
	}

	private void checkoutNewLocalBranch(String branchName)
			throws Exception {
		CreateLocalBranchOperation createBranch = new CreateLocalBranchOperation(
				repository, branchName, repository.findRef("master"), null);
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

	private void pushToUpstream(String remoteName) {
		SWTBotTree project = selectProject();
		JobJoiner joiner = JobJoiner.startListening(JobFamilies.PUSH, 20,
				TimeUnit.SECONDS);
		ContextMenuHelper
				.clickContextMenu(project,
						getPushToUpstreamMenuPath(remoteName));
		TestUtil.openJobResultDialog(joiner.join());
		SWTBotShell resultDialog = TestUtil
				.botForShellStartingWith("Push Results");
		resultDialog.close();
	}

	private void assertPushToUpstreamDisabled() {
		SWTBotTree project = selectProject();
		boolean enabled = ContextMenuHelper.isContextMenuItemEnabled(project,
				getPushToUpstreamMenuPath("Upstream"));
		assertFalse("Expected Push to Upstream to be disabled", enabled);
	}

	private String[] getPushToUpstreamMenuPath(String remoteName) {
		return new String[] { "Team", "Push to " + remoteName };
	}
}

/*******************************************************************************
 * Copyright (c) 2014, 2022 Robin Stocker <robin@nibor.org> and others.
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
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the "Push to Upstream" action.
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
		// There is no "origin" config.
		assertPushToUpstreamDisabled();
	}

	@Test
	public void pushWithOriginConfig() throws Exception {
		checkoutNewLocalBranch("foo");
		// Existing configuration without push refspec
		String remoteName = "origin";
		repository.getConfig().setString("remote", remoteName, "url",
				repository.getConfig().getString("remote", "push", "pushurl"));
		repository.getConfig().setString("remote", remoteName, "fetch",
				"refs/heads/*:refs/remotes/origin/*");
		pushToUpstream("origin", "foo", true, false);
		assertBranchPushed("foo", remoteRepository);
	}

	@Test
	public void pushIsDisabledWithPushDefaultNothing() throws Exception {
		checkoutNewLocalBranch("foo");
		repository.getConfig().setString(ConfigConstants.CONFIG_PUSH_SECTION,
				null, ConfigConstants.CONFIG_KEY_DEFAULT, "nothing");
		String remoteName = "origin";
		repository.getConfig().setString("remote", remoteName, "url",
				repository.getConfig().getString("remote", "push", "pushurl"));
		repository.getConfig().setString("remote", remoteName, "fetch",
				"refs/heads/*:refs/remotes/origin/*");
		assertPushToUpstreamDisabled("origin");
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
	public void pushWithExistingUpstreamConfigurationDifferent()
			throws Exception {
		checkoutNewLocalBranch("bar");
		// Existing configuration
		String remoteName = "fetch";
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/bar2");

		pushToUpstream(remoteName, "bar", true, false);
		assertBranchPushed("bar", "bar2", remoteRepository);
	}

	@Test
	public void pushWithExistingUpstreamConfigurationPushDefaultUpstream()
			throws Exception {
		checkoutNewLocalBranch("bar");
		repository.getConfig().setString(ConfigConstants.CONFIG_PUSH_SECTION,
				null, ConfigConstants.CONFIG_KEY_DEFAULT, "upstream");
		// Existing configuration
		String remoteName = "fetch";
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/bar2");

		pushToUpstream(remoteName, "bar", false, false);
		assertBranchPushed("bar", "bar2", remoteRepository);
	}

	@Test
	public void pushWithExistingUpstreamConfigurationPushDefaultCurrent()
			throws Exception {
		checkoutNewLocalBranch("bar");
		repository.getConfig().setString(ConfigConstants.CONFIG_PUSH_SECTION,
				null, ConfigConstants.CONFIG_KEY_DEFAULT, "current");
		// Existing configuration
		String remoteName = "fetch";
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"bar", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/bar2");

		pushToUpstream(remoteName, "bar", false, false);
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

		pushToUpstream(remoteName, "baz", false, true);
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
		assertBranchPushed(branchName, branchName, remoteRepo);
	}

	private void assertBranchPushed(String localName, String remoteName,
			Repository remoteRepo) throws Exception {
		ObjectId pushed = remoteRepo.resolve(remoteName);
		assertNotNull("Expected '" + remoteName
				+ "' to resolve to non-null ObjectId on remote repository",
				pushed);
		ObjectId local = repository.resolve(localName);
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
		pushToUpstream(remoteName, "", false, false);
	}

	private void pushToUpstream(String remoteName, String branchName,
			boolean expectBranchWizard, boolean expectMultipleWarning) {
		SWTBotTree project = selectProject();
		JobJoiner joiner = null;
		if (!expectBranchWizard) {
			joiner = JobJoiner.startListening(JobFamilies.PUSH, 20,
					TimeUnit.SECONDS);
		}
		ContextMenuHelper.clickContextMenu(project,
				getPushToUpstreamMenuPath(remoteName));
		if (expectBranchWizard) {
			PushBranchWizardTester tester = PushBranchWizardTester
					.forBranchName(branchName);
			TestUtil.openJobResultDialog(tester.finish());
		} else if (expectMultipleWarning) {
			SWTBot dialog = bot.shell(UIText.PushOperationUI_PushMultipleTitle)
					.bot();
			dialog.button(UIText.PushOperationUI_PushMultipleOkLabel).click();
		}
		if (joiner != null) {
			TestUtil.openJobResultDialog(joiner.join());
		}
		SWTBotShell resultDialog = TestUtil
				.botForShellStartingWith("Push Results");
		resultDialog.close();
	}

	private void assertPushToUpstreamDisabled() {
		assertPushToUpstreamDisabled("Upstream");
	}

	private void assertPushToUpstreamDisabled(String remoteName) {
		SWTBotTree project = selectProject();
		boolean enabled = ContextMenuHelper.isContextMenuItemEnabled(project,
				getPushToUpstreamMenuPath(remoteName));
		assertFalse("Expected Push to Upstream to be disabled", enabled);
	}

	private String[] getPushToUpstreamMenuPath(String remoteName) {
		return new String[] { "Team", "Push to " + remoteName };
	}
}

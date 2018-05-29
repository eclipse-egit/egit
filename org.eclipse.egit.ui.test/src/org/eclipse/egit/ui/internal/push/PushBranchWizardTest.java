/*******************************************************************************
 * Copyright (c) 2013, 2016 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for "Push Branch..." wizard.
 */
public class PushBranchWizardTest extends LocalRepositoryTestCase {

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
	public void pushToExistingRemote() throws Exception {
		checkoutNewLocalBranch("foo");

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "foo");
		wizard.selectRemote("fetch");
		wizard.selectMerge();
		wizard.next();
		wizard.finish();

		assertBranchPushed("foo", remoteRepository);
		assertBranchConfig("foo", "fetch", "refs/heads/foo", "false");
	}

	@Test
	public void pushHeadToExistingRemote() throws Exception {
		try (Git git = new Git(repository)) {
			AnyObjectId head = repository.resolve(Constants.HEAD);
			git.checkout().setName(head.name()).call();
		}

		PushBranchWizardTester wizard = PushBranchWizardTester
				.startWizard(selectProject(), Constants.HEAD);
		wizard.selectRemote("fetch");
		wizard.enterBranchName("foo");
		wizard.next();
		wizard.finish();

		assertBranchPushed("foo", remoteRepository);
	}

	@Test
	public void pushToExistingRemoteAndSetRebase() throws Exception {
		checkoutNewLocalBranch("bar");

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "bar");
		wizard.selectRemote("fetch");
		wizard.selectRebase();
		assertFalse(wizard.isUpstreamConfigOverwriteWarningShown());
		wizard.next();
		wizard.finish();

		assertBranchPushed("bar", remoteRepository);
		assertBranchConfig("bar", "fetch", "refs/heads/bar", "true");
	}

	@Test
	public void pushToExistingRemoteWithoutConfiguringUpstream()
			throws Exception {
		checkoutNewLocalBranch("baz");

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "baz");
		wizard.selectRemote("fetch");
		wizard.deselectConfigureUpstream();
		wizard.next();
		wizard.finish();

		assertBranchPushed("baz", remoteRepository);
		assertBranchConfig("baz", null, null, null);
	}

	@Test
	public void pushToNewRemote() throws Exception {
		checkoutNewLocalBranch("qux");
		Repository newRemoteRepository = createRemoteRepository();

		URIish remoteUri = getUri(newRemoteRepository);
		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "qux");
		wizard.selectNewRemoteOnBranchPage("quxremote", remoteUri.toString());
		wizard.selectMerge();
		wizard.next();
		wizard.finish();

		assertRemoteConfig("quxremote", remoteUri);
		assertBranchPushed("qux", newRemoteRepository);
		assertBranchConfig("qux", "quxremote", "refs/heads/qux", "false");
	}

	@Test
	public void pushWhenNoRemoteExistsYet() throws Exception {
		removeExistingRemotes();
		checkoutNewLocalBranch("foo");
		Repository other = createRemoteRepository();
		URIish uri = getUri(other);

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "foo");
		wizard.enterRemoteOnInitialPage("origin", uri.toString());
		wizard.next();
		wizard.selectMerge();
		wizard.next();
		wizard.finish();

		assertRemoteConfig("origin", uri);
		assertBranchPushed("foo", other);
		assertBranchConfig("foo", "origin", "refs/heads/foo", "false");
	}

	@Test
	public void pushWithDifferentBranchName() throws Exception {
		checkoutNewLocalBranch("localname");

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "localname");
		wizard.selectRemote("fetch");
		wizard.selectMerge();
		wizard.enterBranchName("remotename");
		wizard.next();
		wizard.finish();

		ObjectId pushed = remoteRepository.resolve("remotename");
		assertNotNull(pushed);
		assertEquals(repository.resolve("localname"), pushed);

		assertBranchConfig("localname", "fetch", "refs/heads/remotename",
				"false");
	}

	@Test
	public void pushWithRemoteUpstreamConfiguration() throws Exception {
		checkoutNewLocalBranch("foo");
		// Existing configuration
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"foo", ConfigConstants.CONFIG_KEY_REMOTE, "fetch");
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"foo", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/foo-on-remote");
		repository.getConfig().setEnum(
				ConfigConstants.CONFIG_BRANCH_SECTION, "foo",
				ConfigConstants.CONFIG_KEY_REBASE, BranchRebaseMode.REBASE);
		// Make sure the repository does not have autosetuprebase set
		repository.getConfig().setBoolean(
				ConfigConstants.CONFIG_BRANCH_SECTION, null,
				ConfigConstants.CONFIG_KEY_AUTOSETUPREBASE, false);

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "foo");
		wizard.selectRemote("fetch");
		wizard.assertBranchName("foo-on-remote");
		wizard.assertRebaseSelected();
		assertFalse(wizard.isUpstreamConfigOverwriteWarningShown());
		wizard.selectMerge();
		assertTrue(wizard.isUpstreamConfigOverwriteWarningShown());
		wizard.deselectConfigureUpstream();
		assertFalse(wizard.isUpstreamConfigOverwriteWarningShown());
		wizard.next();
		wizard.finish();

		ObjectId remoteId = remoteRepository.resolve("foo-on-remote");
		ObjectId localId = repository.resolve("foo");
		assertEquals(localId, remoteId);

		// Still configured
		assertBranchConfig("foo", "fetch", "refs/heads/foo-on-remote", "true");
	}

	@Test
	public void pushWithLocalUpstreamConfiguration() throws Exception {
		checkoutNewLocalBranch("foo");
		// Existing configuration
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"foo", ConfigConstants.CONFIG_KEY_REMOTE, ".");
		repository.getConfig().setString(ConfigConstants.CONFIG_BRANCH_SECTION,
				"foo", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/master");

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "foo");
		wizard.selectRemote("fetch");
		wizard.assertBranchName("foo");
		wizard.assertMergeSelected();
		assertTrue(wizard.isUpstreamConfigOverwriteWarningShown());
		wizard.deselectConfigureUpstream();
		assertFalse(wizard.isUpstreamConfigOverwriteWarningShown());
		wizard.selectMerge();
		wizard.next();
		wizard.finish();

		ObjectId remoteId = remoteRepository.resolve("foo");
		ObjectId localId = repository.resolve("foo");
		assertEquals(localId, remoteId);

		// Newly configured
		assertBranchConfig("foo", "fetch", "refs/heads/foo", "false");
	}

	private void removeExistingRemotes() throws IOException {
		StoredConfig config = repository.getConfig();
		Set<String> remotes = config
				.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);
		for (String remoteName : remotes)
			config.unsetSection(ConfigConstants.CONFIG_REMOTE_SECTION,
					remoteName);
		config.save();
	}

	private void checkoutNewLocalBranch(String branchName)
			throws Exception {
		CreateLocalBranchOperation createBranch = new CreateLocalBranchOperation(
				repository, branchName, repository.findRef("master"), null);
		createBranch.execute(null);
		BranchOperation checkout = new BranchOperation(repository, branchName);
		checkout.execute(null);
	}

	private Repository createRemoteRepository() throws IOException {
		File gitDir = new File(getTestDirectory(), "pushbranchremote");
		Repository repo = FileRepositoryBuilder.create(gitDir);
		repo.create(true);
		assertTrue(repo.isBare());
		return repo;
	}

	private URIish getUri(Repository repo)
			throws MalformedURLException {
		return new URIish(repo.getDirectory().toURI().toURL());
	}

	private SWTBotTree selectProject() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		return projectExplorerTree;
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

	private void assertBranchConfig(String branchName, String remoteName,
			String mergeRef, String rebase) {
		StoredConfig config = repository.getConfig();
		assertEquals(remoteName, config.getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_REMOTE));
		assertEquals(mergeRef, config.getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_MERGE));
		assertEquals(rebase, config.getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
				ConfigConstants.CONFIG_KEY_REBASE));
	}

	private void assertRemoteConfig(String remoteName, URIish remoteUri) {
		StoredConfig config = repository.getConfig();
		assertEquals(remoteUri.toString(), config.getString(
				ConfigConstants.CONFIG_REMOTE_SECTION, remoteName,
				ConfigConstants.CONFIG_KEY_URL));
		assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*",
				config.getString(ConfigConstants.CONFIG_REMOTE_SECTION,
						remoteName, "fetch"));
	}
}

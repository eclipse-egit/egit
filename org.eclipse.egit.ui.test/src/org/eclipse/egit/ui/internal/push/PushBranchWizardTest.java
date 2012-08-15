/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for "Push Branch..." wizard.
 */
public class PushBranchWizardTest extends LocalRepositoryTestCase {

	private Repository repository;
	private Repository remoteRepository;

	private final List<Repository> reposToDelete = new ArrayList<Repository>();

	@Before
	public void createRepositories() throws Exception {
		File repositoryFile = createProjectAndCommitToRepository();
		File remoteRepositoryFile = createRemoteRepository(repositoryFile);
		repository = lookupRepository(repositoryFile);
		remoteRepository = lookupRepository(remoteRepositoryFile);
		reposToDelete.add(repository);
		reposToDelete.add(remoteRepository);
	}

	@After
	public void deleteRepositories() throws Exception {
		deleteAllProjects();
		shutDownRepositories();
		for (Repository r : reposToDelete) {
			if (r.isBare())
				FileUtils.delete(r.getDirectory(), FileUtils.RECURSIVE
						| FileUtils.RETRY);
			else
				FileUtils.delete(r.getWorkTree(), FileUtils.RECURSIVE
						| FileUtils.RETRY);
		}
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
		assertBranchConfig("foo", "fetch", "refs/heads/foo", null);
	}

	@Test
	public void pushToExistingRemoteAndSetRebase() throws Exception {
		checkoutNewLocalBranch("bar");

		PushBranchWizardTester wizard = PushBranchWizardTester.startWizard(
				selectProject(), "bar");
		wizard.selectRemote("fetch");
		wizard.selectRebase();
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
		assertBranchConfig("qux", "quxremote", "refs/heads/qux", null);
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
		assertBranchConfig("foo", "origin", "refs/heads/foo", null);
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

		assertBranchConfig("localname", "fetch", "refs/heads/remotename", null);
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
				repository, branchName, repository.getRef("master"),
				UpstreamConfig.NONE);
		createBranch.execute(null);
		BranchOperation checkout = new BranchOperation(repository, branchName);
		checkout.execute(null);
	}

	private Repository createRemoteRepository() throws IOException {
		File gitDir = new File(getTestDirectory(), "pushbranchremote");
		Repository repo = FileRepositoryBuilder.create(gitDir);
		repo.create();
		assertTrue(repo.isBare());
		reposToDelete.add(repo);
		return repo;
	}

	private URIish getUri(Repository repo)
			throws MalformedURLException {
		return new URIish(repo.getDirectory().toURI().toURL());
	}

	private SWTBotTree selectProject() {
		SWTBotTree projectExplorerTree = bot
				.viewById("org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		return projectExplorerTree;
	}

	private void assertBranchPushed(String branchName, Repository remoteRepo)
			throws Exception {
		ObjectId pushed = remoteRepo.resolve(branchName);
		assertNotNull(pushed);
		assertEquals(repository.resolve(branchName), pushed);
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

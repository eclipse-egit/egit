/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.httpauth;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.ui.common.EGitTestCase;
import org.eclipse.egit.ui.common.LoginDialogTester;
import org.eclipse.egit.ui.common.PushResultDialogTester;
import org.eclipse.egit.ui.common.PushWizardTester;
import org.eclipse.egit.ui.common.RefSpecPageTester;
import org.eclipse.egit.ui.common.RepoPropertiesPage;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.wizards.clone.SampleTestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PushTest extends EGitTestCase {

	private static final int NUMBER_RANDOM_COMMITS = 5;

	private SampleTestRepository remoteRepository;

	private Repository localRepository;

	private File file;

	private File localRepoPath;

	@Before
	public void setup() throws Exception {
		TestUtil.disableProxy();
		remoteRepository = new SampleTestRepository(NUMBER_RANDOM_COMMITS, true);
		localRepoPath = new File(ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toFile(), "test" + System.nanoTime());
		String branch = Constants.R_HEADS + SampleTestRepository.FIX;
		CloneOperation cloneOperation = new CloneOperation(new URIish(
				remoteRepository.getUri()), true, null, localRepoPath, branch,
				"origin", 30);
		cloneOperation
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
						"agitter", "letmein"));
		cloneOperation.run(new NullProgressMonitor());
		file = new File(localRepoPath, SampleTestRepository.A_txt_name);
		assertTrue(file.exists());
		localRepository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(new File(localRepoPath, ".git"));
		assertNotNull(localRepository);
	}

	@Test
	public void testPush() throws Exception {
		// change file
		TestUtil.appendFileContent(file, "additional content", true);
		// commit change
		try (Git git = new Git(localRepository)) {
			git.add().addFilepattern(SampleTestRepository.A_txt_name).call();
			git.commit().setMessage("Change").call();
		}
		configurePush();
		// push change
		PushWizardTester wizardTester = new PushWizardTester();
		RepoPropertiesPage repoPropertiesPage = wizardTester.openPushWizard(localRepository);
		repoPropertiesPage.setPushDestination("push");
		wizardTester.nextPage();
		// now login dialog appears
		LoginDialogTester loginDialogTester = new LoginDialogTester();
		loginDialogTester.login("agitter", "letmein");
		RefSpecPageTester refSpecPageTester = new RefSpecPageTester();
		refSpecPageTester.waitUntilPageIsReady(1);
		wizardTester.finish();
		loginDialogTester.login("agitter", "letmein");
		PushResultDialogTester pushResultDialogTester = new PushResultDialogTester();
		String expectedMessage = "Repository " + remoteRepository.getUri();
		pushResultDialogTester.assertResultMessage(expectedMessage);
		pushResultDialogTester.closeDialog();
	}

	private void configurePush() throws Exception {
		StoredConfig config = localRepository.getConfig();
		config.setString("remote", "push", "pushurl", remoteRepository.getUri());
		config.setString("remote", "push", "push", "+refs/heads/*:refs/heads/*");
		config.save();
	}

	@After
	public void tearDown() throws Exception {
		if (remoteRepository != null)
			remoteRepository.shutDown();
		Activator.getDefault().getRepositoryCache().clear();
		if (localRepository != null)
			localRepository.close();
		if (localRepoPath != null)
			FileUtils.delete(localRepoPath, FileUtils.RECURSIVE
					| FileUtils.RETRY);
	}

}

/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the submodule option of the checkout confirmation in the Git
 * Repositories view.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewCheckoutSubmodulesTest
		extends GitRepositoriesViewTestBase {

	private static final String BRANCH = "with-sub";

	private File repositoryFile;

	@Before
	public void before() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		RepositoryUtil.INSTANCE.addConfiguredRepository(repositoryFile);
		Repository repo = lookupRepository(repositoryFile);
		SubmoduleAddCommand command = new SubmoduleAddCommand(repo);
		command.setPath("sub");
		command.setURI(new URIish(repo.getDirectory().toURI().toString())
				.toString());
		command.call().close();
		try (Git git = new Git(repo)) {
			git.commit().setMessage("Add submodule").call();
			git.branchCreate().setName(BRANCH).call();
		}
		refreshAndWait();
	}

	@After
	public void resetPreferences() {
		Activator.getDefault().getPreferenceStore()
				.setToDefault(UIPreferences.CHECKOUT_UPDATE_SUBMODULES);
		Activator.getDefault().getPreferenceStore()
				.setToDefault(UIPreferences.SHOW_CHECKOUT_CONFIRMATION);
	}

	@Test
	public void confirmationOffersSubmoduleUpdate() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		SWTBotTreeItem node = myRepoViewUtil.getLocalBranchesItem(tree,
				repositoryFile);
		TestUtil.expandAndWait(node).getNode(BRANCH).doubleClick();
		SWTBotShell shell = bot
				.shell(UIText.RepositoriesView_CheckoutConfirmationTitle);
		shell.bot().checkBox(UIText.CheckoutDialog_UpdateSubmodules).select();
		shell.bot()
				.button(UIText.RepositoriesView_CheckoutConfirmationDefaultButtonLabel)
				.click();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		assertEquals(BRANCH, lookupRepository(repositoryFile).getBranch());
		assertTrue(Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.CHECKOUT_UPDATE_SUBMODULES));
	}
}

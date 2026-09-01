/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.submodule;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the "Also update submodules" option of the checkout dialog.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CheckoutUpdateSubmodulesTest extends LocalRepositoryTestCase {

	private static final String SUBMODULE_PATH = "sub";

	private static final String OLD_SUBMODULE_BRANCH = "old-sub";

	private static final String NEW_SUBMODULE_BRANCH = "new-sub";

	private static final String NEW_FILE = "added-later.txt";

	private File repositoryFile;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@After
	public void resetPreference() {
		Activator.getDefault().getPreferenceStore()
				.setToDefault(UIPreferences.CHECKOUT_UPDATE_SUBMODULES);
	}

	@Test
	public void checkboxAbsentWithoutSubmodules() throws Exception {
		SWTBotShell dialog = openCheckoutBranchDialog();
		try {
			assertTrue("Checkbox must not be shown without submodules",
					dialog.bot().getFinder()
							.findControls(allOf(widgetOfType(Button.class),
									withText(
											UIText.CheckoutDialog_UpdateSubmodules)))
							.isEmpty());
		} finally {
			dialog.close();
		}
	}

	@Test
	public void checkoutUpdatesSubmodulesWhenChecked() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		ObjectId newSubHead = repo.resolve(Constants.HEAD);
		Repository subRepo = addSubmodule(repo);
		ObjectId oldSubHead = commitOldSubmoduleBranch(repo, subRepo);
		assertEquals(oldSubHead, subRepo.resolve(Constants.HEAD));

		checkout(Constants.MASTER, true);
		assertEquals(Constants.MASTER, repo.getBranch());
		assertEquals("Submodule must follow the checked out branch",
				newSubHead, subRepo.resolve(Constants.HEAD));
		assertTrue(Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.CHECKOUT_UPDATE_SUBMODULES));

		checkout(OLD_SUBMODULE_BRANCH, false);
		assertEquals(OLD_SUBMODULE_BRANCH, repo.getBranch());
		assertEquals("Submodule must stay untouched when unchecked",
				newSubHead, subRepo.resolve(Constants.HEAD));
		assertFalse(Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.CHECKOUT_UPDATE_SUBMODULES));
	}

	@Test
	public void untrackedFileConflictOffersOverwrite() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		Repository subRepo = addSubmodule(repo);
		ObjectId oldSubHead = subRepo.resolve(Constants.HEAD);
		ObjectId newSubHead = commitNewSubmoduleBranch(repo, subRepo);
		File untracked = new File(subRepo.getWorkTree(), NEW_FILE);
		Files.write(untracked.toPath(),
				"local".getBytes(StandardCharsets.UTF_8));
		assertEquals(oldSubHead, subRepo.resolve(Constants.HEAD));

		JobJoiner joiner = startCheckout(NEW_SUBMODULE_BRANCH, true);
		SWTBotShell conflict = bot
				.shell(UIText.SubmoduleUpdateConflictDialog_Title);
		conflict.bot().button(UIText.SubmoduleUpdateConflictDialog_Overwrite)
				.click();
		joiner.join();

		assertEquals(NEW_SUBMODULE_BRANCH, repo.getBranch());
		assertEquals("Submodule must be updated after overwriting",
				newSubHead, subRepo.resolve(Constants.HEAD));
		assertEquals("committed",
				Files.readString(untracked.toPath()).trim());
	}

	private Repository addSubmodule(Repository repo) throws Exception {
		SubmoduleAddCommand command = new SubmoduleAddCommand(repo);
		command.setPath(SUBMODULE_PATH);
		command.setURI(new URIish(repo.getDirectory().toURI().toString())
				.toString());
		command.call().close();
		try (Git git = new Git(repo)) {
			git.commit().setMessage("Add submodule").call();
		}
		return SubmoduleWalk.getSubmoduleRepository(repo, SUBMODULE_PATH);
	}

	// Creates a branch whose gitlink points to the parent of the submodule's
	// current HEAD and leaves the submodule checked out at that older commit.
	private ObjectId commitOldSubmoduleBranch(Repository repo,
			Repository subRepo) throws Exception {
		RevCommit older;
		try (Git subGit = new Git(subRepo)) {
			older = subRepo.parseCommit(subRepo.resolve(Constants.HEAD))
					.getParent(0);
			subGit.checkout().setName(older.name()).call();
		}
		try (Git git = new Git(repo)) {
			git.checkout().setCreateBranch(true).setName(OLD_SUBMODULE_BRANCH)
					.call();
			git.add().addFilepattern(SUBMODULE_PATH).call();
			git.commit().setMessage("Move submodule back").call();
		}
		return older.getId();
	}

	private void checkout(String branch, boolean updateSubmodules)
			throws Exception {
		startCheckout(branch, updateSubmodules).join();
	}

	// Creates a branch whose gitlink points to a new submodule commit adding
	// NEW_FILE, then leaves both repositories at their previous state.
	private ObjectId commitNewSubmoduleBranch(Repository repo,
			Repository subRepo) throws Exception {
		ObjectId previous = subRepo.resolve(Constants.HEAD);
		RevCommit added;
		try (Git subGit = new Git(subRepo)) {
			Files.write(new File(subRepo.getWorkTree(), NEW_FILE).toPath(),
					"committed".getBytes(StandardCharsets.UTF_8));
			subGit.add().addFilepattern(NEW_FILE).call();
			added = subGit.commit().setMessage("Add file").call();
		}
		try (Git git = new Git(repo)) {
			git.checkout().setCreateBranch(true).setName(NEW_SUBMODULE_BRANCH)
					.call();
			git.add().addFilepattern(SUBMODULE_PATH).call();
			git.commit().setMessage("Move submodule forward").call();
			git.checkout().setName(Constants.MASTER).call();
		}
		try (Git subGit = new Git(subRepo)) {
			subGit.checkout().setName(previous.name()).call();
		}
		return added.getId();
	}

	private JobJoiner startCheckout(String branch, boolean updateSubmodules)
			throws Exception {
		SWTBotShell dialog = openCheckoutBranchDialog();
		TestUtil.navigateTo(dialog.bot().tree(), new String[] {
				UIText.RepositoriesViewLabelProvider_LocalNodetext, branch })
				.select();
		if (updateSubmodules) {
			dialog.bot().checkBox(UIText.CheckoutDialog_UpdateSubmodules)
					.select();
		} else {
			dialog.bot().checkBox(UIText.CheckoutDialog_UpdateSubmodules)
					.deselect();
		}
		JobJoiner joiner = JobJoiner.startListening(JobFamilies.CHECKOUT, 60,
				TimeUnit.SECONDS);
		dialog.bot().button(UIText.CheckoutDialog_OkCheckout).click();
		return joiner;
	}

	private SWTBotShell openCheckoutBranchDialog() {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("SwitchToMenu.label"),
				UIText.SwitchToMenu_OtherMenuLabel };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
		return bot.shell(UIText.BranchSelectionAndEditDialog_WindowTitle);
	}
}

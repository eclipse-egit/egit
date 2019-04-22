/*******************************************************************************
 * Copyright (c) 2012, 2019 SAP AG and others.
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
package org.eclipse.egit.ui.test.team.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Replace With actions
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class ReplaceActionsTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
	}

	@Test
	public void testReplaceWithPrevious() throws Exception {
		String initialContent = getTestFileContent();
		touchAndSubmit(null);
		assertThat(getTestFileContent(), not(initialContent));
		String menuLabel = util.getPluginLocalizedValue(
				"ReplaceWithPreviousVersionAction.label");
		JobJoiner jobJoiner = JobJoiner.startListening(
				JobFamilies.DISCARD_CHANGES, 30, TimeUnit.SECONDS);
		clickReplaceWith(menuLabel);
		jobJoiner.join();
		assertEquals(initialContent, getTestFileContent());
	}

	@Test
	public void testReplaceWithPreviousChanged() throws Exception {
		String initialContent = getTestFileContent();
		touchAndSubmit(null);
		String newContent = getTestFileContent();
		assertThat(newContent, not(initialContent));
		touch("Something else");
		String changedContent = getTestFileContent();
		assertThat(changedContent, not(initialContent));
		assertThat(changedContent, not(newContent));
		String menuLabel = util
				.getPluginLocalizedValue(
						"ReplaceWithPreviousVersionAction.label");
		clickReplaceWith(menuLabel);
		SWTBotShell confirm = bot
				.shell(UIText.DiscardChangesAction_confirmActionTitle);
		executeReplace(confirm,
				UIText.DiscardChangesAction_discardChangesButtonText);
		assertEquals(initialContent, getTestFileContent());
	}

	@Test
	public void testReplaceWithPreviousChangedClosed() throws Exception {
		String initialContent = getTestFileContent();
		touchAndSubmit(null);
		String newContent = getTestFileContent();
		assertThat(newContent, not(initialContent));
		touch("Something else");
		String changedContent = getTestFileContent();
		assertThat(changedContent, not(initialContent));
		assertThat(changedContent, not(newContent));
		String menuLabel = util.getPluginLocalizedValue(
				"ReplaceWithPreviousVersionAction.label");
		clickReplaceWith(menuLabel);
		SWTBotShell confirm = bot
				.shell(UIText.DiscardChangesAction_confirmActionTitle);
		confirm.close();
		TestUtil.processUIEvents();
		// Confirmation closed, nothing should have changed
		assertEquals(changedContent, getTestFileContent());
	}

	@Test
	public void testReplaceWithPreviousWithMerge() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		try (Git git = new Git(repo)) {

			Calendar cal = Calendar.getInstance();
			long time = cal.getTime().getTime();
			PersonIdent sideCommitter = new PersonIdent("Side Committer",
					"side@example.org", time, 0);
			// Make sure commit time stamps are different, otherwise the order
			// in the dialog is not stable
			time += 5000;
			PersonIdent masterCommitter = new PersonIdent("Master Committer",
					"master@example.org", time, 0);

			git.checkout().setCreateBranch(true).setName("side").call();
			touch(PROJ1, "folder/test.txt", "side");
			RevCommit sideCommit = git.commit().setAll(true)
					.setMessage("Side commit").setCommitter(sideCommitter)
					.call();

			git.checkout().setName("master").call();
			touch(PROJ1, "folder/test2.txt", "master");
			git.commit().setAll(true).setMessage("Master commit")
					.setCommitter(masterCommitter).call();

			git.merge().include(sideCommit).call();
		}
		TestUtil.waitForJobs(100, 5000);

		String contentAfterMerge = getTestFileContent();
		assertEquals("side", contentAfterMerge);

		String menuLabel = util
				.getPluginLocalizedValue(
						"ReplaceWithPreviousVersionAction.label");
		clickReplaceWith(menuLabel);
		SWTBotShell selectDialog = bot
				.shell(UIText.CommitSelectDialog_WindowTitle);
		assertEquals(2, selectDialog.bot().table().rowCount());
		selectDialog.close();
		TestUtil.processUIEvents();

		// we have closed, so nothing should have changed
		String contentAfterClose = getTestFileContent();
		assertEquals(contentAfterMerge, contentAfterClose);

		clickReplaceWith(menuLabel);
		selectDialog = bot.shell(UIText.CommitSelectDialog_WindowTitle);
		// Select first parent, which should be the master commit
		SWTBotTable table = selectDialog.bot().table();
		assertEquals("Master commit", table.cell(0, 1));
		table.select(0);
		executeReplace(selectDialog, IDialogConstants.OK_LABEL);
		TestUtil.waitForJobs(100, 5000);

		String replacedContent = getTestFileContent();
		assertThat(replacedContent, not(contentAfterMerge));
	}

	private void clickReplaceWith(String menuLabel) {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Replace With",
				menuLabel);
	}

	private void executeReplace(SWTBotShell dialog, String buttonLabel) {
		JobJoiner jobJoiner = JobJoiner.startListening(
				JobFamilies.DISCARD_CHANGES, 30, TimeUnit.SECONDS);
		dialog.bot()
				.button(buttonLabel)
				.click();
		jobJoiner.join();
	}
}

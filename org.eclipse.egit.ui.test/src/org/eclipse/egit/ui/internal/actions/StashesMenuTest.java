/*******************************************************************************
 * Copyright (c) 2014 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;

import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for Team > Stashes menu.
 */
public class StashesMenuTest extends LocalRepositoryTestCase {

	private static final String STASHES = util
			.getPluginLocalizedValue("StashesMenu.label");

	@Before
	public void createRepository() throws Exception {
		createProjectAndCommitToRepository();
	}

	@Test
	public void menuWithoutStashes() {
		SWTBotTree tree = selectProject();
		assertTrue(ContextMenuHelper.isContextMenuItemEnabled(tree, "Team",
				STASHES));
		assertFalse(ContextMenuHelper.isContextMenuItemEnabled(tree, "Team",
				STASHES, UIText.StashesMenu_NoStashedChangesText));
	}

	@Test
	public void stashAndApplyChanges() throws Exception {
		String originalContent = getTestFileContent();

		String modifiedContent = "changes to stash";
		touch(modifiedContent);
		assertEquals(modifiedContent, getTestFileContent());

		ContextMenuHelper.clickContextMenu(selectProject(), "Team", STASHES,
				UIText.StashesMenu_StashChangesActionText);

		SWTBotShell createDialog = bot
				.shell(UIText.StashCreateCommand_titleEnterCommitMessage);
		SWTBotText enterMessageText = createDialog.bot().text(0);
		String stashMessage = "stash message";
		enterMessageText.setText(stashMessage);
		createDialog.bot().button(UIText.StashCreateCommand_ButtonOK).click();

		TestUtils.waitForJobs(5000, JobFamilies.STASH);

		assertEquals(originalContent, getTestFileContent());

		ContextMenuHelper.clickContextMenu(selectProject(), "Team", STASHES,
				MessageFormat.format(UIText.StashesMenu_StashItemText,
						Integer.valueOf(0), stashMessage));

		SWTBotEditor stashEditor = bot.activeEditor();
		// Check if text with message is there
		stashEditor.bot().styledText(stashMessage);

		stashEditor.bot()
				.toolbarButtonWithTooltip(
						util.getPluginLocalizedValue("StashApplyCommand.label"))
				.click();

		TestUtils.waitForJobs(5000, JobFamilies.STASH);

		assertEquals(modifiedContent, getTestFileContent());
	}

	private SWTBotTree selectProject() {
		SWTBotTree tree = TestUtil.getExplorerTree();
		TestUtil.getNode(tree.getAllItems(), PROJ1).select();
		return tree;
	}
}

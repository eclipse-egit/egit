/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - tag API changes
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Tag action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class TagActionTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);

		TagBuilder tag = new TagBuilder();
		tag.setTag("SomeTag");
		tag.setTagger(RawParseUtils.parsePersonIdent(TestUtil.TESTAUTHOR));
		tag.setMessage("I'm just a little tag");
		tag.setObjectId(repo.resolve(repo.getFullBranch()), Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repo, tag, false);
		top.execute(null);
		touchAndSubmit(null);
	}

	@Test
	public void testTagDialogShowExistingTags() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		SWTBotTable table = tagDialog.bot().tableWithLabel(
				UIText.CreateTagDialog_existingTags);
		TestUtil.waitUntilTableHasRowWithText(tagDialog.bot(), table, "SomeTag", 10000);
	}

	@Test
	public void testCreateTag() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName).setText(
				"SomeTag");
		assertFalse("Ok should be disabled",
				tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton)
						.isEnabled());
		tagDialog.bot().button(UIText.CreateTagDialog_clearButton)
				.click();
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName).setText(
				"AnotherTag");
		assertFalse("Ok should be disabled",
				tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton)
						.isEnabled());
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("Here's the message text");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		waitInUI();
		assertNotNull(lookupRepository(repositoryFile)
				.exactRef(Constants.R_TAGS + "AnotherTag"));
	}

	@Test
	public void testCreateTagAndStartPush() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		SWTBotButton button = tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagAndStartPushButton);
		assertFalse("'Create Tag And Start Push' should be disabled",
				button.isEnabled());
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("tag-to-push");
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("Tag to push");
		button.click();

		SWTBotShell pushTagsWizard = bot
				.shell(UIText.PushTagsWizard_WindowTitle);
		pushTagsWizard.close();
	}

	private SWTBotShell openTagDialog() throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, PROJ1).select();

		String[] menuPath = new String[] {
				util.getPluginLocalizedValue("TeamMenu.label"),
				util.getPluginLocalizedValue("AdvancedMenu.label"),
				util.getPluginLocalizedValue("TagAction_label") };
		ContextMenuHelper.clickContextMenu(projectExplorerTree, menuPath);
		SWTBotShell dialog = bot.shell(UIText.CreateTagDialog_NewTag);
		return dialog;
	}

	@Test
	public void testChangeTagMessage() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		assertFalse("Ok should be disabled",
				tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton)
						.isEnabled());
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName).setText(
				"MessageChangeTag");
		assertFalse("Ok should be disabled",
				tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton)
						.isEnabled());
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("Here's the first message");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		waitInUI();
		assertNotNull(lookupRepository(repositoryFile)
				.exactRef(Constants.R_TAGS + "MessageChangeTag"));
		tagDialog = openTagDialog();
		tagDialog.bot().tableWithLabel(UIText.CreateTagDialog_existingTags)
				.getTableItem("MessageChangeTag").select();
		assertFalse("Ok should be disabled",
				tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton)
						.isEnabled());
		String oldText = tagDialog.bot().styledTextWithLabel(
				UIText.CreateTagDialog_tagMessage).getText();
		assertEquals("Wrong message text", "Here's the first message", oldText);
		tagDialog.bot().checkBox(UIText.CreateTagDialog_overwriteTag).click();
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("New message");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		tagDialog = openTagDialog();
		tagDialog.bot().tableWithLabel(UIText.CreateTagDialog_existingTags)
				.getTableItem("MessageChangeTag").select();
		String newText = tagDialog.bot().styledTextWithLabel(
				UIText.CreateTagDialog_tagMessage).getText();
		assertEquals("Wrong message text", "New message", newText);
		tagDialog.close();
	}

}

/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - tag API changes
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.internal.op.BranchOperation;
import org.eclipse.egit.core.internal.op.TagOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Tag action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class TagActionTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static SWTBotPerspective perspective;

	@BeforeClass
	public static void setup() throws Exception {
		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();

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
		waitInUI();
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	@Before
	public void prepare() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		if (!repo.getBranch().equals("master")) {
			BranchOperation bop = new BranchOperation(repo, "refs/heads/master");
			bop.execute(null);
		}
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
		assertFalse("Ok should be disabled", tagDialog.bot().button(
				IDialogConstants.OK_LABEL).isEnabled());
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName).setText(
				"AnotherTag");
		assertFalse("Ok should be disabled", tagDialog.bot().button(
				IDialogConstants.OK_LABEL).isEnabled());
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("Here's the message text");
		tagDialog.bot().button(IDialogConstants.OK_LABEL).click();
		waitInUI();
		assertTrue(lookupRepository(repositoryFile).getTags().keySet()
				.contains("AnotherTag"));
	}

	private SWTBotShell openTagDialog() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
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
		assertFalse("Ok should be disabled", tagDialog.bot().button(
				IDialogConstants.OK_LABEL).isEnabled());
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName).setText(
				"MessageChangeTag");
		assertFalse("Ok should be disabled", tagDialog.bot().button(
				IDialogConstants.OK_LABEL).isEnabled());
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("Here's the first message");
		tagDialog.bot().button(IDialogConstants.OK_LABEL).click();
		waitInUI();
		assertTrue(lookupRepository(repositoryFile).getTags().keySet()
				.contains("MessageChangeTag"));
		tagDialog = openTagDialog();
		tagDialog.bot().tableWithLabel(UIText.CreateTagDialog_existingTags)
				.getTableItem("MessageChangeTag").select();
		assertFalse("Ok should be disabled", tagDialog.bot().button(
				IDialogConstants.OK_LABEL).isEnabled());
		String oldText = tagDialog.bot().styledTextWithLabel(
				UIText.CreateTagDialog_tagMessage).getText();
		assertEquals("Wrong message text", "Here's the first message", oldText);
		tagDialog.bot().checkBox(UIText.CreateTagDialog_overwriteTag).click();
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("New message");
		tagDialog.bot().button(IDialogConstants.OK_LABEL).click();
		tagDialog = openTagDialog();
		tagDialog.bot().tableWithLabel(UIText.CreateTagDialog_existingTags)
				.getTableItem("MessageChangeTag").select();
		String newText = tagDialog.bot().styledTextWithLabel(
				UIText.CreateTagDialog_tagMessage).getText();
		assertEquals("Wrong message text", "New message", newText);
		tagDialog.close();
	}

}

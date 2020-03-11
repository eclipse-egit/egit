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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Tag action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class TagActionTest extends LocalRepositoryTestCase {
	private File repositoryFile;

	private ObjectId someTagCommit;

	private ObjectId someLightTagCommit;

	private ObjectId headCommit;

	@Before
	public void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);

		someTagCommit = repo.exactRef(Constants.HEAD).getObjectId();
		TagBuilder tag = new TagBuilder();
		tag.setTag("SomeTag");
		tag.setTagger(RawParseUtils.parsePersonIdent(TestUtil.TESTAUTHOR));
		tag.setMessage("I'm just a little tag");
		tag.setObjectId(someTagCommit, Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repo, tag, false, true);
		top.execute(null);

		touchAndSubmit(null);

		someLightTagCommit = repo.exactRef(Constants.HEAD).getObjectId();
		tag = new TagBuilder();
		tag.setTag("SomeLightTag");
		tag.setTagger(RawParseUtils.parsePersonIdent(TestUtil.TESTAUTHOR));
		tag.setObjectId(someLightTagCommit, Constants.OBJ_COMMIT);
		top = new TagOperation(repo, tag, false, false);
		top.execute(null);

		touchAndSubmit(null);
		headCommit = repo.exactRef(Constants.HEAD).getObjectId();
	}

	// This looks like overkill and part of it probably is, but the previous
	// simple select by label version sporadically failed (on Jenkins) and the
	// existing assertions made determining the reason for failure difficult.
	// So what is now done is:
	// * wait a bit, giving the UI thread time to populate the tree (potential
	// reason 1 - not all tags were present)
	// * clear the tag name field and clear the tree selection (no tree
	// filtering before the selection)
	// * check that there is more than one tag (not all tags present)
	// * (commented out) do not select by label, but go through the list (this
	// may actually be
	// overkill because currently the most probable cause is an incomplete tree
	// rather than a wrong selection)
	//
	// The purpose of the additional asserts is to make investigating a
	// potential fail reason easier
	private void selectTagInTree(SWTBotShell tagDialog, int numberOfRows,
			String expectedTag) {
		// setup - wait, clear any possible selection
		try {
			Thread.sleep(500);
			TestUtil.joinJobs(JobFamilies.FILL_TAG_LIST);
		} catch (InterruptedException e1) {
			// waiting for tag list to be filled
		}
		tagDialog.bot().tableWithLabel(UIText.CreateTagDialog_existingTags)
				.unselect();
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("");
		try {
			SWTBotTableItem item = tagDialog.bot()
					.tableWithLabel(UIText.CreateTagDialog_existingTags)
					.getTableItem(numberOfRows - 1);
			assertNotNull("Not all expected tags were present!", item);
		} catch (Exception e) {
			Assert.fail("Not all expected tags were present!");
		}
		// select by label
		tagDialog.bot().tableWithLabel(UIText.CreateTagDialog_existingTags)
				.getTableItem(expectedTag).select();
		// // select by expected tag name and check selected tag
		// for (int i = 0; i < numberOfRows; i++) {
		// try {
		// tagDialog.bot()
		// .tableWithLabel(UIText.CreateTagDialog_existingTags)
		// .unselect();
		// tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
		// .setText("");
		// SWTBotTableItem item = tagDialog.bot()
		// .tableWithLabel(UIText.CreateTagDialog_existingTags)
		// .getTableItem(i);
		// if (item.getText().equals(expectedTag)) {
		// item.select();
		// break;
		// }
		// } catch (Exception e) {
		// // ignore for now
		// }
		// }
		assertEquals("Did not find the expected tag in the list",
				expectedTag,
				tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
						.getText());
	}

	private void assertIsAnnotated(String tag, ObjectId target, String message)
			throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		Ref ref = repo.exactRef(Constants.R_TAGS + tag);
		ObjectId obj = ref.getObjectId();
		try (RevWalk walk = new RevWalk(repo)) {
			RevTag t = walk.parseTag(obj);
			if (message != null) {
				assertEquals("Unexpected tag message", message,
						t.getFullMessage());
			}
			assertEquals("Unexpected commit for tag " + t.getName(), target,
					walk.peel(t));
		}
	}

	private void assertIsLightweight(String tag, ObjectId target)
			throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		Ref ref = repo.exactRef(Constants.R_TAGS + tag);
		ObjectId obj = ref.getObjectId();
		assertEquals("Unexpected commit for tag " + ref.getName(), target, obj);
	}

	@Test
	public void testTagDialogShowExistingTags() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		SWTBotTable table = tagDialog.bot()
				.tableWithLabel(UIText.CreateTagDialog_existingTags);
		TestUtil.waitUntilTableHasRowWithText(tagDialog.bot(), table, "SomeTag",
				10000);
	}

	@Test
	public void testCreateTag() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("SomeTag");
		assertFalse("Ok should be disabled", tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagButton).isEnabled());
		tagDialog.bot().button(UIText.CreateTagDialog_clearButton).click();
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("AnotherTag");
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("Here's the message text");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		assertIsAnnotated("AnotherTag", headCommit, "Here's the message text");
	}

	@Test
	public void testCreateLightWeightTag() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("AnotherLightTag");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		assertIsLightweight("AnotherLightTag", headCommit);
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
		TestUtil.joinJobs(JobFamilies.FILL_TAG_LIST);
		// The job fires an asyncExec.
		dialog.widget.getDisplay().syncExec(() -> {
			// Make sure that asyncExec gets run
		});

		return dialog;
	}

	@Test
	public void testChangeTagMessage() throws Exception {
		SWTBotShell tagDialog = openTagDialog();
		assertFalse("Ok should be disabled", tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagButton).isEnabled());
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("MessageChangeTag");
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("Here's the first message");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		assertIsAnnotated("MessageChangeTag", headCommit,
				"Here's the first message");
		tagDialog = openTagDialog();
		selectTagInTree(tagDialog, 3, "MessageChangeTag");
		assertFalse("Ok should be disabled", tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagButton).isEnabled());
		String oldText = tagDialog.bot()
				.styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.getText();
		assertEquals("Wrong message text", "Here's the first message", oldText);
		tagDialog.bot().checkBox(UIText.CreateTagDialog_overwriteTag).click();
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("New message");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		tagDialog = openTagDialog();
		selectTagInTree(tagDialog, 3, "MessageChangeTag");
		String newText = tagDialog.bot()
				.styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.getText();
		assertEquals("Wrong message text", "New message", newText);
		tagDialog.close();
	}

	@Test
	public void testForceOverwriteLightWeightTag() throws Exception {
		assertIsLightweight("SomeLightTag", someLightTagCommit);
		SWTBotShell tagDialog = openTagDialog();
		selectTagInTree(tagDialog, 2, "SomeLightTag");
		assertFalse("Ok should be disabled", tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagButton).isEnabled());
		tagDialog.bot().checkBox(UIText.CreateTagDialog_overwriteTag).click();
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		assertIsLightweight("SomeLightTag", headCommit);
	}

	@Test
	public void testConvertLightWeightIntoAnnotatedTag() throws Exception {
		assertIsLightweight("SomeLightTag", someLightTagCommit);

		SWTBotShell tagDialog = openTagDialog();
		selectTagInTree(tagDialog, 2, "SomeLightTag");
		assertFalse("Ok should be disabled", tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagButton).isEnabled());
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("New message");
		tagDialog.bot().checkBox(UIText.CreateTagDialog_overwriteTag).click();
		assertTrue("Ok should be enabled", tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagButton).isEnabled());
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		assertIsAnnotated("SomeLightTag", headCommit, "New message");
	}

	@Test
	public void testConvertAnnotatedTagIntoLightWeight() throws Exception {
		assertIsAnnotated("SomeTag", someTagCommit, null);

		SWTBotShell tagDialog = openTagDialog();
		tagDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("SomeTag");
		// Selecting the second item via the table doesn't work on GTK,
		// focusing the table somehow selects the first element, which
		// sets the tag name text field, which filters the table to
		// show only that first element, after which the item.select()
		// below silently fails and we still have the first tag selected,
		// which is "SomeLightTag".
		//
		// SWTBotTableItem item = tagDialog.bot()
		// .tableWithLabel(UIText.CreateTagDialog_existingTags)
		// .getTableItem("SomeTag");
		// assertEquals("Wrong item selected", "SomeTag", item.getText());
		// item.select();
		// assertEquals("Tag name incorrect", "SomeTag", tagDialog.bot()
		// .textWithLabel(UIText.CreateTagDialog_tagName).getText());
		assertFalse("Ok should be disabled", tagDialog.bot()
				.button(UIText.CreateTagDialog_CreateTagButton).isEnabled());
		tagDialog.bot().checkBox(UIText.CreateTagDialog_overwriteTag).click();
		tagDialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("");
		tagDialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		assertIsLightweight("SomeTag", headCommit);
	}
}

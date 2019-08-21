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
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushTagsWizardTester;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Tests for the Git Repositories View tag handling support
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class GitRepositoriesViewTagHandlingTest extends
		GitRepositoriesViewTestBase {

	private File repositoryFile;

	private Repository repository;

	private RevWalk revWalk;

	@Before
	public void beforeClass() throws Exception {
		setVerboseBranchMode(false);
		repositoryFile = createProjectAndCommitToRepository();
		Activator.getDefault().getRepositoryUtil().addConfiguredRepository(
				repositoryFile);
		repository = lookupRepository(repositoryFile);
		revWalk = new RevWalk(repository);
	}

	@Test
	public void testCreateTagMultiple() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		int initialCount = myRepoViewUtil.getTagsItem(tree, repositoryFile)
				.expand().rowCount();

		String initialObjid = getObjectIdOfCommit();
		createTag("FirstTag", "The first tag");
		touchAndSubmit(null);
		String newObject = getObjectIdOfCommit();
		createTag("SecondTag", "The second tag");
		refreshAndWait();
		SWTBotTreeItem tagsItem = TestUtil.expandAndWait(
				myRepoViewUtil.getTagsItem(tree, repositoryFile));
		SWTBotTreeItem[] items = tagsItem.getItems();
		assertEquals("Wrong number of tags", initialCount + 2, items.length);

		assertTrue("Wrong commit id", initialObjid
				.equals(getCommitIdOfTag("FirstTag")));
		assertTrue("Wrong commit id", newObject
				.equals(getCommitIdOfTag("SecondTag")));
	}

	@Test
	public void testTagDeletionSingle() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		int initialCount = myRepoViewUtil.getTagsItem(tree, repositoryFile)
				.expand().rowCount();

		createTag("Delete1", "The first tag");
		refreshAndWait();
		SWTBotTreeItem tagsItem = TestUtil.expandAndWait(
				myRepoViewUtil.getTagsItem(tree, repositoryFile));
		SWTBotTreeItem[] items = tagsItem.getItems();
		assertEquals("Wrong number of tags", initialCount + 1, items.length);
		tagsItem.select("Delete1");
		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("DeleteTagCommand.name"));
		bot.shell(UIText.DeleteTagCommand_titleConfirm).bot()
				.button(IDialogConstants.OK_LABEL).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		refreshAndWait();
		tagsItem = myRepoViewUtil.getTagsItem(tree, repositoryFile).expand();
		items = tagsItem.getItems();
		assertEquals("Wrong number of tags", initialCount, items.length);
	}

	@Test
	public void testTagDeletionMultiple() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		int initialCount = myRepoViewUtil.getTagsItem(tree, repositoryFile)
				.expand().rowCount();

		createTag("Delete2", "The first tag");
		createTag("Delete3", "The second tag");
		refreshAndWait();
		SWTBotTreeItem tagsItem = TestUtil.expandAndWait(
				myRepoViewUtil.getTagsItem(tree, repositoryFile));
		SWTBotTreeItem[] items = tagsItem.getItems();
		assertEquals("Wrong number of tags", initialCount + 2, items.length);
		tagsItem.select("Delete2", "Delete3");
		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("DeleteTagCommand.name"));
		bot.shell(UIText.DeleteTagCommand_titleConfirm).bot()
				.button(IDialogConstants.OK_LABEL).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		refreshAndWait();
		tagsItem = myRepoViewUtil.getTagsItem(tree, repositoryFile).expand();
		items = tagsItem.getItems();
		assertEquals("Wrong number of tags", initialCount, items.length);
	}

	@Test
	public void testResetToTag() throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();

		String initialContent = getTestFileContent();
		createTag("ResetToFirst", "The first tag");
		touchAndSubmit(null);
		String newContent = getTestFileContent();
		assertFalse("Wrong content", initialContent.equals(newContent));
		createTag("ResetToSecond", "The second tag");
		refreshAndWait();
		TestUtil.expandAndWait(myRepoViewUtil.getTagsItem(tree, repositoryFile))
				.getNode("ResetToFirst").select();

		ContextMenuHelper.clickContextMenu(tree, myUtil
				.getPluginLocalizedValue("ResetCommand"));

		SWTBotShell resetDialog = bot.shell(UIText.ResetCommand_WizardTitle);
		resetDialog.bot().radio(
				UIText.ResetTargetSelectionDialog_ResetTypeHardButton).click();
		resetDialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		TestUtil.joinJobs(JobFamilies.RESET);

		bot.shell(UIText.ResetTargetSelectionDialog_ResetConfirmTitle).bot()
				.button(UIText.CommandConfirmationHardResetDialog_resetButtonLabel)
				.click();

		TestUtil.waitForJobs(50, 5000);
		Job.getJobManager().join(JobFamilies.RESET, null);

		ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
				IResource.DEPTH_INFINITE, null);
		assertEquals("Wrong content", initialContent, getTestFileContent());
	}

	@Test
	public void testPushTagPreselectsTag() throws Exception {
		createTag("tag-to-push", "Tag to push");
		SWTBotTree tree = getOrOpenView().bot().tree();
		TestUtil.expandAndWait(myRepoViewUtil.getTagsItem(tree, repositoryFile))
				.getNode("tag-to-push").select();
		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("RepoViewPushTag.label"));

		PushTagsWizardTester tester = PushTagsWizardTester.forShell();
		tester.assertTagChecked("tag-to-push");
		tester.cancel();
	}

	private String getCommitIdOfTag(String tagName) throws Exception {
		return revWalk.parseTag(repository.resolve(tagName)).getObject()
				.getId().name();
	}

	private void createTag(String name, String message) throws Exception {
		SWTBotTree tree = getOrOpenView().bot().tree();
		myRepoViewUtil.getTagsItem(tree, repositoryFile).select();
		ContextMenuHelper.clickContextMenu(tree,
				myUtil.getPluginLocalizedValue("RepoViewCreateTag.label"));
		String shellTitle = UIText.CreateTagDialog_NewTag;
		SWTBotShell createDialog = bot.shell(shellTitle).activate();
		TestUtil.joinJobs(JobFamilies.FILL_TAG_LIST);
		createDialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText(name);
		createDialog.bot()
				.styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText(message);
		createDialog.bot().button(UIText.CreateTagDialog_CreateTagButton)
				.click();
		TestUtil.joinJobs(JobFamilies.TAG);
	}

	private String getObjectIdOfCommit() throws Exception {
		String branch = repository.getFullBranch();
		if (ObjectId.isId(branch))
			return branch;
		if (branch.startsWith(Constants.R_REFS)) {
			RevCommit commit = revWalk.parseCommit(repository.resolve(branch));
			return commit.getId().getName();
		}
		if (branch.startsWith(Constants.R_TAGS)) {
			RevTag tag = revWalk.parseTag(repository.resolve(branch));
			return tag.getObject().getId().name();
		}
		throw new IllegalStateException("Can't resolve commit");
	}
}

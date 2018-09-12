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
package org.eclipse.egit.ui.test.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarToggleButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PlatformUI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class HistoryViewTest extends LocalRepositoryTestCase {
	private static final String SECONDFOLDER = "secondFolder";

	private static final String ADDEDFILE = "another.txt";

	private static final String ADDEDMESSAGE = "A new file in a new folder";

	private int commitCount;

	private File repoFile;

	@Before
	public void setup() throws Exception {
		repoFile = createProjectAndCommitToRepository();
		IProject prj = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		IFolder folder2 = prj.getFolder(SECONDFOLDER);
		folder2.create(false, true, null);
		IFile addedFile = folder2.getFile(ADDEDFILE);
		addedFile.create(
				new ByteArrayInputStream("More content".getBytes(prj
						.getDefaultCharset())), false, null);
		addAndCommit(addedFile, ADDEDMESSAGE);
		// TODO count the commits
		commitCount = 3;
	}

	@Test
	public void testOpenHistoryOnFileNoFilter() throws Exception {
		initFilter(0);

		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1).rowCount());
		assertEquals("Wrong number of commits", commitCount - 1,
				getHistoryViewTable(PROJ1, FOLDER).rowCount());
		assertEquals("Wrong number of commits", commitCount - 1,
				getHistoryViewTable(PROJ1, FOLDER, FILE1).rowCount());
		assertEquals("Wrong number of commits", 1,
				getHistoryViewTable(PROJ1, FOLDER, FILE2).rowCount());
		assertEquals("Wrong number of commits", 1,
				getHistoryViewTable(PROJ1, SECONDFOLDER).rowCount());
		assertEquals("Wrong number of commits", 1,
				getHistoryViewTable(PROJ1, SECONDFOLDER, ADDEDFILE).rowCount());
		assertEquals("Wrong number of commits", 1, getHistoryViewTable(PROJ2)
				.rowCount());

		assertEquals("Wrong commit message", ADDEDMESSAGE,
				getHistoryViewTable(PROJ1, SECONDFOLDER, ADDEDFILE)
						.getTableItem(0).getText(1));
		assertEquals("Wrong commit message", "Initial commit",
				getHistoryViewTable(PROJ1, FOLDER, FILE2).getTableItem(0)
						.getText(1));
	}

	@Test
	public void testOpenHistoryOnFileRepoFilter() throws Exception {
		initFilter(1);
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, FOLDER).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, FOLDER, FILE1).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, FOLDER, FILE2).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, SECONDFOLDER).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, SECONDFOLDER, ADDEDFILE).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ2).rowCount());
	}

	@Test
	public void testOpenHistoryOnFileProjectFilter() throws Exception {
		initFilter(2);
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, FOLDER).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, FOLDER, FILE1).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, FOLDER, FILE2).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, SECONDFOLDER).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, SECONDFOLDER, ADDEDFILE).rowCount());
		assertEquals("Wrong number of commits", 1, getHistoryViewTable(PROJ2)
				.rowCount());
	}

	@Test
	public void testOpenHistoryOnFileFolderFilter() throws Exception {
		initFilter(3);
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, FOLDER).rowCount());
		assertEquals("Wrong number of commits", commitCount - 1,
				getHistoryViewTable(PROJ1, FOLDER, FILE1).rowCount());
		assertEquals("Wrong number of commits", commitCount - 1,
				getHistoryViewTable(PROJ1, FOLDER, FILE2).rowCount());
		assertEquals("Wrong number of commits", commitCount,
				getHistoryViewTable(PROJ1, SECONDFOLDER).rowCount());
		assertEquals("Wrong number of commits", 1,
				getHistoryViewTable(PROJ1, SECONDFOLDER, ADDEDFILE).rowCount());
		assertEquals("Wrong number of commits", 1,
				getHistoryViewTable(PROJ2).rowCount());
	}

	/**
	 * @param filter
	 *            0: none, 1: repository, 2: project, 3: folder
	 * @throws Exception
	 */
	private void initFilter(int filter) throws Exception {
		getHistoryViewTable(PROJ1);
		SWTBotView view = bot
				.viewById(IHistoryView.VIEW_ID);
		SWTBotToolbarToggleButton folder = (SWTBotToolbarToggleButton) view
				.toolbarButton(UIText.GitHistoryPage_AllInParentTooltip);
		SWTBotToolbarToggleButton project = (SWTBotToolbarToggleButton) view
				.toolbarButton(UIText.GitHistoryPage_AllInProjectTooltip);
		SWTBotToolbarToggleButton repo = (SWTBotToolbarToggleButton) view
				.toolbarButton(UIText.GitHistoryPage_AllInRepoTooltip);
		switch (filter) {
		case 0:
			if (folder.isChecked())
				folder.click();
			if (project.isChecked())
				project.click();
			if (repo.isChecked())
				repo.click();
			break;
		case 1:
			if (!repo.isChecked())
				repo.click();
			break;
		case 2:
			if (!project.isChecked())
				project.click();
			break;
		case 3:
			if (!folder.isChecked())
				folder.click();
			break;
		default:
			break;
		}
	}

	@Test
	public void testOpenHistoryOnProject() throws Exception {
		SWTBotTable table = getHistoryViewTable(PROJ1);
		int rowCount = table.rowCount();
		assertTrue(table.rowCount() > 0);
		assertEquals(table.getTableItem(rowCount - 1).getText(1),
				"Initial commit");
	}

	@Test
	public void testAddCommit() throws Exception {
		String commitMessage = "The special commit";
		int countBefore = getHistoryViewTable(PROJ1).rowCount();
		touchAndSubmit(commitMessage);
		int countAfter = getHistoryViewTable(PROJ1).rowCount();
		assertEquals("Wrong number of entries", countBefore + 1, countAfter);
		assertEquals("Wrong comit message", commitMessage,
				getHistoryViewTable(PROJ1).getTableItem(0).getText(1));
	}

	/**
	 * @param path
	 *            must be length 2 or three (folder or file)
	 * @return the bale
	 * @throws Exception
	 */
	private SWTBotTable getHistoryViewTable(String... path) throws Exception {
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		SWTBotTreeItem explorerItem;
		SWTBotTreeItem projectItem = getProjectItem(projectExplorerTree, path[0]);
		if (path.length == 1)
			explorerItem = projectItem;
		else if (path.length == 2)
			explorerItem = TestUtil
					.getChildNode(TestUtil.expandAndWait(projectItem), path[1]);
		else {
			SWTBotTreeItem childItem = TestUtil
					.getChildNode(TestUtil.expandAndWait(projectItem), path[1]);
			explorerItem = TestUtil
					.getChildNode(TestUtil.expandAndWait(childItem), path[2]);
		}
		explorerItem.select();
		ContextMenuHelper.clickContextMenuSync(projectExplorerTree, "Team",
				"Show in History");
		// join GenerateHistoryJob
		Job.getJobManager().join(JobFamilies.GENERATE_HISTORY, null);
		// join UI update triggered by GenerateHistoryJob
		projectExplorerTree.widget.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				// empty
			}
		});

		return getHistoryViewBot().table();
	}

	private SWTBotTable getFileDiffTable() throws Exception {
		Job.getJobManager().join(JobFamilies.HISTORY_DIFF, null);
		// Wait a little bit to give the UiJob triggered a chance to run
		Thread.sleep(100);
		// Then join the UI update
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			/* empty */ });
		return getHistoryViewBot().table(1);
	}

	private SWTBot getHistoryViewBot() {
		return TestUtil.showHistoryView().bot();
	}

	@Test
	public void testAddBranch() throws Exception {
		Repository repo = lookupRepository(repoFile);
		assertNull(repo.resolve(Constants.R_HEADS + "NewBranch"));
		SWTBotTable table = getHistoryViewTable(PROJ1);
		SWTBotTableItem item = table.getTableItem(0);
		item.select();
		ContextMenuHelper.clickContextMenu(table,
				util.getPluginLocalizedValue("HistoryCreateBranch.label"));
		SWTBotShell dialog = bot
				.shell(UIText.CreateBranchWizard_NewBranchTitle);
		dialog.bot().textWithId("BranchName").setText("NewBranch");
		// for some reason, checkboxwithlabel doesn't seem to work
		dialog.bot().checkBox().deselect();
		dialog.bot().button(IDialogConstants.FINISH_LABEL).click();
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		assertNotNull(repo.resolve(Constants.R_HEADS + "NewBranch"));
	}

	@Test
	public void testAddTag() throws Exception {
		Repository repo = lookupRepository(repoFile);
		assertNull(repo.resolve(Constants.R_TAGS + "NewTag"));
		final SWTBotTable table = getHistoryViewTable(PROJ1);
		table.getTableItem(0).select();
		final RevCommit[] commit = new RevCommit[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				TableItem tableItem = table.widget.getSelection()[0];
				ensureTableItemLoaded(tableItem);
				commit[0] = (RevCommit) tableItem.getData();
			}
		});

		ContextMenuHelper.clickContextMenu(table,
				util.getPluginLocalizedValue("HistoryCreateTag.label"));
		SWTBotShell dialog = bot.shell(UIText.CreateTagDialog_NewTag);
		dialog.bot().textWithLabel(UIText.CreateTagDialog_tagName)
				.setText("NewTag");
		dialog.bot().styledTextWithLabel(UIText.CreateTagDialog_tagMessage)
				.setText("New Tag message");
		dialog.bot().button(UIText.CreateTagDialog_CreateTagButton).click();
		TestUtil.joinJobs(JobFamilies.TAG);
		assertNotNull(repo.resolve(Constants.R_TAGS + "NewTag"));
	}

	@Test
	public void testCheckOut() throws Exception {
		Repository repo = lookupRepository(repoFile);
		assertEquals(Constants.MASTER, repo.getBranch());

		final SWTBotTable table = getHistoryViewTable(PROJ1);
		// check out the second line
		final RevCommit[] commit = checkoutLine(table, 1);
		assertEquals(commit[0].getId().name(), repo.getBranch());
	}

	@Test
	public void testShowAllBranches() throws Exception {
		toggleShowAllBranchesButton(true);
		final SWTBotTable table = getHistoryViewTable(PROJ1);
		int commits = getHistoryViewTable(PROJ1).rowCount();
		checkoutLine(table, 1);

		toggleShowAllBranchesButton(false);
		assertEquals("Wrong number of commits", commits - 1,
				getHistoryViewTable(PROJ1).rowCount());
		toggleShowAllBranchesButton(true);
		assertEquals("Wrong number of commits", commits,
				getHistoryViewTable(PROJ1).rowCount());
	}

	@Test
	public void testRevertFailure() throws Exception {
		touchAndSubmit(null);
		setTestFileContent("dirty in working directory"
				+ System.currentTimeMillis());
		final SWTBotTable table = getHistoryViewTable(PROJ1);
		assertTrue(table.rowCount() > 0);
		table.getTableItem(0).select();
		final RevCommit[] commit = new RevCommit[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				TableItem tableItem = table.widget.getSelection()[0];
				ensureTableItemLoaded(tableItem);
				commit[0] = (RevCommit) tableItem.getData();
			}
		});
		assertEquals(1, commit[0].getParentCount());

		ContextMenuHelper.clickContextMenu(table,
				UIText.GitHistoryPage_revertMenuItem);
		SWTBot dialog = bot.shell(UIText.RevertFailureDialog_Title).bot();
		assertEquals(1, dialog.tree().rowCount());
		assertEquals(1, dialog.tree().getAllItems()[0].rowCount());
		assertTrue(dialog.tree().getAllItems()[0].getItems()[0].getText()
				.startsWith(FILE1));
	}

	@Test
	public void testOpenOfDeletedFile() throws Exception {
		Git git = Git.wrap(lookupRepository(repoFile));
		git.rm().addFilepattern(FILE1_PATH).call();
		RevCommit commit = git.commit().setMessage("Delete file").call();

		SWTBotTable commitsTable = getHistoryViewTable(PROJ1);
		assertEquals(commitCount + 1, commitsTable.rowCount());
		commitsTable.select(0);

		SWTBotTable fileDiffTable = getFileDiffTable();
		assertEquals(1, fileDiffTable.rowCount());

		fileDiffTable.select(0);
		assertFalse(fileDiffTable.contextMenu(
				UIText.CommitFileDiffViewer_OpenInEditorMenuLabel).isEnabled());
		fileDiffTable.contextMenu(
				UIText.CommitFileDiffViewer_OpenPreviousInEditorMenuLabel)
				.click();

		// Editor for old file version should be opened
		bot.editorByTitle(FILE1 + " " + commit.getParent(0).getName());
	}

	@Test
	@Ignore
	public void testRebaseAlreadyUpToDate() throws Exception {
		Repository repo = lookupRepository(repoFile);
		Ref stable = repo.findRef("stable");
		SWTBotTable table = getHistoryViewTable(PROJ1);
		SWTBotTableItem stableItem = getTableItemWithId(table, stable.getObjectId());

		stableItem.contextMenu(UIText.GitHistoryPage_rebaseMenuItem).click();
		TestUtil.joinJobs(JobFamilies.REBASE);
	}

	private RevCommit[] checkoutLine(final SWTBotTable table, int line)
			throws InterruptedException {
		table.getTableItem(line).select();
		final RevCommit[] commit = new RevCommit[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				TableItem tableItem = table.widget.getSelection()[0];
				ensureTableItemLoaded(tableItem);
				commit[0] = (RevCommit) tableItem.getData();
			}
		});

		ContextMenuHelper.clickContextMenuSync(table,
				UIText.GitHistoryPage_CheckoutMenuLabel);
		TestUtil.joinJobs(JobFamilies.CHECKOUT);
		return commit;
	}

	/**
	 * Workaround to ensure that the TableItem of a SWT table with style
	 * SWT_VIRTUAL is loaded.
	 *
	 * @param item
	 */
	private static void ensureTableItemLoaded(TableItem item) {
		item.setText(item.getText()); // TODO: is there a better solution?
	}

	private void toggleShowAllBranchesButton(boolean checked) throws Exception{
		getHistoryViewTable(PROJ1);
		SWTBotView view = bot
				.viewById(IHistoryView.VIEW_ID);
		SWTBotToolbarToggleButton showAllBranches = (SWTBotToolbarToggleButton) view
				.toolbarButton(UIText.GitHistoryPage_showAllBranches);
		boolean isChecked = showAllBranches.isChecked();
		if(isChecked && !checked || !isChecked && checked)
			showAllBranches.click();
	}

	private static SWTBotTableItem getTableItemWithId(SWTBotTable table,
			ObjectId wantedId) {
		for (int i = 0; i < table.rowCount(); i++) {
			String id = table.cell(i, UIText.CommitGraphTable_CommitId);
			String idWithoutEllipsis = id.substring(0, 7);
			if (wantedId.getName().startsWith(idWithoutEllipsis))
				return table.getTableItem(i);
		}

		throw new IllegalStateException("TableItem for commit with ID " + wantedId + " not found.");
	}
}

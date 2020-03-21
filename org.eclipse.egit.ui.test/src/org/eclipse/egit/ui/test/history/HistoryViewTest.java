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

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withRegex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.gitflow.op.InitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.RefFilterHelper;
import org.eclipse.egit.ui.internal.history.RefFilterHelper.RefFilter;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestBase;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.PlatformUI;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class HistoryViewTest extends GitRepositoriesViewTestBase {
	private static final String SECONDFOLDER = "secondFolder";

	private static final String ADDEDFILE = "another.txt";

	private static final String ADDEDMESSAGE = "A new file in a new folder";

	private int commitCount;

	private File repoFile;

	private RefFilterHelper refFilterHelper;

	@Before
	public void setupTests() throws Exception {
		repoFile = createProjectAndCommitToRepository();
		IProject prj = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		IFolder folder2 = prj.getFolder(SECONDFOLDER);
		folder2.create(false, true, null);
		IFile addedFile = folder2.getFile(ADDEDFILE);
		addedFile.create(
				new ByteArrayInputStream(
						"More content".getBytes(prj.getDefaultCharset())),
				false, null);
		addAndCommit(addedFile, ADDEDMESSAGE);

		// TODO count the commits
		commitCount = 3;

		setupAdditionalCommits();

		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();
		repositoryUtil.addConfiguredRepository(repoFile);

		Repository repo = myRepoViewUtil.lookupRepository(repoFile);

		refFilterHelper = new RefFilterHelper(repo);
		refFilterHelper.setRefFilters(refFilterHelper.getDefaults());
		refFilterHelper.resetLastSelectionStateToDefault();
	}

	private void checkout(Git git, String ref, boolean create)
			throws Exception {
		CheckoutCommand checkout = git.checkout();
		checkout.setName(ref);
		checkout.setCreateBranch(create);
		checkout.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM);
		checkout.call();
	}

	private void commitNewFile(String fileName, String commitMsg)
			throws Exception {
		IProject prj = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		IFile toCreate = prj.getFile(fileName);
		toCreate.create(
				new ByteArrayInputStream(
						"Content".getBytes(prj.getDefaultCharset())),
				false, null);
		addAndCommit(toCreate, commitMsg);
	}

	private static void tag(Git git, String name) throws Exception {
		TagCommand tag = git.tag();
		tag.setName(name);
		PersonIdent committer = new PersonIdent(TestUtil.TESTCOMMITTER_NAME,
				TestUtil.TESTCOMMITTER_EMAIL);
		tag.setTagger(committer);
		Repository repo = git.getRepository();
		RevCommit headCommit = repo.parseCommit(
				repo.exactRef(Constants.HEAD).getLeaf().getObjectId());
		tag.setObjectId(headCommit);
		tag.call();
	}

	private void resetHard(Git git, String to) throws Exception {
		ResetCommand reset = git.reset();
		reset.setRef(to);
		reset.setMode(ResetType.HARD);
		reset.call();
	}

	private void push(Git git) throws Exception {
		PushCommand push = git.push();
		push.setPushAll();
		push.call();
	}

	private void fetch(Git git) throws Exception {
		FetchCommand fetch = git.fetch();
		fetch.call();
	}

	private void setupAdditionalCommits() throws Exception {
		Repository repo = myRepoViewUtil.lookupRepository(repoFile);

		try (Git git = Git.wrap(repo)) {
			createSimpleRemoteRepository(repoFile);

			checkout(git, "master", false);
			checkout(git, "testR", true);
			commitNewFile("testR.txt", "testR");
			push(git);
			resetHard(git, "HEAD~");

			checkout(git, "master", false);
			checkout(git, "testD", true);
			commitNewFile("testDa.txt", "testDa");
			push(git);
			fetch(git);
			resetHard(git, "HEAD~");
			commitNewFile("testDb.txt", "testDb");

			checkout(git, "master", false);
			checkout(git, "test1", true);
			commitNewFile("test1.txt", "test1");

			commitNewFile("test1t.txt", "test1t");
			tag(git, "TEST1t");
			resetHard(git, "HEAD~");

			checkout(git, "master", false);
			checkout(git, "test2", true);
			commitNewFile("test2.txt", "test2");

			checkout(git, "master", false);
			checkout(git, "test12", true);
			commitNewFile("test12.txt", "test12");

			checkout(git, "master", false);
		}
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
		SWTBotMenu filterMenu = view
				.viewMenu(UIText.GitHistoryPage_FilterSubMenuLabel);
		switch (filter) {
		case 0: {
			SWTBotMenu checkedChild = filterMenu
					.menu(new BaseMatcher<MenuItem>() {

						@Override
						public boolean matches(Object item) {
							return item instanceof MenuItem
									&& ((MenuItem) item).getSelection();
						}

						@Override
						public void describeTo(Description description) {
							description.appendText("Checked menu item");

						}
					}, true, 0);
			if (checkedChild != null) {
				checkedChild.click();
			}
			break;
		}
		case 1: {
			SWTBotMenu repo = filterMenu
					.menu(UIText.GitHistoryPage_AllInRepoMenuLabel);
			if (!repo.isChecked())
				repo.click();
			break;
		}
		case 2: {
			SWTBotMenu project = filterMenu
					.menu(UIText.GitHistoryPage_AllInProjectMenuLabel);
			if (!project.isChecked())
				project.click();
			break;
		}
		case 3: {
			SWTBotMenu folder = filterMenu
					.menu(UIText.GitHistoryPage_AllInParentMenuLabel);
			if (!folder.isChecked())
				folder.click();
			break;
		}
		default:
			break;
		}
	}

	@Test
	public void testOpenHistoryOnProject() throws Exception {
		SWTBotTable table = getHistoryViewTable(PROJ1);
		int rowCount = table.rowCount();
		assertTrue(table.rowCount() > 0);
		assertEquals("Initial commit",
				table.getTableItem(rowCount - 1).getText(1));
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

	private SWTBotTable getHistoryViewTable() throws Exception {
		SWTBot historyView = getHistoryViewBot();
		Job.getJobManager().join(JobFamilies.GENERATE_HISTORY, null);
		historyView.getDisplay().syncExec(() -> {
			// Join UI update triggered by GenerateHistoryJob
		});
		return historyView.table();
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
		return getHistoryViewTable();
	}

	private SWTBotTable getFileDiffTable() throws Exception {
		Job.getJobManager().join(JobFamilies.HISTORY_FILE_DIFF, null);
		// Wait a little bit to give the UiJob triggered a chance to run
		Thread.sleep(100);
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			// Join the UI update
		});
		return getHistoryViewBot().table(1);
	}

	private SWTBot getHistoryViewBot() {
		return TestUtil.showHistoryView().bot();
	}

	@Test
	public void testSelectBranch() throws Exception {
		SWTBotTable commitTable = getHistoryViewTable(PROJ1);
		assertEquals("Unexpected number of commits", commitCount,
				commitTable.rowCount());
		// Current branch is "master". Create a new commit on a new branch, then
		// switch back to master.
		try (Git git = new Git(lookupRepository(repoFile))) {
			git.checkout().setCreateBranch(true).setName("otherBranch").call();
			TestUtil.waitForDecorations();
			touchAndSubmit("Updated");
			ObjectId otherCommit = git.getRepository().resolve("otherBranch");
			Ref master = git.checkout().setName(Constants.MASTER).call();
			assertNotNull("Branch is null", master.getLeaf().getObjectId());
			assertNotEquals("Branch not switched", otherCommit,
					master.getLeaf().getObjectId());
			TestUtil.waitForDecorations();
			// History table should not show otherCommit
			commitTable = getHistoryViewTable();
			assertEquals("Unexpected number of commits", commitCount,
					commitTable.rowCount());
			// Open git repo view, select "otherBranch".
			SWTBotView view = TestUtil.showView(RepositoriesView.VIEW_ID);
			TestUtil.joinJobs(JobFamilies.REPO_VIEW_REFRESH);
			TestUtil.waitForDecorations();
			SWTBotTree tree = view.bot().tree();
			SWTBotTreeItem localBranches = myRepoViewUtil
					.getLocalBranchesItem(tree, repoFile);
			TestUtil.expandAndWait(localBranches).getNode("otherBranch")
					.select();
			ContextMenuHelper.clickContextMenuSync(tree, "Show In", "History");
			// History table should show both branches
			commitTable = getHistoryViewTable();
			assertEquals("Unexpected number of commits", commitCount + 1,
					commitTable.rowCount());
			Table swtTable = commitTable.widget;
			ObjectId[] firstId = { null };
			swtTable.getDisplay().syncExec(() -> {
				Object obj = swtTable.getItem(0).getData();
				RevCommit c = Adapters.adapt(obj, RevCommit.class);
				if (c != null) {
					firstId[0] = c.getId();
				}
			});
			assertEquals("Unexpected commit in table", otherCommit, firstId[0]);
		}
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

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

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

	private String[] getCommitMsgsFromUi(final SWTBotTable table) {
		int length = table.rowCount();
		String[] result = new String[length];

		for (int i = 0; i < length; i++) {
			RevCommit commit = getCommitInLine(table, i)[0];
			String msg = commit.getFullMessage();
			// Want newest commit last.
			result[length - (1 + i)] = msg;
		}

		return result;
	}

	private SWTBotMenu getFilterMenuItem(
			SWTBotToolbarDropDownButton selectedRefs, String refFilter) {
		return selectedRefs.menuItem(new TypeSafeMatcher<MenuItem>() {

			@Override
			public void describeTo(Description description) {
				description.appendText(
						"MenuItem for RefFilter \"" + refFilter + "\"");
			}

			@Override
			protected boolean matchesSafely(MenuItem item) {
				return item.getText().startsWith(refFilter);
			}

		});
	}

	private void uncheckRefFilter(SWTBotToolbarDropDownButton selectedRefs,
			String refFilter) {
		SWTBotMenu filter = getFilterMenuItem(selectedRefs, refFilter);
		assertTrue("Expected " + refFilter + " to be checked",
				filter.isChecked());
		filter.click();
	}

	private void checkRefFilter(SWTBotToolbarDropDownButton selectedRefs,
			String refFilter) {
		SWTBotMenu filter = getFilterMenuItem(selectedRefs, refFilter);
		assertTrue("Expected " + refFilter + " to be unchecked",
				!filter.isChecked());
		filter.click();
	}

	private void assertNoCommit(SWTBotTable table) {
		bot.waitUntil(new DefaultCondition() {

			@Override
			public boolean test() throws Exception {
				return table.rowCount() == 0;
			}

			@Override
			public String getFailureMessage() {
				return "CommitGraphTable did not become empty";
			}
		});

		assertThat("Expected no commit", getCommitMsgsFromUi(table),
				emptyArray());
	}

	private void assertCommitsAfterBase(SWTBotTable table, String... commitMsgs)
			throws Exception {
		TestUtil.waitForJobs(50, 5000);
		// There are three expected fixed commits, plus then the ones given in
		// the parameter.
		int expectedNumberOfCommits = commitMsgs.length + 3;
		bot.waitUntil(new DefaultCondition() {

			@Override
			public boolean test() throws Exception {
				return table.rowCount() == expectedNumberOfCommits;
			}

			@Override
			public String getFailureMessage() {
				return "CommitGraphTable did not get expected number of rows: "
						+ expectedNumberOfCommits;
			}

		});
		List<Matcher<? super String>> matchers = new ArrayList<>();
		matchers.add(equalTo("Initial commit"));
		matchers.add(startsWith("Touched at"));
		matchers.add(equalTo("A new file in a new folder"));

		for (String msg : commitMsgs) {
			matchers.add(equalTo(msg));
		}

		assertThat("Expected different commits",
				getCommitMsgsFromUi(table),
				is(arrayContainingInAnyOrder(matchers)));
		table.unselect();
	}

	@Test
	public void testSelectShownRefs() throws Exception {
		Set<RefFilter> filters = refFilterHelper.getRefFilters();
		filters.add(refFilterHelper.new RefFilter("refs/heads/test1"));
		filters.add(refFilterHelper.new RefFilter("refs/heads/test?"));
		filters.add(refFilterHelper.new RefFilter("refs/heads/test*"));
		refFilterHelper.setRefFilters(filters);

		Repository repo = myRepoViewUtil.lookupRepository(repoFile);

		SWTBotTable table = getHistoryViewTable(PROJ1);
		SWTBotView view = bot.viewById(IHistoryView.VIEW_ID);
		SWTBotToolbarDropDownButton selectedRefs = (SWTBotToolbarDropDownButton) view
				.toolbarButton(UIText.GitHistoryPage_showingHistoryOfHead);

		try(Git git = Git.wrap(repo)) {
			checkout(git, "testD", false);
		}
		assertCommitsAfterBase(table, "testDb");

		uncheckRefFilter(selectedRefs, "HEAD");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "refs/**/${git_branch}");
		assertCommitsAfterBase(table, "testDa", "testDb");

		uncheckRefFilter(selectedRefs, "refs/**/${git_branch}");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "refs/heads/**");
		assertCommitsAfterBase(table, "test1", "test2", "test12", "testDb");

		uncheckRefFilter(selectedRefs, "refs/heads/**");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "refs/remotes/**");
		assertCommitsAfterBase(table, "testDa", "testR");

		uncheckRefFilter(selectedRefs, "refs/remotes/**");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "refs/tags/**");
		assertCommitsAfterBase(table, "test1", "test1t");

		uncheckRefFilter(selectedRefs, "refs/tags/**");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "refs/heads/test1");
		assertCommitsAfterBase(table, "test1");

		uncheckRefFilter(selectedRefs, "refs/heads/test1");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "refs/heads/test?");
		assertCommitsAfterBase(table, "test1", "test2", "testDb");

		uncheckRefFilter(selectedRefs, "refs/heads/test?");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "refs/heads/test*");
		assertCommitsAfterBase(table, "test1", "test2", "test12", "testDb");

		uncheckRefFilter(selectedRefs, "refs/heads/test*");
		assertNoCommit(table);
	}

	@Test
	public void testToggleShownRefs() throws Exception {
		SWTBotTable table = getHistoryViewTable(PROJ1);
		SWTBotView view = bot.viewById(IHistoryView.VIEW_ID);
		SWTBotToolbarDropDownButton selectedRefs = (SWTBotToolbarDropDownButton) view
				.toolbarButton(UIText.GitHistoryPage_showingHistoryOfHead);

		checkRefFilter(selectedRefs, "refs/heads/**");
		checkRefFilter(selectedRefs, "refs/remotes/**");
		checkRefFilter(selectedRefs, "refs/tags/**");

		assertCommitsAfterBase(table, "test1", "test2", "test12", "testDa",
				"testDb", "test1t", "testR");

		selectedRefs.click();
		assertCommitsAfterBase(table);

		uncheckRefFilter(selectedRefs, "HEAD");
		assertNoCommit(table);

		checkRefFilter(selectedRefs, "HEAD");
		assertCommitsAfterBase(table);

		selectedRefs.click();
		assertCommitsAfterBase(table, "test1", "test2", "test12", "testDa",
				"testDb", "test1t", "testR");

		uncheckRefFilter(selectedRefs, "refs/heads/**");
		uncheckRefFilter(selectedRefs, "refs/remotes/**");
		uncheckRefFilter(selectedRefs, "refs/tags/**");
	}

	@Test
	public void testOpenRefFilterDialogFromDropdown() throws Exception {
		getHistoryViewTable(PROJ1); // Make sure the history view is visible
		SWTBotView view = bot.viewById(IHistoryView.VIEW_ID);
		SWTBotToolbarDropDownButton selectedRefs = (SWTBotToolbarDropDownButton) view
				.toolbarButton(UIText.GitHistoryPage_showingHistoryOfHead);

		selectedRefs.menuItem(UIText.GitHistoryPage_configureFilters).click();
		// This will cause an exception if the dialog is not found
		bot.shell(UIText.GitHistoryPage_filterRefDialog_dialogTitle).bot()
				.button(IDialogConstants.OK_LABEL).click();
	}

	@Test
	public void testOpenRefFilterDialogFromMenu() throws Exception {
		getHistoryViewTable(PROJ1); // Make sure the history view is visible
		SWTBotView view = bot.viewById(IHistoryView.VIEW_ID);

		view.viewMenu(UIText.GitHistoryPage_configureFilters).click();
		// This will cause an exception if the dialog is not found
		bot.shell(UIText.GitHistoryPage_filterRefDialog_dialogTitle).bot()
				.button(IDialogConstants.OK_LABEL).click();
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

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

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
		bot.editorByTitle(
				FILE1 + " " + commit.getParent(0).getName().substring(0, 7));
	}

	@Test
	public void testStartGitflowReleaseEnabled() throws Exception {
		Repository repository = lookupRepository(repoFile);
		new InitOperation(repository).execute(null);

		final SWTBotTable table = getHistoryViewTable(PROJ1);
		table.getTableItem(1).select();

		String itemLabelRegex = NLS.bind(org.eclipse.egit.gitflow.ui.internal.
						UIText.DynamicHistoryMenu_startGitflowReleaseFrom, ".*");
		SWTBotMenu startReleaseMenu = table.contextMenu().menu(withRegex(itemLabelRegex),
				true, 0);

		assertTrue(startReleaseMenu.isEnabled());
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

	private RevCommit[] getCommitInLine(SWTBotTable table, int line) {
		table.getTableItem(line).select();
		final RevCommit[] commit = new RevCommit[1];

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				TableItem tableItem = table.widget.getSelection()[0];
				ensureTableItemLoaded(tableItem);
				commit[0] = (RevCommit) tableItem.getData();
			}
		});

		return commit;
	}

	private RevCommit[] checkoutLine(final SWTBotTable table, int line)
			throws InterruptedException {
		table.getTableItem(line).select();
		final RevCommit[] commit = getCommitInLine(table, line);

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

/******************************************************************************
 *  Copyright (c) 2014 Tasktop Technologies.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Tomasz Zarna (Tasktop) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;
import org.junit.Before;
import org.junit.Test;

public class SwitchToMenuTest extends LocalRepositoryTestCase {

	private static final String TEAM_LABEL = util
			.getPluginLocalizedValue("TeamMenu.label");

	private static final String SWITCH_TO_LABEL_MULTIPLE = util
			.getPluginLocalizedValue("SwitchToMenuMultiple.label");

	private SwitchToMenu switchToMenu;

	private IHandlerService handlerService;

	@Before
	public void setUp() throws Exception {
		switchToMenu = new SwitchToMenu();
		handlerService = mock(IHandlerService.class);
		IServiceLocator serviceLocator = mock(IServiceLocator.class);
		when(serviceLocator.getService(IHandlerService.class)).thenReturn(
				handlerService);
		switchToMenu.initialize(serviceLocator);
	}

	@Test
	public void emptySelection() {
		mockSelection(new EmptySelection());

		MenuItem[] items = fillMenu();

		assertEquals(0, items.length);
	}

	@Test
	public void selectionNotAdaptableToRepository() {
		mockSelection(
				new StructuredSelection(new Object()));

		MenuItem[] items = fillMenu();

		assertEquals(0, items.length);
	}

	@Test
	public void selectionWithProj1() throws Exception {
		createProjectAndCommitToRepository();
		selectionWithProj1Common();
	}

	@Test
	public void selectionWithProj1AndReflog() throws Exception {
		File gitDir = createProjectAndCommitToRepository();

		// create additional reflog entries
		try (Git git = new Git(lookupRepository(gitDir))) {
			git.checkout().setName("stable").call();
			git.checkout().setName("master").call();
		}

		selectionWithProj1Common();

		// delete reflog again to not confuse other tests
		new File(gitDir, Constants.LOGS + "/" + Constants.HEAD).delete();
	}

	private void selectionWithProj1Common() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		mockSelection(new StructuredSelection(project));

		MenuItem[] items = fillMenu();

		assertEquals(6, items.length);
		assertTextEquals(UIText.SwitchToMenu_NewBranchMenuLabel, items[0]);
		assertStyleEquals(SWT.SEPARATOR, items[1]);
		assertTextEquals("master", items[2]);
		assertTextEquals("stable", items[3]);
		assertStyleEquals(SWT.SEPARATOR, items[4]);
		assertTextEquals(UIText.SwitchToMenu_OtherMenuLabel, items[5]);
	}

	@Test
	public void selectionWithRepositoryHavingOver20Branches() throws Exception {
		Repository repo = lookupRepository(createProjectAndCommitToRepository());
		for (int i = 0; i < SwitchToMenu.MAX_NUM_MENU_ENTRIES; i++) {
			createBranch(repo, "refs/heads/change/" + i);
		}
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		mockSelection(new StructuredSelection(project));

		MenuItem[] items = fillMenu();

		assertEquals(24, items.length);
		assertTextEquals(UIText.SwitchToMenu_NewBranchMenuLabel, items[0]);
		assertStyleEquals(SWT.SEPARATOR, items[1]);
		assertTextEquals("change/0", items[2]);
		assertTextEquals("change/1", items[3]);
		assertTextEquals("change/2", items[4]);
		assertTextEquals("change/3", items[5]);
		assertTextEquals("change/4", items[6]);
		assertTextEquals("change/5", items[7]);
		assertTextEquals("change/6", items[8]);
		assertTextEquals("change/7", items[9]);
		assertTextEquals("change/8", items[10]);
		assertTextEquals("change/9", items[11]);
		assertTextEquals("change/10", items[12]);
		assertTextEquals("change/11", items[13]);
		assertTextEquals("change/12", items[14]);
		assertTextEquals("change/13", items[15]);
		assertTextEquals("change/14", items[16]);
		assertTextEquals("change/15", items[17]);
		assertTextEquals("change/16", items[18]);
		assertTextEquals("change/17", items[19]);
		assertTextEquals("change/18", items[20]);
		assertTextEquals("change/19", items[21]);
		// "master" and "stable" didn't make it
		assertStyleEquals(SWT.SEPARATOR, items[22]);
		assertTextEquals(UIText.SwitchToMenu_OtherMenuLabel, items[23]);
	}

	@Test
	public void validateMenuEntriesForMultiSelectionWithMultipleRepositories()
			throws Exception {
		File gitOne = createProjectAndCommitToRepository(REPO1, PROJ1);
		File gitTwo = createProjectAndCommitToRepository(REPO2, PROJ2);

		Repository repoOne = lookupRepository(gitOne);
		Repository repoTwo = lookupRepository(gitTwo);
		for (int i = 0; i < SwitchToMenu.MAX_NUM_MENU_ENTRIES; i++) {
			createBranch(repoOne, "refs/heads/change/" + i);
			createBranch(repoTwo, "refs/heads/change/" + (i + 15));
		}

		mockMultiProjectSelection(PROJ1, PROJ2);

		MenuItem[] items = fillMenu();
		assertTextEquals("change/15", items[0]);
		assertTextEquals("change/16", items[1]);
		assertTextEquals("change/17", items[2]);
		assertTextEquals("change/18", items[3]);
		assertTextEquals("change/19", items[4]);
		assertTextEquals("master", items[5]);
		assertTextEquals("stable", items[6]);
	}

	@Test
	public void validateBranchSwitchingForForMultiSelectionWithMultipleRepositories()
			throws Exception {

		File gitOne = createProjectAndCommitToRepository(REPO1, PROJ1);
		File gitTwo = createProjectAndCommitToRepository(REPO2, PROJ2);
		Repository repoOne = lookupRepository(gitOne);
		Repository repoTwo = lookupRepository(gitTwo);

		// Set up different branch sources
		try (Git git = new Git(repoOne)) {
			git.checkout().setName("master").call();
		}

		try (Git git = new Git(repoTwo)) {
			git.checkout().setName("stable").call();
		}

		String branchName = "commonBranchAmongRepositories";
		String branchRef = "refs/heads/" + branchName;
		createBranch(repoOne, branchRef);
		createBranch(repoTwo, branchRef);

		assertEquals("master", repoOne.getBranch());
		assertEquals("stable", repoTwo.getBranch());

		// Multi repository Switch To
		SWTBotTree tree = TestUtil.getExplorerTree();
		SWTBotTree select = tree.select(tree.getAllItems());
		ContextMenuHelper.clickContextMenu(select, TEAM_LABEL,
				SWITCH_TO_LABEL_MULTIPLE, branchName);
		TestUtil.joinJobs(JobFamilies.CHECKOUT);

		assertEquals(branchName, repoOne.getBranch());
		assertEquals(branchName, repoTwo.getBranch());
	}

	@Test
	public void multipleSelectionWithMultipleRepositoriesAndNoCommonBranches()
			throws Exception {
		File gitOne = createProjectAndCommitToRepository(REPO1, PROJ1);
		File gitTwo = createProjectAndCommitToRepository(REPO2, PROJ2);

		try (Git git = new Git(lookupRepository(gitOne))) {
			git.checkout().setName("stable").call();
			git.branchDelete().setBranchNames("master").setForce(true).call();
		}

		try (Git git = new Git(lookupRepository(gitTwo))) {
			git.branchDelete().setBranchNames("stable").setForce(true).call();
		}

		mockMultiProjectSelection(PROJ1, PROJ2);

		MenuItem[] items = fillMenu();
		assertTextEquals(UIText.SwitchToMenu_NoCommonBranchesFound, items[0]);

		// delete reflog again to not confuse other tests
		new File(gitOne, Constants.LOGS + "/" + Constants.HEAD).delete();
		new File(gitTwo, Constants.LOGS + "/" + Constants.HEAD).delete();
	}

	private void mockSelection(ISelection selection) {
		EvaluationContext context = new EvaluationContext(null, new Object());
		context.addVariable(ISources.ACTIVE_MENU_SELECTION_NAME, selection);
		when(handlerService.getCurrentState()).thenReturn(context);
	}

	private void mockMultiProjectSelection(String... projNames) {

		List<IProject> projects = new ArrayList<>();
		for (String s : projNames) {
			projects.add(
					ResourcesPlugin.getWorkspace().getRoot().getProject(s));
		}
		mockSelection(new StructuredSelection(projects));
	}

	private MenuItem[] fillMenu() {
		final MenuItem[][] items = new MenuItem[1][];
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Menu menu = new Menu(new Shell(Display.getDefault()));
				switchToMenu.fill(menu, 0 /* index */);
				items[0] = menu.getItems();
			}
		});
		return items[0];
	}

	private static class EmptySelection implements ISelection {
		@Override
		public boolean isEmpty() {
			return true;
		}
	}

	private static void assertTextEquals(final String expectedText,
			final MenuItem item) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				assertEquals(expectedText, item.getText());
			}
		});
	}

	private static void assertStyleEquals(final int expectedStyle,
			final MenuItem item) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				assertEquals(expectedStyle, item.getStyle());
			}
		});
	}
}

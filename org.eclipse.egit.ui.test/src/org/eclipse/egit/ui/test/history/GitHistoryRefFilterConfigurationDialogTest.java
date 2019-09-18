/*******************************************************************************
 * Copyright (C) 2019, Tim Neumann <Tim.Neumann@advantest.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryRefFilterConfigurationDialog;
import org.eclipse.egit.ui.internal.history.RefFilterHelper;
import org.eclipse.egit.ui.internal.history.RefFilterHelper.RefFilter;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(SWTBotJunit4ClassRunner.class)
public class GitHistoryRefFilterConfigurationDialogTest
		extends LocalRepositoryTestCase {
	private Display display;

	private Repository repo;

	private SWTBotShell dialogBot;

	private GitHistoryRefFilterConfigurationDialog dialog;

	private RefFilterHelper refFilterHelper;

	private GitRepositoriesViewTestUtils myRepoViewUtil;

	private RefFilter newRefFilter(String filterString, boolean preConfigured,
			boolean selected) {
		return RefFilterUtil.newRefFilter(refFilterHelper, filterString,
				preConfigured, selected);
	}

	@Before
	public void setupTests() throws Exception {
		display = PlatformUI.getWorkbench().getDisplay();

		myRepoViewUtil = new GitRepositoriesViewTestUtils();
		File repoFile = createProjectAndCommitToRepository();

		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();
		repositoryUtil.addConfiguredRepository(repoFile);

		repo = myRepoViewUtil.lookupRepository(repoFile);

		refFilterHelper = mock(RefFilterHelper.class);

		List<RefFilter> testFilters = new ArrayList<>();
		testFilters.add(newRefFilter("HEAD", true, false));
		testFilters.add(newRefFilter("refs/**/[CURRENT-BRANCH]", true, false));
		testFilters.add(newRefFilter("refs/heads/**", true, true));
		testFilters.add(newRefFilter("refs/remotes/**", true, false));
		testFilters.add(newRefFilter("refs/tags/**", true, false));
		testFilters.add(newRefFilter("Mock", false, false));
		testFilters.add(newRefFilter("test", false, false));
		testFilters.add(newRefFilter("filter", false, true));

		when(refFilterHelper.getRefFilters())
				.thenReturn(new HashSet<>(testFilters));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Set<RefFilter> filters = invocation.getArgument(0);
				for (RefFilter filter : filters) {
					filter.setSelected(filter.equals(testFilters.get(0)));
				}
				return null;
			}
		}).when(refFilterHelper).selectOnlyHEAD(anySet());

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Set<RefFilter> filters = invocation.getArgument(0);
				for (RefFilter filter : filters) {
					filter.setSelected(filter.equals(testFilters.get(1)));
				}
				return null;
			}
		}).when(refFilterHelper).selectOnlyCurrentBranch(anySet());

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Set<RefFilter> filters = invocation.getArgument(0);
				for (RefFilter filter : filters) {
					filter.setSelected(filter.equals(testFilters.get(0))
							|| filter.equals(testFilters.get(2))
							|| filter.equals(testFilters.get(3))
							|| filter.equals(testFilters.get(4)));
				}
				return null;
			}
		}).when(refFilterHelper).selectExactlyAllBranchesAndTags(anySet());

		List<RefFilter> defaults = new ArrayList<>();
		defaults.add(newRefFilter("HEAD", true, true));
		defaults.add(newRefFilter("refs/**/[CURRENT-BRANCH]", true, false));
		defaults.add(newRefFilter("refs/heads/**", true, false));
		defaults.add(newRefFilter("refs/remotes/**", true, false));
		defaults.add(newRefFilter("refs/tags/**", true, false));

		when(refFilterHelper.getDefaults())
				.thenReturn(new HashSet<>(defaults));

		display.asyncExec(() -> {
			dialog = new GitHistoryRefFilterConfigurationDialog(
					display.getActiveShell(), repo, refFilterHelper);
			dialog.open();
		});
		dialogBot = bot
				.shell(UIText.GitHistoryPage_filterRefDialog_dialogTitle);
	}

	@After
	public void teardown() {
		myRepoViewUtil.dispose();
		display.asyncExec(dialog::close);
	}

	private static class RefFilterInfo {
		private final String filterString;

		private final boolean preConfigured;

		private final boolean checked;

		public RefFilterInfo(String filterString, boolean preConfigured,
				boolean checked) {
			this.filterString = filterString;
			this.preConfigured = preConfigured;
			this.checked = checked;
		}

		public String getFilterString() {
			return filterString;
		}

		public boolean isPreConfigured() {
			return preConfigured;
		}

		public boolean isChecked() {
			return checked;
		}
	}

	private List<Matcher<? super RefFilter>> getRefFilterMatchersFromInfos(
			List<RefFilterInfo> infos) {
		return infos.stream().map(info -> newRefFilterMatcher(info))
				.collect(Collectors.toList());
	}

	private Matcher<RefFilter> newRefFilterMatcher(RefFilterInfo info) {
		return RefFilterUtil.newRefFilterMatcher(info.getFilterString(),
				info.isPreConfigured(), info.isChecked());
	}

	private List<Matcher<? super SWTBotTableItem>> getTableRowMatchersFromInfos(
			List<RefFilterInfo> infos) {
		return infos.stream().map(info -> newTableRowMatcher(info))
				.collect(Collectors.toList());
	}

	private Matcher<SWTBotTableItem> newTableRowMatcher(RefFilterInfo info) {
		return newTableRowMatcher(info.getFilterString(),
				info.isPreConfigured(), info.isChecked());
	}

	private Matcher<SWTBotTableItem> newTableRowMatcher(String text,
			boolean preConfigured, boolean checked) {
		return new TypeSafeMatcher<SWTBotTableItem>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("A table row with ");
				description.appendValue(text);
				description.appendText(" which is ");
				if (!checked) {
					description.appendText("_not_ ");
				}
				description.appendText("checked");

			}

			private boolean matchText(String itemText) {
				if (preConfigured) {
					return Objects.equals(itemText,
							text + " - (preconfigured)");
				} else {
					return Objects.equals(itemText, text);
				}
			}

			@Override
			protected boolean matchesSafely(SWTBotTableItem item) {
				return matchText(item.getText()) && item.isChecked() == checked;
			}
		};
	}

	private List<SWTBotTableItem> listOfRows(SWTBotTable table) {
		int rows = table.rowCount();
		List<SWTBotTableItem> result = new ArrayList<>(rows);
		for (int i = 0; i < rows; i++) {
			result.add(table.getTableItem(i));
		}
		return result;
	}

	private void click(String button) {
		SWTBotButton btn = dialogBot.bot().button(button);
		btn.click();
	}

	private void clickOk() {
		click(IDialogConstants.OK_LABEL);
	}

	private void clickCancel() {
		click(IDialogConstants.CANCEL_LABEL);
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<Set<RefFilter>> newRefFilterSetArgCaptor() {
		return ArgumentCaptor.forClass(Set.class);
	}

	private void verifyTableContents(List<RefFilterInfo> expected) {
		SWTBotTable table = dialogBot.bot().table();

		assertThat("Expeceted different rows", listOfRows(table),
				contains(getTableRowMatchersFromInfos(expected)));
	}

	private void verifyResult(List<RefFilterInfo> expected) {
		ArgumentCaptor<Set<RefFilter>> argument = newRefFilterSetArgCaptor();
		verify(refFilterHelper, times(1)).setRefFilters(argument.capture());

		assertThat("Expected different set to be saved", argument.getValue(),
				containsInAnyOrder(getRefFilterMatchersFromInfos(expected)));
	}

	private void verifyTableContentsClickOkAndVerifyResult(
			List<RefFilterInfo> expected) {
		verifyTableContents(expected);
		clickOk();
		verifyResult(expected);
	}

	@Test
	public void testInitialTableContent() throws Exception {
		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testCheckOne() throws Exception {
		SWTBotTable table = dialogBot.bot().table();

		table.getTableItem(0).check();

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, true));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testCheckTwo() throws Exception {
		SWTBotTable table = dialogBot.bot().table();

		table.getTableItem(3).check();
		table.getTableItem(6).check();

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, true));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, true));
		expected.add(new RefFilterInfo("Mock", false, true));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testUncheckOne() throws Exception {
		SWTBotTable table = dialogBot.bot().table();

		table.getTableItem(2).uncheck();

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, false));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testUncheckTwo() throws Exception {
		SWTBotTable table = dialogBot.bot().table();

		table.getTableItem(2).uncheck();
		table.getTableItem(5).uncheck();

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, false));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, false));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testUncheckAndCheck() throws Exception {
		SWTBotTable table = dialogBot.bot().table();

		table.getTableItem(2).uncheck();
		table.getTableItem(7).check();

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, false));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, true));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	private Event createEvent(Control control) {
		Event event = new Event();
		event.keyCode = SWT.NONE;
		event.stateMask = SWT.NONE;
		event.doit = true;
		event.widget = control;
		event.button = 1;
		return event;
	}

	private void keyEvent(Control control, int keyCode) {
		Event event = createEvent(control);
		event.button = 0;
		event.type = SWT.KeyDown;
		event.keyCode = keyCode;
		event.character = (char) keyCode;

		Display.getDefault().post(event);

		event.type = SWT.KeyUp;

		Display.getDefault().post(event);
	}

	private void typeTextAndEnter(String text) {
		Control c = dialog.getShell();
		for (int i = 0; i < text.length(); i++) {
			keyEvent(c, text.charAt(i));
		}
		keyEvent(c, 13);
	}

	@Test
	public void testAdd() throws Exception {
		click(UIText.GitHistoryPage_filterRefDialog_button_add);

		typeTextAndEnter("added");

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("added", false, false));
		expected.add(new RefFilterInfo("filter", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	private SWTBotTreeItem findMaster(SWTBotTreeItem[] items) {
		for (SWTBotTreeItem item : items) {
			if (item.getText().startsWith("master")) {
				return item;
			}
		}

		SWTBotTreeItem found = null;

		for (SWTBotTreeItem item : items) {
			found = findMaster(item.getItems());
			if (found != null) {
				return found;
			}
		}

		return null;
	}

	@Test
	public void testAddRef() throws Exception {
		click(UIText.GitHistoryPage_filterRefDialog_button_addRef);
		SWTBotShell refSelectDialogBot = bot.shell(
				UIText.GitHistoryPage_filterRefDialog_selectRefDialog_dialogTitle);

		SWTBotTree treeBot = refSelectDialogBot.bot().tree();
		SWTBotTreeItem[] items = treeBot.getAllItems();

		SWTBotTreeItem masterItem = findMaster(items);

		if (masterItem == null) {
			fail("No master branch in select ref dialog.");
		} else {
			masterItem.select();
		}

		refSelectDialogBot.bot().button(IDialogConstants.OK_LABEL).click();

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("refs/heads/master", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testRemove() throws Exception {
		SWTBotTable table = dialogBot.bot().table();
		table.getTableItem(5).select();
		click(UIText.GitHistoryPage_filterRefDialog_button_remove);

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testEdit() throws Exception {
		SWTBotTable table = dialogBot.bot().table();
		table.getTableItem(5).select();
		click(UIText.GitHistoryPage_filterRefDialog_button_edit);

		typeTextAndEnter("edited");

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("edited", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testDoubleClick() throws Exception {
		SWTBotTable table = dialogBot.bot().table();
		SWTBotTableItem item = table.getTableItem(5);
		item.doubleClick();

		typeTextAndEnter("edited");

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("edited", false, true));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testButtonHead() throws Exception {

		click(UIText.GitHistoryPage_filterRefDialog_button_headOnly);

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, true));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, false));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, false));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testButtonCurrentBranch() throws Exception {
		click(UIText.GitHistoryPage_filterRefDialog_button_currentBranchOnly);

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, false));
		expected.add(new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, true));
		expected.add(new RefFilterInfo("refs/heads/**", true, false));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));
		expected.add(new RefFilterInfo("filter", false, false));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testButtonAllBranches() throws Exception {
		click(UIText.GitHistoryPage_filterRefDialog_button_allBranchesAndTags);

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, true));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, true));
		expected.add(new RefFilterInfo("refs/remotes/**", true, true));
		expected.add(new RefFilterInfo("refs/tags/**", true, true));
		expected.add(new RefFilterInfo("filter", false, false));
		expected.add(new RefFilterInfo("Mock", false, false));
		expected.add(new RefFilterInfo("test", false, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
	}

	@Test
	public void testButtonRestoreDefaults() throws Exception {
		click(JFaceResources.getString("defaults"));

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, true));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, false));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));

		verifyTableContents(expected);
		verify(refFilterHelper, Mockito.never())
				.resetLastSelectionStateToDefault();
	}

	@Test
	public void testButtonRestoreDefaultsAndOk() throws Exception {
		click(JFaceResources.getString("defaults"));

		List<RefFilterInfo> expected = new ArrayList<>();
		expected.add(new RefFilterInfo("HEAD", true, true));
		expected.add(
				new RefFilterInfo("refs/**/[CURRENT-BRANCH]", true, false));
		expected.add(new RefFilterInfo("refs/heads/**", true, false));
		expected.add(new RefFilterInfo("refs/remotes/**", true, false));
		expected.add(new RefFilterInfo("refs/tags/**", true, false));

		verifyTableContentsClickOkAndVerifyResult(expected);
		verify(refFilterHelper).resetLastSelectionStateToDefault();
	}

	@Test
	public void testButtonRestoreDefaultsAndCancel() throws Exception {
		click(JFaceResources.getString("defaults"));
		click(IDialogConstants.CANCEL_LABEL);

		verify(refFilterHelper, Mockito.never())
				.setRefFilters(ArgumentMatchers.any());
		verify(refFilterHelper, Mockito.never())
				.resetLastSelectionStateToDefault();
	}

	@Test
	public void testCancel() throws Exception {
		clickCancel();
		verify(refFilterHelper, Mockito.never())
				.setRefFilters(ArgumentMatchers.any());
	}

	@Test
	public void testCancelAfterCheck() throws Exception {
		SWTBotTable table = dialogBot.bot().table();

		table.getTableItem(0).check();

		clickCancel();
		verify(refFilterHelper, Mockito.never())
				.setRefFilters(ArgumentMatchers.any());
	}

	@Test
	public void testCancelAfterUncheck() throws Exception {
		SWTBotTable table = dialogBot.bot().table();

		table.getTableItem(2).uncheck();

		clickCancel();
		verify(refFilterHelper, Mockito.never())
				.setRefFilters(ArgumentMatchers.any());
	}

	@Test
	public void testCancelAfterAdd() throws Exception {
		click(UIText.GitHistoryPage_filterRefDialog_button_add);

		typeTextAndEnter("added");

		clickCancel();
		verify(refFilterHelper, Mockito.never())
				.setRefFilters(ArgumentMatchers.any());
	}

	@Test
	public void testCancelAfterEdit() throws Exception {
		SWTBotTable table = dialogBot.bot().table();
		table.getTableItem(5).select();
		click(UIText.GitHistoryPage_filterRefDialog_button_edit);

		typeTextAndEnter("edited");

		clickCancel();
		verify(refFilterHelper, Mockito.never())
				.setRefFilters(ArgumentMatchers.any());
	}

	@Test
	public void testCancelAfterButtonHead() throws Exception {
		click(UIText.GitHistoryPage_filterRefDialog_button_headOnly);

		clickCancel();
		verify(refFilterHelper, Mockito.never())
				.setRefFilters(ArgumentMatchers.any());
	}
}

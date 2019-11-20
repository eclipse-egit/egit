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

import static org.eclipse.egit.ui.test.history.RefFilterUtil.newRefFilterMatcher;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.history.RefFilterHelper;
import org.eclipse.egit.ui.internal.history.RefFilterHelper.RefFilter;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RefFilterHelperTest extends LocalRepositoryTestCase {

	private Repository repo;

	private RefFilterHelper refFilterHelper;

	private IPersistentPreferenceStore prefStore;

	private GitRepositoriesViewTestUtils myRepoViewUtil;

	@SuppressWarnings("boxing")
	@Before
	public void setupTests() throws Exception {
		myRepoViewUtil = new GitRepositoriesViewTestUtils();
		File repoFile = createProjectAndCommitToRepository();
		createSimpleRemoteRepository(repoFile);

		RepositoryUtil repositoryUtil = Activator.getDefault()
				.getRepositoryUtil();
		repositoryUtil.addConfiguredRepository(repoFile);

		repo = myRepoViewUtil.lookupRepository(repoFile);

		try (Git git = Git.wrap(repo)) {
			TagCommand tag = git.tag();
			tag.setName("TestTag");
			PersonIdent committer = new PersonIdent(TestUtil.TESTCOMMITTER_NAME,
					TestUtil.TESTCOMMITTER_EMAIL);
			tag.setTagger(committer);
			RevCommit headCommit = repo.parseCommit(
					repo.exactRef(Constants.HEAD).getLeaf().getObjectId());
			tag.setObjectId(headCommit);
			tag.call();
		}
		prefStore = mock(IPersistentPreferenceStore.class);

		when(prefStore.needsSaving()).thenReturn(true);
		when(prefStore.getDefaultString(anyString())).thenReturn("");

		refFilterHelper = new RefFilterHelper(repo, prefStore);
	}

	@After
	public void teardown() {
		myRepoViewUtil.dispose();
	}

	private String getRepoSpecificPrefKeyForConfiguredFilters() {
		return Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(this.repo,
						UIPreferences.RESOURCEHISTORY_REF_FILTERS);
	}

	@Test
	public void testConstructor() throws Exception {
		verify(prefStore)
				.getDefaultString(UIPreferences.RESOURCEHISTORY_REF_FILTERS);
		verify(prefStore, times(2)).getDefaultString(
				UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS);
		verify(prefStore).getDefaultString(
				UIPreferences.RESOURCEHISTORY_LAST_SELECTED_REF_FILTERS);
		verify(prefStore).setDefault(
				eq(getRepoSpecificPrefKeyForConfiguredFilters()), eq(""));
		verify(prefStore).setDefault(
				eq(getRepoSpecificPrefKeyForSelectedFilters()), eq(""));
		verify(prefStore).setDefault(
				eq(getRepoSpecificPrefKeyForLastSelectedFilters()), eq(""));
	}

	@Test
	public void testGetConfiguredFilters() throws Exception {
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForConfiguredFilters())))
						.thenReturn("Mock:test:filter");
		List<String> result = refFilterHelper.getConfiguredFilters();
		assertThat("Expected different list of configured filters", result,
				containsInAnyOrder("Mock", "test", "filter"));
	}

	@Test
	public void testSetConfiguredFiltersNoSave() throws Exception {
		List<String> filters = Arrays.asList("Mock", "test", "filter");

		refFilterHelper.setConfiguredFilters(filters, false);
		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForConfiguredFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("Mock", "test", "filter"))));
		verify(prefStore, never()).save();
	}

	@Test
	public void testSetConfiguredFiltersWithSave() throws Exception {
		List<String> filters = Arrays.asList("Mock", "test", "filter");

		refFilterHelper.setConfiguredFilters(filters, true);
		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForConfiguredFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("Mock", "test", "filter"))));
		verify(prefStore).save();
	}

	private String getRepoSpecificPrefKeyForSelectedFilters() {
		return Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(this.repo,
						UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS);
	}

	@Test
	public void testGetSelectedFilters() throws Exception {
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForSelectedFilters())))
						.thenReturn("Mock:test:filter");
		List<String> result = refFilterHelper.getSelectedFilters();
		assertThat("Expected different list of selected filters", result,
				containsInAnyOrder("Mock", "test", "filter"));
	}

	@Test
	public void testSetSelectedFiltersNoSave() throws Exception {
		List<String> filters = Arrays.asList("Mock", "test", "filter");

		refFilterHelper.setSelectedFilters(filters, false);
		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForSelectedFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("Mock", "test", "filter"))));
		verify(prefStore, never()).save();
	}

	@Test
	public void testSetSelectedFiltersWithSave() throws Exception {
		List<String> filters = Arrays.asList("Mock", "test", "filter");

		refFilterHelper.setSelectedFilters(filters, true);
		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForSelectedFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("Mock", "test", "filter"))));
		verify(prefStore).save();
	}

	private String getRepoSpecificPrefKeyForLastSelectedFilters() {
		return Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(this.repo,
						UIPreferences.RESOURCEHISTORY_LAST_SELECTED_REF_FILTERS);
	}

	@Test
	public void testGetLastSelectedFilters() throws Exception {
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForLastSelectedFilters())))
						.thenReturn("Mock:test:filter");
		List<String> result = refFilterHelper.getLastSelectedFilters();
		assertThat("Expected different list of last selected filters", result,
				containsInAnyOrder("Mock", "test", "filter"));
	}

	@Test
	public void testSetLastSelectedFiltersNoSave() throws Exception {
		List<String> filters = Arrays.asList("Mock", "test", "filter");

		refFilterHelper.setLastSelectedFilters(filters, false);
		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForLastSelectedFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("Mock", "test", "filter"))));
		verify(prefStore, never()).save();
	}

	@Test
	public void testSetLastSelectedFiltersWithSave() throws Exception {
		List<String> filters = Arrays.asList("Mock", "test", "filter");

		refFilterHelper.setLastSelectedFilters(filters, true);
		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForLastSelectedFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("Mock", "test", "filter"))));
		verify(prefStore).save();
	}

	private RefFilter newRefFilter(String filterString, boolean preConfigured,
			boolean selected) {
		return RefFilterUtil.newRefFilter(refFilterHelper, filterString,
				preConfigured, selected);
	}

	@Test
	public void testGetFilters() throws Exception {
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForConfiguredFilters())))
						.thenReturn("Mock:test:filter");
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForSelectedFilters())))
						.thenReturn("HEAD:test");
		Set<RefFilter> result = refFilterHelper.getRefFilters();

		List<Matcher<? super RefFilter>> expected = new ArrayList<>();
		expected.add(newRefFilterMatcher("HEAD", true, true));
		expected.add(
				newRefFilterMatcher("refs/**/${git_branch}", true, false));
		expected.add(newRefFilterMatcher("refs/heads/**", true, false));
		expected.add(newRefFilterMatcher("refs/remotes/**", true, false));
		expected.add(newRefFilterMatcher("refs/tags/**", true, false));
		expected.add(newRefFilterMatcher("Mock", false, false));
		expected.add(newRefFilterMatcher("test", false, true));
		expected.add(newRefFilterMatcher("filter", false, false));

		assertThat("Expected different filters", result,
				containsInAnyOrder(expected));
	}

	@Test
	public void testRestoreLastSelectionState() throws Exception {
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForLastSelectedFilters())))
						.thenReturn("HEAD:test");
		Set<RefFilter> testSet = new HashSet<>();
		testSet.add(newRefFilter("HEAD", true, false));
		testSet.add(newRefFilter("refs/**/${git_branch}", true,
				false));
		testSet.add(newRefFilter("refs/heads/**", true, true));
		testSet.add(newRefFilter("refs/remotes/**", true, false));
		testSet.add(newRefFilter("refs/tags/**", true, false));
		testSet.add(newRefFilter("Mock", false, false));
		testSet.add(newRefFilter("test", false, false));
		testSet.add(newRefFilter("filter", false, true));

		List<Matcher<? super RefFilter>> expected = new ArrayList<>();
		expected.add(newRefFilterMatcher("HEAD", true, true));
		expected.add(
				newRefFilterMatcher("refs/**/${git_branch}", true, false));
		expected.add(newRefFilterMatcher("refs/heads/**", true, false));
		expected.add(newRefFilterMatcher("refs/remotes/**", true, false));
		expected.add(newRefFilterMatcher("refs/tags/**", true, false));
		expected.add(newRefFilterMatcher("Mock", false, false));
		expected.add(newRefFilterMatcher("test", false, true));
		expected.add(newRefFilterMatcher("filter", false, false));

		refFilterHelper.restoreLastSelectionState(testSet);
		assertThat("Expected different filters", testSet,
				containsInAnyOrder(expected));
	}

	@Test
	public void testSetFilters() throws Exception {
		Set<RefFilter> testSet = new HashSet<>();
		testSet.add(newRefFilter("HEAD", true, false));
		testSet.add(newRefFilter("refs/**/${git_branch}", true,
				false));
		testSet.add(newRefFilter("refs/heads/**", true, true));
		testSet.add(newRefFilter("refs/remotes/**", true, false));
		testSet.add(newRefFilter("refs/tags/**", true, false));
		testSet.add(newRefFilter("Mock", false, false));
		testSet.add(newRefFilter("test", false, false));
		testSet.add(newRefFilter("filter", false, true));

		refFilterHelper.setRefFilters(testSet);

		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForConfiguredFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("Mock", "test", "filter"))));

		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForSelectedFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("refs/heads/**", "filter"))));

		verify(prefStore).save();
	}

	@Test
	public void testSaveSelectionStateAsLastSelectionState() throws Exception {
		Set<RefFilter> testSet = new HashSet<>();
		testSet.add(newRefFilter("HEAD", true, false));
		testSet.add(newRefFilter("refs/**/${git_branch}", true,
				false));
		testSet.add(newRefFilter("refs/heads/**", true, true));
		testSet.add(newRefFilter("refs/remotes/**", true, false));
		testSet.add(newRefFilter("refs/tags/**", true, false));
		testSet.add(newRefFilter("Mock", false, false));
		testSet.add(newRefFilter("test", false, false));
		testSet.add(newRefFilter("filter", false, true));

		refFilterHelper.saveSelectionStateAsLastSelectionState(testSet);

		verify(prefStore).setValue(
				eq(getRepoSpecificPrefKeyForLastSelectedFilters()),
				argThat(refFilterConfigStringOf(
						containsInAnyOrder("refs/heads/**", "filter"))));

		verify(prefStore).save();
	}

	@Test
	public void testResetLastSelectionStateToDefault() throws Exception {
		refFilterHelper.resetLastSelectionStateToDefault();

		verify(prefStore).setToDefault(
				eq(getRepoSpecificPrefKeyForLastSelectedFilters()));

		verify(prefStore).save();
	}

	private Matcher<Ref> ref(Matcher<String> refName) {
		return new TypeSafeMatcher<Ref>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("A ref with the name ");
				refName.describeTo(description);
			}

			@Override
			protected boolean matchesSafely(Ref item) {
				return refName.matches(item.getName());
			}
		};
	}

	private void checkMatchingRefsForSelectedRefFilters(
			String selectedRefConfig, List<Matcher<? super Ref>> expected)
			throws Exception {
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForSelectedFilters())))
						.thenReturn(selectedRefConfig);
		when(prefStore
				.getString(eq(getRepoSpecificPrefKeyForConfiguredFilters())))
						.thenReturn("");
		Set<Ref> result = refFilterHelper
				.getMatchingRefsForSelectedRefFilters();

		assertThat("Expected different refs for config: " + selectedRefConfig,
				result, containsInAnyOrder(expected));
	}

	@Test
	public void testGetMatchingRefsForSelectedRefFilters()
			throws Exception {
		checkMatchingRefsForSelectedRefFilters("HEAD",
				Arrays.asList(ref(equalTo("HEAD"))));
		checkMatchingRefsForSelectedRefFilters("refs/**/${git_branch}",
				Arrays.asList(ref(equalTo("refs/heads/master")),
						ref(equalTo("refs/remotes/origin/master"))));
		checkMatchingRefsForSelectedRefFilters("refs/heads/**",
				Arrays.asList(ref(equalTo("refs/heads/master")),
						ref(equalTo("refs/heads/stable"))));
		checkMatchingRefsForSelectedRefFilters("refs/remotes/**",
				Arrays.asList(ref(equalTo("refs/remotes/origin/master"))));
		checkMatchingRefsForSelectedRefFilters("refs/tags/**",
				Arrays.asList(ref(equalTo("refs/tags/TestTag"))));
	}

	@Test
	public void testSelectOnlyHEAD() throws Exception {
		Set<RefFilter> testSet = new HashSet<>();
		testSet.add(newRefFilter("HEAD", true, false));
		testSet.add(newRefFilter("refs/**/${git_branch}", true,
				false));
		testSet.add(newRefFilter("refs/heads/**", true, true));
		testSet.add(newRefFilter("refs/remotes/**", true, false));
		testSet.add(newRefFilter("refs/tags/**", true, false));
		testSet.add(newRefFilter("Mock", false, false));
		testSet.add(newRefFilter("test", false, false));
		testSet.add(newRefFilter("filter", false, true));

		List<Matcher<? super RefFilter>> expected = new ArrayList<>();
		expected.add(newRefFilterMatcher("HEAD", true, true));
		expected.add(
				newRefFilterMatcher("refs/**/${git_branch}", true, false));
		expected.add(newRefFilterMatcher("refs/heads/**", true, false));
		expected.add(newRefFilterMatcher("refs/remotes/**", true, false));
		expected.add(newRefFilterMatcher("refs/tags/**", true, false));
		expected.add(newRefFilterMatcher("Mock", false, false));
		expected.add(newRefFilterMatcher("test", false, false));
		expected.add(newRefFilterMatcher("filter", false, false));

		refFilterHelper.selectOnlyHEAD(testSet);
		assertThat("Expected different filters", testSet,
				containsInAnyOrder(expected));
	}

	@Test
	public void testIsOnlyHEADSelected() throws Exception {
		Set<RefFilter> testSet1 = new HashSet<>();
		testSet1.add(newRefFilter("HEAD", true, false));
		testSet1.add(newRefFilter("refs/**/${git_branch}",
				true, false));
		testSet1.add(newRefFilter("refs/heads/**", true, true));
		testSet1.add(
				newRefFilter("refs/remotes/**", true, false));
		testSet1.add(newRefFilter("refs/tags/**", true, false));
		testSet1.add(newRefFilter("Mock", false, false));
		testSet1.add(newRefFilter("test", false, false));
		testSet1.add(newRefFilter("filter", false, true));

		assertFalse("Not only head selected",
				refFilterHelper.isOnlyHEADSelected(testSet1));

		Set<RefFilter> testSet2 = new HashSet<>();
		testSet2.add(newRefFilter("HEAD", true, true));
		testSet2.add(newRefFilter("refs/**/${git_branch}",
				true, false));
		testSet2.add(newRefFilter("refs/heads/**", true, true));
		testSet2.add(
				newRefFilter("refs/remotes/**", true, false));
		testSet2.add(newRefFilter("refs/tags/**", true, false));
		testSet2.add(newRefFilter("Mock", false, false));
		testSet2.add(newRefFilter("test", false, false));
		testSet2.add(newRefFilter("filter", false, true));

		assertFalse("Not only head selected",
				refFilterHelper.isOnlyHEADSelected(testSet2));

		Set<RefFilter> testSet3 = new HashSet<>();
		testSet3.add(newRefFilter("HEAD", true, true));
		testSet3.add(newRefFilter("refs/**/${git_branch}",
				true, false));
		testSet3.add(newRefFilter("refs/heads/**", true, false));
		testSet3.add(
				newRefFilter("refs/remotes/**", true, false));
		testSet3.add(newRefFilter("refs/tags/**", true, false));
		testSet3.add(newRefFilter("Mock", false, false));
		testSet3.add(newRefFilter("test", false, false));
		testSet3.add(newRefFilter("filter", false, false));

		assertTrue("Only head selected!",
				refFilterHelper.isOnlyHEADSelected(testSet3));

	}

	@Test
	public void testSelectOnlyCurrentBranch() throws Exception {
		Set<RefFilter> testSet = new HashSet<>();
		testSet.add(newRefFilter("HEAD", true, false));
		testSet.add(newRefFilter("refs/**/${git_branch}", true,
				false));
		testSet.add(newRefFilter("refs/heads/**", true, true));
		testSet.add(newRefFilter("refs/remotes/**", true, false));
		testSet.add(newRefFilter("refs/tags/**", true, false));
		testSet.add(newRefFilter("Mock", false, false));
		testSet.add(newRefFilter("test", false, false));
		testSet.add(newRefFilter("filter", false, true));

		List<Matcher<? super RefFilter>> expected = new ArrayList<>();
		expected.add(newRefFilterMatcher("HEAD", true, false));
		expected.add(
				newRefFilterMatcher("refs/**/${git_branch}", true, true));
		expected.add(newRefFilterMatcher("refs/heads/**", true, false));
		expected.add(newRefFilterMatcher("refs/remotes/**", true, false));
		expected.add(newRefFilterMatcher("refs/tags/**", true, false));
		expected.add(newRefFilterMatcher("Mock", false, false));
		expected.add(newRefFilterMatcher("test", false, false));
		expected.add(newRefFilterMatcher("filter", false, false));

		refFilterHelper.selectOnlyCurrentBranch(testSet);
		assertThat("Expected different filters", testSet,
				containsInAnyOrder(expected));
	}

	@Test
	public void testIsOnlyCurrentBranchSelected() throws Exception {
		Set<RefFilter> testSet1 = new HashSet<>();
		testSet1.add(newRefFilter("HEAD", true, false));
		testSet1.add(newRefFilter("refs/**/${git_branch}",
				true, false));
		testSet1.add(newRefFilter("refs/heads/**", true, true));
		testSet1.add(
				newRefFilter("refs/remotes/**", true, false));
		testSet1.add(newRefFilter("refs/tags/**", true, false));
		testSet1.add(newRefFilter("Mock", false, false));
		testSet1.add(newRefFilter("test", false, false));
		testSet1.add(newRefFilter("filter", false, true));

		assertFalse("Not only current branch selected",
				refFilterHelper.isOnlyCurrentBranchSelected(testSet1));

		Set<RefFilter> testSet2 = new HashSet<>();
		testSet2.add(newRefFilter("HEAD", true, false));
		testSet2.add(newRefFilter("refs/**/${git_branch}",
				true, true));
		testSet2.add(newRefFilter("refs/heads/**", true, true));
		testSet2.add(
				newRefFilter("refs/remotes/**", true, false));
		testSet2.add(newRefFilter("refs/tags/**", true, false));
		testSet2.add(newRefFilter("Mock", false, false));
		testSet2.add(newRefFilter("test", false, false));
		testSet2.add(newRefFilter("filter", false, true));

		assertFalse("Not only current branch selected",
				refFilterHelper.isOnlyCurrentBranchSelected(testSet2));

		Set<RefFilter> testSet3 = new HashSet<>();
		testSet3.add(newRefFilter("HEAD", true, false));
		testSet3.add(newRefFilter("refs/**/${git_branch}",
				true, true));
		testSet3.add(newRefFilter("refs/heads/**", true, false));
		testSet3.add(
				newRefFilter("refs/remotes/**", true, false));
		testSet3.add(newRefFilter("refs/tags/**", true, false));
		testSet3.add(newRefFilter("Mock", false, false));
		testSet3.add(newRefFilter("test", false, false));
		testSet3.add(newRefFilter("filter", false, false));

		assertTrue("Only current branch selected!",
				refFilterHelper.isOnlyCurrentBranchSelected(testSet3));

	}

	@Test
	public void testSelectExactlyAllBranchesAndTags() throws Exception {
		Set<RefFilter> testSet = new HashSet<>();
		testSet.add(newRefFilter("HEAD", true, false));
		testSet.add(newRefFilter("refs/**/${git_branch}", true,
				false));
		testSet.add(newRefFilter("refs/heads/**", true, true));
		testSet.add(newRefFilter("refs/remotes/**", true, false));
		testSet.add(newRefFilter("refs/tags/**", true, false));
		testSet.add(newRefFilter("Mock", false, false));
		testSet.add(newRefFilter("test", false, false));
		testSet.add(newRefFilter("filter", false, true));

		List<Matcher<? super RefFilter>> expected = new ArrayList<>();
		expected.add(newRefFilterMatcher("HEAD", true, true));
		expected.add(
				newRefFilterMatcher("refs/**/${git_branch}", true, false));
		expected.add(newRefFilterMatcher("refs/heads/**", true, true));
		expected.add(newRefFilterMatcher("refs/remotes/**", true, true));
		expected.add(newRefFilterMatcher("refs/tags/**", true, true));
		expected.add(newRefFilterMatcher("Mock", false, false));
		expected.add(newRefFilterMatcher("test", false, false));
		expected.add(newRefFilterMatcher("filter", false, false));

		refFilterHelper.selectExactlyAllBranchesAndTags(testSet);
		assertThat("Expected different filters", testSet,
				containsInAnyOrder(expected));
	}

	@Test
	public void testIsExactlyAllBranchesAndTagsSelected() throws Exception {
		Set<RefFilter> testSet1 = new HashSet<>();
		testSet1.add(newRefFilter("HEAD", true, false));
		testSet1.add(newRefFilter("refs/**/${git_branch}",
				true, false));
		testSet1.add(newRefFilter("refs/heads/**", true, true));
		testSet1.add(
				newRefFilter("refs/remotes/**", true, false));
		testSet1.add(newRefFilter("refs/tags/**", true, false));
		testSet1.add(newRefFilter("Mock", false, false));
		testSet1.add(newRefFilter("test", false, false));
		testSet1.add(newRefFilter("filter", false, true));

		assertFalse("Not only current branch selected",
				refFilterHelper.isExactlyAllBranchesAndTagsSelected(testSet1));

		Set<RefFilter> testSet2 = new HashSet<>();
		testSet2.add(newRefFilter("HEAD", true, true));
		testSet2.add(newRefFilter("refs/**/${git_branch}",
				true, false));
		testSet2.add(newRefFilter("refs/heads/**", true, true));
		testSet2.add(newRefFilter("refs/remotes/**", true, true));
		testSet2.add(newRefFilter("refs/tags/**", true, true));
		testSet2.add(newRefFilter("Mock", false, false));
		testSet2.add(newRefFilter("test", false, false));
		testSet2.add(newRefFilter("filter", false, true));

		assertFalse("Not only current branch selected",
				refFilterHelper.isExactlyAllBranchesAndTagsSelected(testSet2));

		Set<RefFilter> testSet3 = new HashSet<>();
		testSet3.add(newRefFilter("HEAD", true, true));
		testSet3.add(newRefFilter("refs/**/${git_branch}",
				true, false));
		testSet3.add(newRefFilter("refs/heads/**", true, true));
		testSet3.add(newRefFilter("refs/remotes/**", true, true));
		testSet3.add(newRefFilter("refs/tags/**", true, true));
		testSet3.add(newRefFilter("Mock", false, false));
		testSet3.add(newRefFilter("test", false, false));
		testSet3.add(newRefFilter("filter", false, false));

		assertTrue("Only current branch selected!",
				refFilterHelper.isExactlyAllBranchesAndTagsSelected(testSet3));

	}

	@Test
	public void testGetDefaults() throws Exception {
		when(prefStore.getDefaultString(
				eq(UIPreferences.RESOURCEHISTORY_REF_FILTERS)))
						.thenReturn("Mock:test:filter");
		when(prefStore.getDefaultString(
				eq(UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS)))
						.thenReturn("HEAD:test");
		Set<RefFilter> result = refFilterHelper.getDefaults();

		List<Matcher<? super RefFilter>> expected = new ArrayList<>();
		expected.add(newRefFilterMatcher("HEAD", true, true));
		expected.add(
				newRefFilterMatcher("refs/**/${git_branch}", true, false));
		expected.add(newRefFilterMatcher("refs/heads/**", true, false));
		expected.add(newRefFilterMatcher("refs/remotes/**", true, false));
		expected.add(newRefFilterMatcher("refs/tags/**", true, false));
		expected.add(newRefFilterMatcher("Mock", false, false));
		expected.add(newRefFilterMatcher("test", false, true));
		expected.add(newRefFilterMatcher("filter", false, false));

		assertThat("Expected different filters", result,
				containsInAnyOrder(expected));
	}

	private Matcher<String> refFilterConfigStringOf(
			Matcher<Iterable<? extends String>> items) {
		return new RefFilterConfigStringMatcher(items);
	}

	private static class RefFilterConfigStringMatcher
			extends TypeSafeMatcher<String> {

		private final Matcher<Iterable<? extends String>> itemsMatcher;

		public RefFilterConfigStringMatcher(
				Matcher<Iterable<? extends String>> itemsMatcher) {
			this.itemsMatcher = itemsMatcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("A ref filter config string containing ");
			itemsMatcher.describeTo(description);
		}

		@Override
		protected boolean matchesSafely(String item) {
			return itemsMatcher.matches(Arrays.asList(item.split(":")));
		}

	}
}

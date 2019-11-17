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
package org.eclipse.egit.ui.internal.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.tools.ant.types.selectors.TokenizedPath;
import org.apache.tools.ant.types.selectors.TokenizedPattern;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;

/**
 * A class for handling the config reading, writing and parsing for the ref
 * filters as well as matching refs against a filter.
 */
public class RefFilterHelper {

	private static final String ANY = "**"; //$NON-NLS-1$

	private static final String REF_SEPARATOR = ":"; //$NON-NLS-1$

	private static final String MACRO_CURRENT_BRANCH = "[CURRENT-BRANCH]"; //$NON-NLS-1$

	private static final String DEFAULT_SELECTED_REFS = Constants.HEAD;

	private static final String DEFAULT_SELECTED_REFS_ALL_BRANCHES =
			Constants.HEAD + REF_SEPARATOR
			+ Constants.R_HEADS + ANY + REF_SEPARATOR
			+ Constants.R_REMOTES + ANY + REF_SEPARATOR
			+ Constants.R_TAGS + ANY;

	private final IPreferenceStore store;

	@NonNull
	private final Repository repository;

	private List<RefFilter> preconfiguredFilters;

	private List<RefFilter> filtersForHEAD;
	private List<RefFilter> filtersForCurrentBranch;
	private List<RefFilter> filtersForAllBranchesAndTags;

	private Map<String, Function<Repository, String>> macros;

	private static @NonNull IPreferenceStore checkNull(IPreferenceStore store) {
		if (store == null) {
			throw new NullPointerException("Preference store is null."); //$NON-NLS-1$
		}
		return store;
	}

	/**
	 * Create a new RefFilterHelper for the given repository using the
	 * preference store provided by the activator of this ui plugin.
	 *
	 * @param repository
	 *            The repository to create the helper for; must not be null
	 */
	public RefFilterHelper(@NonNull Repository repository) {
		this(repository,
				checkNull(Activator.getDefault().getPreferenceStore()));
	}

	/**
	 * Create a new RefFilterHelper for the given repository using the given
	 * preference store.
	 *
	 * @param repository
	 *            The repository to create the helper for; must not be null
	 * @param store
	 *            The preference store to use; must not be null
	 */
	public RefFilterHelper(@NonNull Repository repository,
			@NonNull IPreferenceStore store) {
		this.repository = repository;
		this.store = store;
		setupPreconfiguredFilters();
		setupMacros();
		// Just always init the repo defaults as we don't know if this repo had
		// a helper before.
		initDefaultsForRepo();
	}

	private RefFilter newPreConfFilter(String filter) {
		return new RefFilter(filter, true);
	}

	private RefFilter newPreConfPrefixFilter(String prefix) {
		return newPreConfFilter(prefix + "**"); //$NON-NLS-1$
	}

	private void setupPreconfiguredFilters() {
		preconfiguredFilters = new ArrayList<>();
		filtersForHEAD = new ArrayList<>();
		filtersForCurrentBranch = new ArrayList<>();
		filtersForAllBranchesAndTags = new ArrayList<>();

		RefFilter head = newPreConfFilter(Constants.HEAD);
		preconfiguredFilters.add(head);
		filtersForHEAD.add(head);
		filtersForAllBranchesAndTags.add(head);

		RefFilter currentBranch = newPreConfFilter(
				Constants.R_REFS + "**/" + MACRO_CURRENT_BRANCH); //$NON-NLS-1$
		preconfiguredFilters.add(currentBranch);
		filtersForCurrentBranch.add(currentBranch);

		RefFilter branches = newPreConfPrefixFilter(Constants.R_HEADS);
		preconfiguredFilters.add(branches);
		filtersForAllBranchesAndTags.add(branches);

		RefFilter remoteBranches = newPreConfPrefixFilter(Constants.R_REMOTES);
		preconfiguredFilters.add(remoteBranches);
		filtersForAllBranchesAndTags.add(remoteBranches);

		RefFilter tags = newPreConfPrefixFilter(Constants.R_TAGS);
		preconfiguredFilters.add(tags);
		filtersForAllBranchesAndTags.add(tags);
	}

	private void setupMacros() {
		macros = new LinkedHashMap<>();
		macros.put(MACRO_CURRENT_BRANCH, repo -> {
			try {
				return repo.getBranch();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
			return ""; //$NON-NLS-1$
		});
	}

	private void setDefaultSelectionBasedOnShowAllBranches() {
		String currentDefault = store.getDefaultString(
				UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS);

		if (currentDefault != DEFAULT_SELECTED_REFS
				&& currentDefault != DEFAULT_SELECTED_REFS_ALL_BRANCHES) {
			// The default was changed elsewhere.
			// Probably a plugin customization.
			// Don't overwrite it.
			return;
		}

		boolean showAll = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES);

		if (showAll) {
			store.setDefault(UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS,
					DEFAULT_SELECTED_REFS_ALL_BRANCHES);
		} else {
			store.setDefault(UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS,
					DEFAULT_SELECTED_REFS);
		}
	}

	private void initDefaultForRepo(String preferenceName) {
		String repoSpecificPrefName = Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(this.repository,
						preferenceName);

		store.setDefault(repoSpecificPrefName,
				store.getDefaultString(preferenceName));
	}

	/**
	 * Init the default of the repo specific pref from global default. This
	 * needs to happen after each eclipse startup (as the default is not
	 * persisted) for each repo.
	 */
	private void initDefaultsForRepo() {
		setDefaultSelectionBasedOnShowAllBranches();
		initDefaultForRepo(UIPreferences.RESOURCEHISTORY_REF_FILTERS);
		initDefaultForRepo(UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS);
		initDefaultForRepo(
				UIPreferences.RESOURCEHISTORY_LAST_SELECTED_REF_FILTERS);
	}

	/**
	 * Get a preference string from the preference store. This should return the
	 * repository specific preference string if applicable.
	 *
	 * @param preferenceName
	 *            the name of the preference
	 * @return the string-valued preference
	 */
	protected String getPreferenceString(String preferenceName) {
		String repoSpecificPrefName = Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(this.repository,
						preferenceName);

		return store.getString(repoSpecificPrefName);
	}

	private List<String> getFiltersFromPref(String preferenceName) {
		String refFiltersString = getPreferenceString(preferenceName);
		String[] filters = refFiltersString.split(REF_SEPARATOR);

		return Arrays.asList(filters);
	}

	private void savePreferencStoreIfNeeded() {
		if (store.needsSaving()
				&& store instanceof IPersistentPreferenceStore) {
			try {
				((IPersistentPreferenceStore) store).save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
		}
	}

	private void setFiltersInPref(String preferenceName, List<String> filters,
			boolean save) {
		String repoSpecificPrefName = Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(this.repository,
						preferenceName);
		String refFiltersString = String.join(REF_SEPARATOR, filters);
		store.setValue(repoSpecificPrefName, refFiltersString);

		if (save) {
			savePreferencStoreIfNeeded();
		}
	}

	/**
	 * Get the configured ref filters from the preference store.
	 *
	 * @return A list of all configured ref filter strings
	 */
	public List<String> getConfiguredFilters() {
		return getFiltersFromPref(UIPreferences.RESOURCEHISTORY_REF_FILTERS);
	}

	/**
	 * Set the configured ref filters in the preference store.
	 *
	 * @param filters
	 *            The list of configured ref filter strings to set
	 * @param save
	 *            Whether to save the preference store if necessary
	 */
	public void setConfiguredFilters(List<String> filters, boolean save) {
		setFiltersInPref(UIPreferences.RESOURCEHISTORY_REF_FILTERS, filters,
				save);
	}

	/**
	 * Get the selected ref filters from the preference store.
	 *
	 * @return A list of all selected ref filter strings
	 */
	public List<String> getSelectedFilters() {
		return getFiltersFromPref(
				UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS);
	}

	/**
	 * Set the selected ref filters in the preference store.
	 *
	 * @param filters
	 *            The list of selected ref filter strings to set
	 * @param save
	 *            Whether to save the preference store if necessary
	 */
	public void setSelectedFilters(List<String> filters, boolean save) {
		setFiltersInPref(UIPreferences.RESOURCEHISTORY_SELECTED_REF_FILTERS,
				filters, save);
	}

	/**
	 * Get the last selected ref filters from the preference store.
	 *
	 * @return A list of the last selected ref filter strings
	 */
	public List<String> getLastSelectedFilters() {
		return getFiltersFromPref(
				UIPreferences.RESOURCEHISTORY_LAST_SELECTED_REF_FILTERS);
	}

	/**
	 * Set the last selected ref filters in the preference store.
	 *
	 * @param filters
	 *            The list of last selected ref filter strings to set
	 * @param save
	 *            Whether to save the preference store if necessary
	 */
	public void setLastSelectedFilters(List<String> filters, boolean save) {
		setFiltersInPref(
				UIPreferences.RESOURCEHISTORY_LAST_SELECTED_REF_FILTERS,
				filters, save);
	}

	private void addPreconfiguredFilters(Map<String, RefFilter> filters) {
		for (RefFilter filter : preconfiguredFilters) {
			// Don't use the existing object because selection states would be
			// persisted immediately without calling setRefFilters.
			filters.put(filter.getFilterString(), new RefFilter(filter));
		}
	}

	/**
	 * @return the set of all ref filters
	 */
	public Set<RefFilter> getRefFilters() {
		Map<String, RefFilter> filters = new LinkedHashMap<>();
		addPreconfiguredFilters(filters);

		for (String filter : getConfiguredFilters()) {
			if (filter == null || filter.isEmpty()) {
				continue;
			}
			filters.put(filter, new RefFilter(filter, false));
		}

		for (String filter : getSelectedFilters()) {
			if (filter == null || filter.isEmpty()) {
				continue;
			}
			// A user could change the pref files manually
			// Therefore we need to make sure all selected filters are also
			// available.
			// So we add them to the set if they are not already there
			filters.putIfAbsent(filter, new RefFilter(filter, false));
			filters.get(filter).setSelected(true);
		}
		return new LinkedHashSet<>(filters.values());
	}

	/**
	 * Restore the last selection state.
	 * @param filters The set of filters to restore the state for.
	 */
	public void restoreLastSelectionState(Set<RefFilter> filters) {
		for(RefFilter filter : filters) {
			filter.setSelected(getLastSelectedFilters()
					.contains(filter.getFilterString()));
		}
	}

	/**
	 * Set the given rev filters in the preference store.
	 * <p>
	 * This overrides the selected and the configured filters in the preference
	 * store.
	 * <p>
	 *
	 * @param filters
	 *            The set of filters to save.
	 */
	public void setRefFilters(Set<RefFilter> filters) {
		List<String> selected = filters.stream().filter(RefFilter::isSelected)
				.map(RefFilter::getFilterString).collect(Collectors.toList());
		setSelectedFilters(selected, false);

		List<String> configured = filters.stream()
				.filter(f -> !f.isPreconfigured())
				.map(RefFilter::getFilterString).collect(Collectors.toList());
		setConfiguredFilters(configured, false);

		savePreferencStoreIfNeeded();
	}

	/**
	 * Save the selection state of the given filter set as the last selection
	 * state.
	 *
	 * @param filters
	 *            The filters to get the selection state from
	 */
	public void saveSelectionStateAsLastSelectionState(Set<RefFilter> filters) {
		List<String> selected = new ArrayList<>();
		for(RefFilter filter : filters) {
			if (filter.isSelected()) {
				selected.add(filter.getFilterString());
			}
		}
		setLastSelectedFilters(selected, true);
	}

	/**
	 * Reset the last selection state to the default.
	 */
	public void resetLastSelectionStateToDefault() {
		String repoSpecificPrefName = Activator.getDefault().getRepositoryUtil()
				.getRepositorySpecificPreferenceKey(this.repository,
						UIPreferences.RESOURCEHISTORY_LAST_SELECTED_REF_FILTERS);
		store.setToDefault(repoSpecificPrefName);
		savePreferencStoreIfNeeded();
	}

	/**
	 * Get all matching refs in the given repository for the currently selected
	 * ref filters.
	 *
	 * @return All matching refs from the repo
	 * @throws IOException
	 *             the reference space cannot be accessed.
	 */
	public Set<Ref> getMatchingRefsForSelectedRefFilters()
			throws IOException {
		RefDatabase db = this.repository.getRefDatabase();
		Set<Ref> result = new LinkedHashSet<>();
		Set<RefFilter> selectedFilters = getRefFilters().stream()
				.filter(f -> f.isSelected())
				.collect(Collectors.toCollection(LinkedHashSet::new));

		for (Ref ref : db.getRefs()) {
			TokenizedPath refPath = new TokenizedPath(
					ref.getName().replace('/', File.separatorChar));
			for (RefFilter filter : selectedFilters) {
				if (filter.matches(refPath)) {
					result.add(ref);
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Select only the HEAD preconfigured ref filter.
	 * <p>
	 * This will modify objects in the given list.
	 * </p>
	 *
	 * @param filters
	 *            The filters to change the selection of
	 */
	public void selectOnlyHEAD(Set<RefFilter> filters) {
		for (RefFilter filter : filters) {
			filter.setSelected(filtersForHEAD.contains(filter));
		}
	}

	/**
	 * Check whether only the HEAD preconfigured ref filter is selected.
	 *
	 * @param filters
	 *            The filters to check
	 * @return Whether exactly HEAD is selected
	 */
	public boolean isOnlyHEADSelected(Set<RefFilter> filters) {
		for (RefFilter filter : filters) {
			if (filter.isSelected()) {
				if (!filtersForHEAD.contains(filter)) {
					return false;
				}
			} else {
				if (filtersForHEAD.contains(filter)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Select only the preconfigured ref filter for the current branch (local
	 * and remote).
	 * <p>
	 * This will modify objects in the given list.
	 * </p>
	 *
	 * @param filters
	 *            The filters to change the selection of
	 */
	public void selectOnlyCurrentBranch(Set<RefFilter> filters) {
		for (RefFilter filter : filters) {
			filter.setSelected(filtersForCurrentBranch.contains(filter));
		}
	}

	/**
	 * Check whether only the preconfigured ref filter for the current branch
	 * (local and remote) is selected.
	 *
	 * @param filters
	 *            The filters to check
	 * @return Whether exactly the current branch is selected
	 */
	public boolean isOnlyCurrentBranchSelected(Set<RefFilter> filters) {
		for (RefFilter filter : filters) {
			if (filter.isSelected()) {
				if (!filtersForCurrentBranch.contains(filter)) {
					return false;
				}
			} else {
				if (filtersForCurrentBranch.contains(filter)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Select exactly the preconfigured ref filters, which represent all
	 * branches and tags.
	 * <p>
	 * This will modify objects in the given list.
	 * </p>
	 *
	 * @param filters
	 *            The filters to change the selection of.
	 */
	public void selectExactlyAllBranchesAndTags(Set<RefFilter> filters) {
		for (RefFilter filter : filters) {
			filter.setSelected(filtersForAllBranchesAndTags.contains(filter));
		}
	}

	/**
	 * Check whether exactly the preconfigured ref filters which represent all
	 * branches and tags.
	 *
	 * @param filters
	 *            The filters to check
	 * @return Whether exactly all branches and tags are selected
	 */
	public boolean isExactlyAllBranchesAndTagsSelected(Set<RefFilter> filters) {
		for (RefFilter filter : filters) {
			if (filter.isSelected()) {
				if (!filtersForAllBranchesAndTags.contains(filter))
					return false;
			} else {
				if (filtersForAllBranchesAndTags.contains(filter))
					return false;
			}
		}
		return true;
	}

	/**
	 * Get the default ref filters
	 *
	 * @return a set of the default ref filters.
	 */
	public Set<RefFilter> getDefaults() {
		setDefaultSelectionBasedOnShowAllBranches();
		RefFilterHelper defaultsHelper = new RefFilterHelper(this.repository) {
			@Override
			protected String getPreferenceString(String preferenceName) {
				return store.getDefaultString(preferenceName);
			}
		};
		return defaultsHelper.getRefFilters();
	}

	/**
	 * Representation of a ref filter
	 */
	public class RefFilter {

		private final boolean preconfigured;

		private String filterString;
		private TokenizedPattern filterPattern;

		private TokenizedPattern expandedFilterPattern;

		private boolean selected;

		/**
		 * Create a ref filter as the copy of an original.
		 * <p>
		 * The new filter has the same values as the original, but future
		 * changes on either of the original or this new filter won't affect the
		 * other one.
		 * </p>
		 *
		 * @param original
		 */
		public RefFilter(RefFilter original) {
			this(original.getFilterString(), original.isPreconfigured());
			this.selected = original.isSelected();
		}

		/**
		 * Create a new ref filter
		 *
		 * @param filterString
		 *            The filter string for the new ref filter; must not be
		 *            null; must not be empty.
		 *
		 * @throws IllegalArgumentException
		 *             if the filter string is null or empty
		 */
		public RefFilter(String filterString) {
			this(filterString, false);
		}

		/**
		 * Create a new ref filter
		 *
		 * @param filterString
		 *            The filter string for the new ref filter; must not be
		 *            null; must not be empty.
		 * @param isPreconfigured
		 *            Whether the new Filter is a preconfigured one
		 *
		 * @throws IllegalArgumentException
		 *             if the filter string is null or empty
		 */
		private RefFilter(String filterString, boolean isPreconfigured) {
			if (filterString == null || filterString.isEmpty()) {
				throw new IllegalArgumentException(
						"Filter string is null or empty."); //$NON-NLS-1$
			}
			this.filterString = filterString;
			this.filterPattern = createPattern(filterString);
			this.preconfigured = isPreconfigured;
		}

		/**
		 * @return whether this is a preconfigured filter
		 */
		public boolean isPreconfigured() {
			return preconfigured;
		}

		private TokenizedPattern patternWithExpandedMacros() {
			if (expandedFilterPattern == null) {
				expandedFilterPattern = expandMacros();
			}
			return expandedFilterPattern;
		}

		private TokenizedPattern expandMacros() {
			TokenizedPattern currentPattern = filterPattern;
			for(Map.Entry<String, Function<Repository, String>> macro : macros.entrySet()) {
				String macroString = macro.getKey();
				if (currentPattern.containsPattern(macroString)) {
					String replacingString = macro.getValue().apply(repository);
					String newString = currentPattern.getPattern()
							.replace(macroString, replacingString);
					currentPattern = createPattern(newString);
				}
			}
			return currentPattern;
		}

		private TokenizedPattern createPattern(String pattern) {
			return new TokenizedPattern(
					pattern.replace('/', File.separatorChar));
		}

		/**
		 * Tries to match the given ref against this filter.
		 *
		 * @param refPath
		 *            The path of the ref to match
		 * @return true if the ref path matches the pattern of this filter
		 */
		public boolean matches(TokenizedPath refPath) {
			return patternWithExpandedMacros().matchPath(refPath,
					true);
		}

		/**
		 * @return the filter string; cannot be null; cannot be empty
		 */
		public String getFilterString() {
			return this.filterString;
		}

		/**
		 * @param filterString
		 *            the filterString to set; must not be null; must not be
		 *            empty
		 * @throws IllegalArgumentException
		 *             if the filter string is null or empty
		 * @throws IllegalStateException
		 *             if this is a preconfigured filter
		 */
		public void setFilterString(String filterString) {
			if (filterString == null || filterString.isEmpty()) {
				throw new IllegalArgumentException(
						"Filter string is null or empty."); //$NON-NLS-1$
			}
			if (preconfigured) {
				throw new IllegalStateException(
						"Cannot change a preconfigured filter."); //$NON-NLS-1$
			}
			this.filterString = filterString;
			this.filterPattern = createPattern(filterString);
			this.expandedFilterPattern = null;
		}

		/**
		 * @return whether this filter is currently selected
		 */
		public boolean isSelected() {
			return selected;
		}

		/**
		 * @param selected
		 *            whether this filter is selected
		 */
		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		@Override
		public int hashCode() {
			return filterPattern.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof RefFilter)) {
				return false;
			}
			return filterPattern.equals(((RefFilter) obj).filterPattern);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("RefFilter ["); //$NON-NLS-1$
			if (filterPattern != null) {
				builder.append("pattern="); //$NON-NLS-1$
				builder.append(filterPattern);
				builder.append(", "); //$NON-NLS-1$
				if (expandedFilterPattern != null
						&& !expandedFilterPattern.equals(filterPattern)) {
					builder.append("expandedPattern="); //$NON-NLS-1$
					builder.append(expandedFilterPattern);
					builder.append(", "); //$NON-NLS-1$
				}
			}
			builder.append("preconfigured="); //$NON-NLS-1$
			builder.append(preconfigured);
			builder.append(", selected="); //$NON-NLS-1$
			builder.append(selected);
			builder.append("]"); //$NON-NLS-1$
			return builder.toString();
		}
	}
}

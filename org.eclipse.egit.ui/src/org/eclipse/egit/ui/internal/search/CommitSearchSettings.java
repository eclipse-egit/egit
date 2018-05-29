/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * Class to hold settings used to search commits.
 */
public class CommitSearchSettings {

	private static final String SEARCH_SECTION = "searchSection"; //$NON-NLS-1$

	private static final String TEXT_PATTERN = "textPattern"; //$NON-NLS-1$

	private static final String REGEX_SEARCH = "regexSearch"; //$NON-NLS-1$

	private static final String IGNORE_CASE = "ignoreCase"; //$NON-NLS-1$

	private static final String MATCH_AUTHOR = "matchAuthor"; //$NON-NLS-1$

	private static final String MATCH_COMMITTER = "matchCommitter"; //$NON-NLS-1$

	private static final String MATCH_MESSAGE = "matchMessage"; //$NON-NLS-1$

	private static final String MATCH_COMMIT = "matchCommit"; //$NON-NLS-1$

	private static final String MATCH_PARENTS = "matchParents"; //$NON-NLS-1$

	private static final String MATCH_TREE = "matchTree"; //$NON-NLS-1$

	private static final String ALL_BRANCHES = "allBranches"; //$NON-NLS-1$

	private static final String REPOSITORY_COUNT = "repositoryCount"; //$NON-NLS-1$

	private static final String REPOSITORY = "repository"; //$NON-NLS-1$

	/**
	 * Create search settings from dialog settings
	 *
	 * @param settings
	 * @return search settings
	 */
	public static CommitSearchSettings create(IDialogSettings settings) {
		IDialogSettings section = settings.getSection(SEARCH_SECTION);
		CommitSearchSettings searchSettings = new CommitSearchSettings();
		if (section != null) {
			searchSettings.setTextPattern(section.get(TEXT_PATTERN));
			searchSettings.setRegExSearch(section.getBoolean(REGEX_SEARCH));
			searchSettings.setCaseSensitive(!section.getBoolean(IGNORE_CASE));
			searchSettings.setMatchAuthor(section.getBoolean(MATCH_AUTHOR));
			searchSettings.setMatchCommitter(section
					.getBoolean(MATCH_COMMITTER));
			searchSettings.setMatchMessage(section.getBoolean(MATCH_MESSAGE));
			searchSettings.setMatchCommit(section.getBoolean(MATCH_COMMIT));
			searchSettings.setMatchParents(section.getBoolean(MATCH_PARENTS));
			searchSettings.setMatchTree(section.getBoolean(MATCH_TREE));
			searchSettings.setAllBranches(section.getBoolean(ALL_BRANCHES));
			try {
				int count = section.getInt(REPOSITORY_COUNT);
				for (int i = 0; i < count; i++)
					searchSettings.addRepository(section.get(REPOSITORY + i));
			} catch (NumberFormatException ignored) {
				// Ignored
			}
		}
		return searchSettings;
	}

	private boolean isMatchCommit = true;

	private boolean isMatchCommitter = true;

	private boolean isMatchAuthor = true;

	private boolean isMatchTree = true;

	private boolean isMatchMessage = true;

	private boolean isMatchParents = true;

	private boolean isCaseSensitive = false;

	private boolean isRegExSearch = false;

	private boolean isAllBranches = false;

	private String textPattern = null;

	private List<String> repositories = new LinkedList<>();

	/**
	 * Store search settings in dialog settings
	 *
	 * @param settings
	 */
	public void store(IDialogSettings settings) {
		IDialogSettings section = settings.getSection(SEARCH_SECTION);
		if (section == null)
			section = settings.addNewSection(SEARCH_SECTION);

		section.put(IGNORE_CASE, !isCaseSensitive);
		section.put(REGEX_SEARCH, isRegExSearch);
		section.put(TEXT_PATTERN, textPattern);
		section.put(MATCH_AUTHOR, isMatchAuthor);
		section.put(MATCH_COMMIT, isMatchCommit);
		section.put(MATCH_COMMITTER, isMatchCommitter);
		section.put(MATCH_MESSAGE, isMatchMessage);
		section.put(MATCH_PARENTS, isMatchParents);
		section.put(MATCH_TREE, isMatchTree);
		section.put(ALL_BRANCHES, isAllBranches);

		int count = 0;
		for (String repo : this.repositories) {
			section.put(REPOSITORY + count, repo);
			count++;
		}
		section.put(REPOSITORY_COUNT, this.repositories.size());
	}

	/** @param path */
	public void addRepository(String path) {
		this.repositories.add(path);
	}

	/** @return list of repository paths */
	public List<String> getRepositories() {
		return this.repositories;
	}

	/** @return isMatchCommit */
	public boolean isMatchCommit() {
		return this.isMatchCommit;
	}

	/** @param isMatchCommit */
	public void setMatchCommit(boolean isMatchCommit) {
		this.isMatchCommit = isMatchCommit;
	}

	/** @return isMatchCommitter */
	public boolean isMatchCommitter() {
		return this.isMatchCommitter;
	}

	/** @param isMatchCommitter */
	public void setMatchCommitter(boolean isMatchCommitter) {
		this.isMatchCommitter = isMatchCommitter;
	}

	/** @return isMatchAuthor */
	public boolean isMatchAuthor() {
		return this.isMatchAuthor;
	}

	/** @param isMatchAuthor */
	public void setMatchAuthor(boolean isMatchAuthor) {
		this.isMatchAuthor = isMatchAuthor;
	}

	/** @return isMatchTree */
	public boolean isMatchTree() {
		return this.isMatchTree;
	}

	/** @param isMatchTree */
	public void setMatchTree(boolean isMatchTree) {
		this.isMatchTree = isMatchTree;
	}

	/** @return isMatchMessage */
	public boolean isMatchMessage() {
		return this.isMatchMessage;
	}

	/** @param isMatchMessage */
	public void setMatchMessage(boolean isMatchMessage) {
		this.isMatchMessage = isMatchMessage;
	}

	/** @return isMatchParents */
	public boolean isMatchParents() {
		return this.isMatchParents;
	}

	/** @param isMatchParents */
	public void setMatchParents(boolean isMatchParents) {
		this.isMatchParents = isMatchParents;
	}

	/** @return isCaseSensitive */
	public boolean isCaseSensitive() {
		return this.isCaseSensitive;
	}

	/** @param isCaseSensitive */
	public void setCaseSensitive(boolean isCaseSensitive) {
		this.isCaseSensitive = isCaseSensitive;
	}

	/** @return isRegExSearch */
	public boolean isRegExSearch() {
		return this.isRegExSearch;
	}

	/** @param isRegExSearch */
	public void setRegExSearch(boolean isRegExSearch) {
		this.isRegExSearch = isRegExSearch;
	}

	/** @return textPattern */
	public String getTextPattern() {
		return this.textPattern;
	}

	/** @param textPattern */
	public void setTextPattern(String textPattern) {
		this.textPattern = textPattern;
	}

	/** @return isAllBranches */
	public boolean isAllBranches() {
		return this.isAllBranches;
	}

	/** @param isAllBranches */
	public void setAllBranches(boolean isAllBranches) {
		this.isAllBranches = isAllBranches;
	}

}

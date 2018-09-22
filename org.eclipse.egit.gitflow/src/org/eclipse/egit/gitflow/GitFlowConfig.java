/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow;

import static org.eclipse.egit.gitflow.GitFlowDefaults.DEVELOP;
import static org.eclipse.egit.gitflow.GitFlowDefaults.FEATURE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.HOTFIX_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.RELEASE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.VERSION_TAG;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.R_HEADS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Wrapper for JGit repository.
 *
 * @since 4.0
 */
public class GitFlowConfig {
	/** Key for .git/config */
	public static final String MASTER_KEY = "master"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String DEVELOP_KEY = "develop"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String HOTFIX_KEY = "hotfix"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String RELEASE_KEY = "release"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String FEATURE_KEY = "feature"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String VERSION_TAG_KEY = "versiontag"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String USER_SECTION = "user"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String BRANCH_SECTION = "branch"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String PREFIX_SECTION = "prefix"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String GITFLOW_SECTION = "gitflow"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String REMOTE_KEY = "remote"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String MERGE_KEY = "merge"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String FEATURE_START_FETCH_KEY = "fetch"; //$NON-NLS-1$

	/** Name of .git/config sub section. */
	public static final String FEATURE_START_SUBSECTION = "feature.start"; //$NON-NLS-1$

	private Repository repository;

	/**
	 * @param repository
	 */
	public GitFlowConfig(Repository repository) {
		Assert.isNotNull(repository);
		this.repository = repository;
	}

	/**
	 * @return git init done?
	 * @throws IOException
	 */
	public boolean isInitialized() throws IOException {
		StoredConfig config = repository.getConfig();
		Set<String> sections = config.getSections();
		return sections.contains(GITFLOW_SECTION);
	}

	/**
	 * @return Local user of this repository.
	 */
	public String getUser() {
		StoredConfig config = repository.getConfig();
		String userName = config.getString(USER_SECTION, null, "name"); //$NON-NLS-1$
		String email = config.getString(USER_SECTION, null, "email"); //$NON-NLS-1$
		return String.format("%s <%s>", userName, email); //$NON-NLS-1$
	}

	/**
	 * @return feature prefix configured for this repository.
	 */
	public String getFeaturePrefix() {
		return getPrefix(FEATURE_KEY, FEATURE_PREFIX);
	}

	/**
	 * @return release prefix configured for this repository.
	 */
	public String getReleasePrefix() {
		return getPrefix(RELEASE_KEY, RELEASE_PREFIX);
	}

	/**
	 * @return hotfix prefix configured for this repository.
	 */
	public String getHotfixPrefix() {
		return getPrefix(HOTFIX_KEY, HOTFIX_PREFIX);
	}

	/**
	 * @return version prefix configured for this repository, that is used in
	 *         tags.
	 */
	public String getVersionTagPrefix() {
		return getPrefix(VERSION_TAG_KEY, VERSION_TAG);
	}

	/**
	 * @return name of develop configured for this repository.
	 */
	public String getDevelop() {
		return getBranch(DEVELOP_KEY, DEVELOP);
	}

	/**
	 * @return full name of develop configured for this repository.
	 */
	public String getDevelopFull() {
		return R_HEADS + getDevelop();
	}

	/**
	 * @return name of master configured for this repository.
	 */
	public String getMaster() {
		return getBranch(MASTER_KEY, GitFlowDefaults.MASTER);
	}

	/**
	 * @param prefixName
	 * @param defaultPrefix
	 * @return value for key prefixName from .git/config or default
	 */
	public String getPrefix(String prefixName, String defaultPrefix) {
		StoredConfig config = repository.getConfig();
		String result = config.getString(GITFLOW_SECTION, PREFIX_SECTION,
				prefixName);
		return (result == null) ? defaultPrefix : result;
	}

	/**
	 * @param branch
	 * @param defaultBranch
	 * @return value for key branch from .git/config or default
	 */
	public String getBranch(String branch, String defaultBranch) {
		StoredConfig config = repository.getConfig();
		String result = config.getString(GITFLOW_SECTION, BRANCH_SECTION,
				branch);
		return (result == null) ? defaultBranch : result;
	}

	/**
	 * Set prefix in .git/config
	 *
	 * @param prefixName
	 * @param value
	 */
	public void setPrefix(String prefixName, String value) {
		StoredConfig config = repository.getConfig();
		config.setString(GITFLOW_SECTION, PREFIX_SECTION, prefixName, value);
	}

	/**
	 * Set branchName in .git/config
	 *
	 * @param branchName
	 * @param value
	 */
	public void setBranch(String branchName, String value) {
		StoredConfig config = repository.getConfig();
		config.setString(GITFLOW_SECTION, BRANCH_SECTION, branchName, value);
	}

	/**
	 * @param featureName
	 * @return full name of branch featureName
	 */
	public String getFullFeatureBranchName(String featureName) {
		return R_HEADS + getFeatureBranchName(featureName);
	}

	/**
	 * @param featureName
	 * @return name of branch featureName
	 */
	public String getFeatureBranchName(String featureName) {
		return getFeaturePrefix() + featureName;
	}

	/**
	 * @param hotfixName
	 * @return name of branch hotfixName
	 */
	public String getHotfixBranchName(String hotfixName) {
		return getHotfixPrefix() + hotfixName;
	}

	/**
	 * @param hotfixName
	 * @return full name of branch hotfixName
	 */
	public String getFullHotfixBranchName(String hotfixName) {
		return R_HEADS + getHotfixBranchName(hotfixName);
	}

	/**
	 * @param releaseName
	 * @return full name of branch releaseName
	 */
	public String getFullReleaseBranchName(String releaseName) {
		return R_HEADS + getReleaseBranchName(releaseName);
	}

	/**
	 * @param releaseName
	 * @return name of branch releaseName
	 */
	public String getReleaseBranchName(String releaseName) {
		return getReleasePrefix() + releaseName;
	}

	/**
	 * @return Configured origin.
	 */
	public RemoteConfig getDefaultRemoteConfig() {
		StoredConfig rc = repository.getConfig();
		RemoteConfig result;
		try {
			result = new RemoteConfig(rc, DEFAULT_REMOTE_NAME);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		return result;
	}

	/**
	 * @return Whether or not there is a default remote configured.
	 */
	public boolean hasDefaultRemote() {
		RemoteConfig config = getDefaultRemoteConfig();
		return !config.getURIs().isEmpty();
	}

	/**
	 * @param featureName
	 * @param value
	 * @throws IOException
	 */
	public void setRemote(String featureName, String value) throws IOException {
		setBranchValue(featureName, value, REMOTE_KEY);
	}

	/**
	 * @param featureName
	 * @param value
	 * @throws IOException
	 */
	public void setUpstreamBranchName(String featureName, String value) throws IOException {
		setBranchValue(featureName, value, MERGE_KEY);
	}

	/**
	 * @param featureName
	 * @return Upstream branch name
	 */
	public String getUpstreamBranchName(String featureName) {
		StoredConfig config = repository.getConfig();
		return config.getString(BRANCH_SECTION,
				getFeatureBranchName(featureName), MERGE_KEY);
	}

	private void setBranchValue(String featureName, String value,
			String mergeKey) throws IOException {
		StoredConfig config = repository.getConfig();
		config.setString(BRANCH_SECTION, featureName, mergeKey, value);
		config.save();
	}

	/**
	 * @param featureName
	 * @return remote tracking branch
	 */
	public String getRemoteName(String featureName) {
		StoredConfig config = repository.getConfig();
		return config.getString(BRANCH_SECTION,
				getFeatureBranchName(featureName), REMOTE_KEY);
	}

	/**
	 * @param isFetchOnFeatureStart
	 *            Whether or not to fetch from upstream before feature branch is
	 *            created.
	 * @throws IOException
	 *
	 * @since 5.2
	 */
	public void setFetchOnFeatureStart(boolean isFetchOnFeatureStart)
			throws IOException {
		StoredConfig config = repository.getConfig();
		config.setBoolean(GITFLOW_SECTION, FEATURE_START_SUBSECTION, FEATURE_START_FETCH_KEY,
				isFetchOnFeatureStart);
		config.save();
	}

	/**
	 * @return Whether or not the to fetch from upstream before feature branch
	 *         is created.
	 *
	 * @since 5.2
	 */
	public boolean isFetchOnFeatureStart() {
		StoredConfig config = repository.getConfig();
		return config.getBoolean(GITFLOW_SECTION, FEATURE_START_SUBSECTION,
				FEATURE_START_FETCH_KEY, false);
	}
}

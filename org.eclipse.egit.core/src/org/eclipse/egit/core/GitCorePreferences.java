/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Robin Rosenberg
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2015, Obeo
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

/** Preferences used by the core plugin. */
public class GitCorePreferences {
	/** */
	public static final String core_packedGitWindowSize =
		"core_packedGitWindowSize";  //$NON-NLS-1$
	/** */
	public static final String core_packedGitLimit =
		"core_packedGitLimit";  //$NON-NLS-1$
	/** */
	public static final String core_packedGitMMAP =
		"core_packedGitMMAP";  //$NON-NLS-1$
	/** */
	public static final String core_deltaBaseCacheLimit =
		"core_deltaBaseCacheLimit";  //$NON-NLS-1$
	/** */
	public static final String core_streamFileThreshold =
		"core_streamFileThreshold"; //$NON-NLS-1$
	/** */
	public static final String core_autoShareProjects =
		"core_autoShareProjects";  //$NON-NLS-1$
	/** */
	public static final String core_autoIgnoreDerivedResources =
		"core_autoIgnoreDerivedResources"; //$NON-NLS-1$

	/**
	 * When reading this preference, use
	 * {@link RepositoryUtil#getDefaultRepositoryDir()} instead, (for variable
	 * substitution).
	 */
	public static final String core_defaultRepositoryDir =
		"core_defaultRepositoryDir"; //$NON-NLS-1$

	/**
	 * Holds the key to the preferred merge strategy in the MergeStrategy
	 * registry, i.e. the preferred strategy can be obtained by
	 * {@code MergeStrategy.get(key)}.
	 */
	public static final String core_preferredMergeStrategy = "core_preferredMergeStrategy"; //$NON-NLS-1$

	/**
	 * Default key value of the core_preferredMergeStrategy property in the UI,
	 * which means that EGit must not pass any specific merge strategy to JGit,
	 * to let JGit use its default behavior.
	 */
	public static final String core_preferredMergeStrategy_Default = "jgit-default-mergeStrategy"; //$NON-NLS-1$

	/**
	 * if {@code true} file deletions are automatically staged by
	 * GitMoveDeleteHook
	 */
	public static final String core_autoStageDeletion = "core_auto_stage_deletion"; //$NON-NLS-1$

	/**
	 * if {@code true} file moves are automatically staged by GitMoveDeleteHook
	 */
	public static final String core_autoStageMoves = "core_auto_stage_moves"; //$NON-NLS-1$

	/**
	 * Max number of simultaneous pull jobs, default is one.
	 */
	public static final String core_maxPullThreadsCount = "core_max_pull_threads_count"; //$NON-NLS-1$
}

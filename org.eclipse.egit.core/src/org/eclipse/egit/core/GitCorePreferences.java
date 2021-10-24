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
public final class GitCorePreferences {

	private GitCorePreferences() {
		// No instantiation
	}

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

	/**
	 * Whether to store SSH key passphrases in the Eclipse secure store.
	 */
	public static final String core_saveCredentialsInSecureStore = "core_save_credentials_in_secure_store"; //$NON-NLS-1$

	/**
	 * HTTP client library to use. Currently allowed values are "jdk" and
	 * "apache", case insensitive, if undefined or any other value the default
	 * "jdk" will be used.
	 */
	public static final String core_httpClient = "core_http_client"; //$NON-NLS-1$

	/**
	 * The timeout in seconds for establishing a remote connection for cloning,
	 * pushing, or fetching.
	 */
	public static final String core_remoteConnectionTimeout = "core_remote_connection_timeout"; //$NON-NLS-1$

	/**
	 * If {@code true}, use an SSH agent, if available. If {@code false}, never
	 * use an SSH agent.
	 */
	public static final String core_sshAgent = "core_ssh_agent"; //$NON-NLS-1$

	/**
	 * The (absolute) path to an external GPG executable to use for signing
	 * commits or tags. If invalid or there is no executable file at the given
	 * location, it is ignored, and EGit tries to find a GPG executable itself
	 * on $PATH.
	 */
	public static final String core_gpgExecutable = "core_gpg_executable"; //$NON-NLS-1$

	/**
	 * The type of signer to use. Valid values are "bc" or "gpg".
	 */
	public static final String core_gpgSigner = "core_gpg_signer"; //$NON-NLS-1$

	/**
	 * Contains a memento containing the configured GitHost URIs.
	 */
	public static final String core_gitServers = "core_git_servers"; //$NON-NLS-1$

	/**
	 * The size of JGit's text buffer: the amount of bytes of a file or blob
	 * that will be examined to determine whether it is text or binary, and if
	 * text, whether it uses CR-LF line endings.
	 */
	public static final String core_textBufferSize = "core_textbuffersize"; //$NON-NLS-1$

}

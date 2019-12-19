/*******************************************************************************
 * Copyright (c) 2013, 2016 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 493352
 *******************************************************************************/
package org.eclipse.egit.core.internal.gerrit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Gerrit utilities
 */
public class GerritUtil {

	/**
	 * The Gerrit magic push prefix {@value} .
	 */
	public static final String REFS_FOR = "refs/for/"; //$NON-NLS-1$

	/**
	 * The Gerrit magic push prefix {@value}
	 */
	public static final String REFS_PUBLISH = "refs/publish/"; //$NON-NLS-1$

	/**
	 * The Gerrit magic push prefix {@value}
	 */
	public static final String REFS_DRAFTS = "refs/drafts/"; //$NON-NLS-1$

	/**
	 * @param config
	 * @return true if there if "create change ID" is configured
	 */
	public static boolean getCreateChangeId(Config config) {
		return config.getBoolean(ConfigConstants.CONFIG_GERRIT_SECTION,
				ConfigConstants.CONFIG_KEY_CREATECHANGEID, false);
	}

	/**
	 * Set the "create change ID" configuration
	 *
	 * @param config
	 */
	public static void setCreateChangeId(Config config) {
		config.setBoolean(ConfigConstants.CONFIG_GERRIT_SECTION, null,
				ConfigConstants.CONFIG_KEY_CREATECHANGEID, true);
	}

	/**
	 * Find the remote config for the given remote name
	 *
	 * @param config
	 *            the configuration containing the remote config
	 * @param remoteName
	 *            the name of the remote to find
	 * @return the found remoteConfig or {@code null}
	 * @throws URISyntaxException
	 *             if the configuration contains an illegal URI
	 */
	public static RemoteConfig findRemoteConfig(Config config, String remoteName)
			throws URISyntaxException {
		RemoteConfig remoteConfig = null;
		List<RemoteConfig> allRemoteConfigs = RemoteConfig
				.getAllRemoteConfigs(config);
		for (RemoteConfig rc : allRemoteConfigs) {
			if (rc.getName().equals(remoteName)) {
				remoteConfig = rc;
				break;
			}
		}
		return remoteConfig;
	}

	/**
	 * Configure the pushURI for Gerrit
	 *
	 * @param remoteConfig
	 *            the remote configuration to add this to
	 * @param pushURI
	 *            the pushURI to configure
	 */
	public static void configurePushURI(RemoteConfig remoteConfig,
			URIish pushURI) {
		List<URIish> pushURIs = new ArrayList<>(
				remoteConfig.getPushURIs());
		for (URIish urIish : pushURIs) {
			remoteConfig.removePushURI(urIish);
		}
		remoteConfig.addPushURI(pushURI);
	}

	/**
	 * Configure the gerrit push refspec HEAD:refs/for/<gerritBranch>
	 *
	 * @param remoteConfig
	 *            the remote configuration to configure this in
	 * @param gerritBranch
	 *            the branch to push to review for
	 */
	public static void configurePushRefSpec(RemoteConfig remoteConfig,
			String gerritBranch) {
		List<RefSpec> pushRefSpecs = new ArrayList<>(
				remoteConfig.getPushRefSpecs());
		for (RefSpec refSpec : pushRefSpecs) {
			remoteConfig.removePushRefSpec(refSpec);
		}
		remoteConfig.addPushRefSpec(new RefSpec(
				"HEAD:" + GerritUtil.REFS_FOR + gerritBranch)); //$NON-NLS-1$
	}

	/**
	 * Configure fetching review summary notes
	 *
	 * @param remoteConfig
	 *            the remote configuration to configure this in
	 * @return {@code true} if the {@code remoteConfig} was changed,
	 *         {@code false} otherwise.
	 */
	public static boolean configureFetchNotes(RemoteConfig remoteConfig) {
		String notesRef = Constants.R_NOTES + "*"; //$NON-NLS-1$
		List<RefSpec> fetchRefSpecs = remoteConfig.getFetchRefSpecs();
		for (RefSpec refSpec : fetchRefSpecs) {
			if (refSpec.matchSource(notesRef)) {
				return false;
			}
		}
		remoteConfig.addFetchRefSpec(new RefSpec(notesRef + ':' + notesRef));
		return true;
	}


	/**
	 * @param rc
	 *            the remote configuration
	 * @return {@code true} if the remote configuration is configured for
	 *         pushing to Gerrit
	 */
	public static boolean isGerritPush(RemoteConfig rc) {
		for (RefSpec pushSpec : rc.getPushRefSpecs()) {
			String destination = pushSpec.getDestination();
			if (destination == null) {
				continue;
			}
			if (destination.startsWith(GerritUtil.REFS_FOR)
					|| destination.startsWith(GerritUtil.REFS_PUBLISH)
					|| destination.startsWith(GerritUtil.REFS_DRAFTS)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param rc
	 *            the remote configuration
	 * @return {@code true} if the remote configuration is configured for
	 *         fetching from Gerrit
	 */
	public static boolean isGerritFetch(RemoteConfig rc) {
		for (RefSpec fetchSpec : rc.getFetchRefSpecs()) {
			String source = fetchSpec.getSource();
			String destination = fetchSpec.getDestination();
			if (source == null || destination == null) {
				continue;
			}
			if (source.startsWith(Constants.R_NOTES)
					&& destination.startsWith(Constants.R_NOTES)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * If the repository is not bare and looks like it might be a Gerrit
	 * repository, try to configure it such that EGit's Gerrit support is
	 * enabled.
	 *
	 * @param repository
	 *            to try to configure
	 */
	public static void tryToAutoConfigureForGerrit(
			@NonNull Repository repository) {
		if (repository.isBare()) {
			return;
		}
		StoredConfig config = repository.getConfig();
		boolean isGerrit = false;
		boolean changed = false;
		try {
			for (RemoteConfig remote : RemoteConfig
					.getAllRemoteConfigs(config)) {
				if (isGerritPush(remote)) {
					isGerrit = true;
					if (configureFetchNotes(remote)) {
						changed = true;
						remote.update(config);
					}
				}
			}
		} catch (URISyntaxException ignored) {
			// Ignore it here -- we're just trying to set up Gerrit support.
		}
		if (isGerrit) {
			if (config.getString(ConfigConstants.CONFIG_GERRIT_SECTION, null,
					ConfigConstants.CONFIG_KEY_CREATECHANGEID) != null) {
				// Already configured.
			} else {
				setCreateChangeId(config);
				changed = true;
			}
			if (changed) {
				try {
					config.save();
				} catch (IOException e) {
					Activator.logError(
							MessageFormat.format(
									CoreText.GerritUtil_ConfigSaveError,
									repository.getDirectory()),
							e);
				}
			}
		}
	}

	/**
	 * If the repository is not bare and looks like it might be a Gerrit
	 * repository, try to configure it such that EGit's Gerrit support is
	 * enabled. Does nothing if the {@code repositoryDir} is {@code null} or the
	 * repository cannot be configured.
	 *
	 * @param repositoryDir
	 *            .git Directory of the repository to try to configure
	 */
	public static void tryToAutoConfigureForGerrit(
			@Nullable File repositoryDir) {
		if (repositoryDir != null) {
			try {
				Repository repository = Activator.getDefault()
						.getRepositoryCache().lookupRepository(repositoryDir);
				if (repository != null) {
					tryToAutoConfigureForGerrit(repository);
				}
			} catch (IOException ignored) {
				// Ignore it here -- this is just a best-effort. If the repo
				// cannot be read, other places will report the problem.
			}
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.hosts;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Utilities for detecting git server types from {@link RemoteConfig}s.
 */
public final class GitHosts {

	private static final Pattern GITHUB_REMOTE_URL_PATTERN = Pattern.compile(
			"(?:(?:https?|ssh)://)?(?:[^@:/]+(?::[^@:/]*)?@)?github.com[:/][^/]+/.*\\.git"); //$NON-NLS-1$

	private GitHosts() {
		// No instantiation
	}

	/**
	 * Tells whether the argument looks like a Github clone URI.
	 *
	 * @param uri
	 *            URI to check
	 * @return {@code true} if {@code uri} is a Github clone URI,
	 *         {@code false} otherwise
	 */
	public static boolean isGithubUri(String uri) {
		return GITHUB_REMOTE_URL_PATTERN.matcher(uri).matches();
	}

	/**
	 * Tells whether the argument looks like a {@link RemoteConfig} for a Github
	 * upstream repository.
	 *
	 * @param rc
	 *            {@link RemoteConfig} to chcek
	 * @return {@code true} if {@code rc} is a Github {@link RemoteConfig},
	 *         {@code false} otherwise
	 */
	public static boolean isGithubConfig(RemoteConfig rc) {
		List<URIish> fetch = rc.getURIs();
		return !fetch.isEmpty() && isGithubUri(fetch.get(0).toPrivateString());
	}

	/**
	 * Tells whether the {@link Config} has any {@link RemoteConfig}s for a
	 * Github upstream.
	 *
	 * @param config
	 *            {@link Config} to check
	 * @return {@code true} if there is at least one Github
	 *         {@link RemoteConfig}, {@code false} otherwise
	 * @throws URISyntaxException
	 *             if the {@link Config} is invalid
	 */
	public static boolean hasGithubConfig(Config config)
			throws URISyntaxException {
		return RemoteConfig.getAllRemoteConfigs(config).stream()
				.anyMatch(GitHosts::isGithubConfig);
	}

	/**
	 * Retrieves all Github {@link RemoteConfig}s from the {@link Config}.
	 *
	 * @param config
	 *            {@link Config} to extract the {@link RemoteConfig}s from
	 * @return the Github {@link RemoteConfig}s, empty if there are none
	 * @throws URISyntaxException
	 *             if the {@link Config} is invalid
	 */
	public static Collection<RemoteConfig> getGithubConfigs(Config config)
			throws URISyntaxException {
		return RemoteConfig.getAllRemoteConfigs(config).stream()
				.filter(GitHosts::isGithubConfig).collect(Collectors.toList());
	}
}

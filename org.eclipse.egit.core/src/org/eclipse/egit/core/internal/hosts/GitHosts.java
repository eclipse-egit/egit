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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Provides {@link ServerType}s and utilities for detecting git server types
 * from {@link RemoteConfig}s.
 */
public final class GitHosts {

	private static final String GITHUB_ID = "github"; //$NON-NLS-1$

	private static final Pattern GITHUB_REMOTE_URL_PATTERN = Pattern.compile(
			"(?:(?:https?|ssh)://)?(?:[^@:/]+(?::[^@:/]*)?@)?github.com[:/][^/]+/.*\\.git"); //$NON-NLS-1$

	private static final Pattern GITHUB_PR_URL_PATTERN = Pattern
			.compile("https?://.*/pull/(\\d+)"); //$NON-NLS-1$

	private static final String GITLAB_ID = "gitlab"; //$NON-NLS-1$

	private static final Pattern GITLAB_REMOTE_URL_PATTERN = Pattern.compile(
			"(?:(?:https?|ssh)://)?(?:[^@:/]+(?::[^@:/]*)?@)?gitlab.com[:/][^/]+/.*\\.git"); //$NON-NLS-1$

	private static final Pattern ECLIPSE_GITLAB_REMOTE_URL_PATTERN = Pattern
			.compile(
					"(?:(?:https?|ssh)://)?(?:[^@:/]+(?::[^@:/]*)?@)?gitlab.eclipse.org[:/][^/]+/.*\\.git"); //$NON-NLS-1$

	private static final Pattern GITLAB_PR_URL_PATTERN = Pattern
			.compile("https?://.*/merge_requests/(\\d+)"); //$NON-NLS-1$

	private static final Pattern DIGITS = Pattern.compile("\\d+"); //$NON-NLS-1$

	private static final Map<String, Collection<Pattern>> URIS = new ConcurrentHashMap<>();

	static {
		addServerPattern(GITHUB_ID, GITHUB_REMOTE_URL_PATTERN);

		addServerPattern(GITLAB_ID, GITLAB_REMOTE_URL_PATTERN);
		addServerPattern(GITLAB_ID, ECLIPSE_GITLAB_REMOTE_URL_PATTERN);
	}

	private GitHosts() {
		// No instantiation
	}

	/**
	 * A {@link ServerType} encapsulates some characteristics of certain git
	 * server types.
	 */
	public enum ServerType {

		/** A {@link ServerType} describing Github git servers. */
		GITHUB(GITHUB_ID, "refs/pull/", "/head", //$NON-NLS-1$ //$NON-NLS-2$
				GITHUB_PR_URL_PATTERN),

		/** A {@link ServerType} describing Gitlab git servers. */
		GITLAB(GITLAB_ID, "refs/merge-requests/", "/head", //$NON-NLS-1$ //$NON-NLS-2$
				GITLAB_PR_URL_PATTERN);

		/** Constant indicating "no change ID". */
		public static final long NO_CHANGE_ID = -1;

		private final String id;

		private final String refPrefix;

		private final String refSuffix;

		private final Pattern urlPattern;

		private final Pattern refPattern;

		private final Pattern inputPattern;

		private ServerType(String id, String refPrefix, String refSuffix,
				Pattern urlPattern) {
			this.id = id;
			this.urlPattern = urlPattern;
			this.refPrefix = refPrefix;
			this.refSuffix = refSuffix;
			refPattern = Pattern.compile(refPrefix + "(\\d+)" + refSuffix); //$NON-NLS-1$
			inputPattern = refSuffix != null
					? Pattern
							.compile(refPrefix + "(\\d+)(?:" + refSuffix + ")?") //$NON-NLS-1$ //$NON-NLS-2$
					: refPattern;
		}

		/**
		 * Returns an internal ID for this {@link ServerType}.
		 *
		 * @return the internal ID
		 */
		public String getId() {
			return id;
		}

		/**
		 * Determines whether a remote URI looks like an URI for a host of this
		 * {@link ServerType}.
		 *
		 * @param uri
		 *            to check
		 * @return {@code true} if the URI most likely refers to a git server of
		 *         this {@link ServerType}, {@code false} otherwise
		 */
		public boolean uriMatches(String uri) {
			Collection<Pattern> patterns = URIS.get(this.getId());
			return patterns != null && patterns.stream()
					.anyMatch(p -> p.matcher(uri).matches());
		}

		/**
		 * Determines a change ID from a git ref.
		 *
		 * @param ref
		 *            to extract the change ID from
		 * @return the change ID, or {@link #NO_CHANGE_ID} if none can be
		 *         determined
		 */
		public long fromRef(String ref) {
			try {
				if (ref == null) {
					return NO_CHANGE_ID;
				}
				Matcher m = refPattern.matcher(ref);
				if (!m.matches() || m.group(1) == null) {
					return NO_CHANGE_ID;
				}
				return Long.parseLong(m.group(1));
			} catch (NumberFormatException | IndexOutOfBoundsException e) {
				// if we can't parse this, just return null
				return NO_CHANGE_ID;
			}
		}

		/**
		 * Determines a change ID from a general string.
		 *
		 * @param input
		 *            to find the change ID in
		 * @return the change ID, or {@link #NO_CHANGE_ID} if no change ID can
		 *         be determined
		 */
		public long fromString(String input) {
			if (input == null) {
				return NO_CHANGE_ID;
			}
			try {
				Matcher matcher = urlPattern.matcher(input);
				if (matcher.matches()) {
					return Long.parseLong(matcher.group(1));
				}
				matcher = inputPattern.matcher(input);
				if (matcher.matches()) {
					return Long.parseLong(matcher.group(1));
				}
				matcher = DIGITS.matcher(input);
				if (matcher.matches()) {
					return Long.parseLong(input);
				}
			} catch (NumberFormatException e) {
				// Numerical overflow?
			}
			return NO_CHANGE_ID;
		}

		/**
		 * Creates a git ref for fetching the the change with the given change
		 * ID.
		 *
		 * @param changeId
		 *            to fetch
		 * @return the git ref, or {@code null} if {@code changeId < 0}
		 */
		public String toFetchRef(long changeId) {
			if (changeId < 0) {
				return null;
			}
			return refSuffix == null ? refPrefix + changeId
					: refPrefix + changeId + refSuffix;
		}

		/**
		 * Retrieves the ref prefix for change refs.
		 *
		 * @return the prefix
		 */
		public String getRefPrefix() {
			return refPrefix;
		}
	}

	/**
	 * Tells whether the argument looks like a {@link RemoteConfig} for an
	 * upstream repository of a given {@link ServerType}.
	 *
	 * @param rc
	 *            {@link RemoteConfig} to check
	 * @param server
	 *            {@link ServerType} to check
	 * @return {@code true} if {@code rc} is a {@link RemoteConfig} with an
	 *         upstream URI matching the given {@link ServerType},
	 *         {@code false} otherwise
	 */
	public static boolean isServerConfig(RemoteConfig rc, ServerType server) {
		List<URIish> fetch = rc.getURIs();
		return !fetch.isEmpty()
				&& server.uriMatches(fetch.get(0).toPrivateString());
	}

	/**
	 * Tells whether the {@link Config} has any {@link RemoteConfig}s for a
	 * given {@link ServerType}.
	 *
	 * @param config
	 *            {@link Config} to check
	 * @param server
	 *            {@link ServerType} to test for
	 * @return {@code true} if there is at least one Github
	 *         {@link RemoteConfig}, {@code false} otherwise
	 * @throws URISyntaxException
	 *             if the {@link Config} is invalid
	 */
	public static boolean hasServerConfig(Config config, ServerType server)
			throws URISyntaxException {
		return RemoteConfig.getAllRemoteConfigs(config).stream()
				.anyMatch(rc -> isServerConfig(rc, server));
	}

	/**
	 * Retrieves all {@link RemoteConfig}s for the given {@link ServerType} from
	 * the {@link Config}.
	 *
	 * @param config
	 *            {@link Config} to extract the {@link RemoteConfig}s from
	 * @param server
	 *            {@link ServerType} to test for
	 * @return the {@link RemoteConfig}s for the given {@link ServerType}, empty
	 *         if there are none
	 * @throws URISyntaxException
	 *             if the {@link Config} is invalid
	 */
	public static Collection<RemoteConfig> getServerConfigs(Config config,
			ServerType server)
			throws URISyntaxException {
		return RemoteConfig.getAllRemoteConfigs(config).stream()
				.filter(rc -> isServerConfig(rc, server))
				.collect(Collectors.toList());
	}

	/**
	 * Adds a {@link Pattern} matching URIs for a given {@link ServerType}.
	 *
	 * @param server
	 *            described by the pattern
	 * @param uriPattern
	 *            to add
	 */
	public static void addServerPattern(ServerType server, Pattern uriPattern) {
		addServerPattern(server.getId(), uriPattern);
	}

	private static void addServerPattern(String id, Pattern uriPattern) {
		if (uriPattern != null) {
			URIS.computeIfAbsent(id, key -> new CopyOnWriteArrayList<>())
					.add(uriPattern);
		}
	}
}

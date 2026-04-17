/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel <Lars.Vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.hosts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.egit.core.internal.hosts.GitHosts.BranchRef;
import org.eclipse.egit.core.internal.hosts.GitHosts.ServerType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Parses user-pasted text that identifies a branch on a remote git host.
 * <p>
 * Parsing is delegated to the {@link ServerType}s defined in {@link GitHosts}
 * so custom hosts configured through preferences are handled the same way as
 * the built-in GitHub, GitLab and Gitea patterns.
 * <p>
 * Always returns a non-{@code null} {@link RemoteBranchInput} carrying at
 * minimum the original trimmed input as branch name; {@link #getOwner()} is
 * present only when a host pattern matched.
 */
public final class RemoteBranchInput {

	private final String branchName;

	private final String owner;

	private final ServerType serverType;

	private RemoteBranchInput(String branchName, String owner,
			ServerType serverType) {
		this.branchName = branchName;
		this.owner = owner;
		this.serverType = serverType;
	}

	/**
	 * @return the branch name without any owner prefix or URL wrapping; never
	 *         {@code null}
	 */
	public String getBranchName() {
		return branchName;
	}

	/**
	 * @return the owner/group/user part parsed from the input, or {@code null}
	 *         if the input did not include one
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return the {@link ServerType} the input was parsed as, or {@code null}
	 *         if no host pattern matched
	 */
	public ServerType getServerType() {
		return serverType;
	}

	/**
	 * Parses user-entered text into a {@link RemoteBranchInput}.
	 *
	 * @param input
	 *            raw text from the user (for example from a paste); may be
	 *            {@code null} or contain surrounding whitespace
	 * @return a parsed {@link RemoteBranchInput}, or {@code null} if
	 *         {@code input} is {@code null} or blank
	 */
	public static RemoteBranchInput parse(String input) {
		if (input == null) {
			return null;
		}
		String trimmed = input.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		// Try web URLs first: they are unambiguous across server types.
		for (ServerType type : ServerType.values()) {
			Optional<BranchRef> ref = type.parseBranchUrl(trimmed);
			if (ref.isPresent()) {
				return new RemoteBranchInput(ref.get().getBranchName(),
						ref.get().getOwner(), ref.get().getServerType());
			}
		}

		// Then shorthand textual references (like "owner:branch"). Skip these
		// if the input looks like a URL, since the loose text patterns could
		// otherwise match the URL prefix.
		if (!trimmed.contains("://")) { //$NON-NLS-1$
			for (ServerType type : ServerType.values()) {
				Optional<BranchRef> ref = type.parseBranchText(trimmed);
				if (ref.isPresent() && Repository.isValidRefName(
						Constants.R_HEADS + ref.get().getBranchName())) {
					return new RemoteBranchInput(ref.get().getBranchName(),
							ref.get().getOwner(), ref.get().getServerType());
				}
			}
		}

		return new RemoteBranchInput(trimmed, null, null);
	}

	/**
	 * Finds a {@link RemoteConfig} matching this input's owner and, when
	 * known, its {@link ServerType}. When {@link #getServerType()} is non-
	 * {@code null}, only remotes whose URI points at a server of that type are
	 * considered; this prevents, for example, a pasted GitLab URL from
	 * steering onto a GitHub remote whose name happens to match the GitLab
	 * group.
	 *
	 * @param remotes
	 *            candidates
	 * @return an {@link Optional} with the matching {@link RemoteConfig}, or
	 *         {@link Optional#empty()} if no match is found
	 */
	public Optional<RemoteConfig> findMatchingRemote(
			Collection<RemoteConfig> remotes) {
		return findRemoteByOwner(remotes, owner, serverType);
	}

	/**
	 * Finds a {@link RemoteConfig} from {@code remotes} that matches the given
	 * owner. Match strategies in priority order:
	 * <ol>
	 * <li>remote name equals the owner (case-insensitive);
	 * <li>first path segment of the remote's first fetch URI equals the owner;
	 * for GitLab-style nested groups the full group path is also compared.
	 * </ol>
	 *
	 * @param remotes
	 *            candidates
	 * @param owner
	 *            owner string to match; may be {@code null}
	 * @return an {@link Optional} with the matching {@link RemoteConfig}, or
	 *         {@link Optional#empty()} if no match is found
	 */
	public static Optional<RemoteConfig> findRemoteByOwner(
			Collection<RemoteConfig> remotes, String owner) {
		return findRemoteByOwner(remotes, owner, null);
	}

	/**
	 * Finds a {@link RemoteConfig} from {@code remotes} that matches the given
	 * owner and, when {@code serverType} is non-{@code null}, only considers
	 * remotes whose first fetch URI identifies a server of that type. Match
	 * strategies otherwise mirror
	 * {@link #findRemoteByOwner(Collection, String)}.
	 *
	 * @param remotes
	 *            candidates
	 * @param owner
	 *            owner string to match; may be {@code null}
	 * @param serverType
	 *            {@link ServerType} to restrict candidates to, or {@code null}
	 *            to accept any remote
	 * @return an {@link Optional} with the matching {@link RemoteConfig}, or
	 *         {@link Optional#empty()} if no match is found
	 */
	public static Optional<RemoteConfig> findRemoteByOwner(
			Collection<RemoteConfig> remotes, String owner,
			ServerType serverType) {
		if (owner == null || owner.isEmpty() || remotes == null) {
			return Optional.empty();
		}
		List<RemoteConfig> candidates;
		if (serverType == null) {
			candidates = remotes instanceof List
					? (List<RemoteConfig>) remotes
					: new ArrayList<>(remotes);
		} else {
			candidates = new ArrayList<>();
			for (RemoteConfig rc : remotes) {
				if (GitHosts.isServerConfig(rc, serverType)) {
					candidates.add(rc);
				}
			}
		}
		for (RemoteConfig rc : candidates) {
			if (owner.equalsIgnoreCase(rc.getName())) {
				return Optional.of(rc);
			}
		}
		for (RemoteConfig rc : candidates) {
			if (uriHasOwner(rc.getURIs(), owner)
					|| uriHasOwner(rc.getPushURIs(), owner)) {
				return Optional.of(rc);
			}
		}
		return Optional.empty();
	}

	private static boolean uriHasOwner(List<URIish> uris, String owner) {
		if (uris == null) {
			return false;
		}
		for (URIish u : uris) {
			String path = u.getPath();
			if (path == null || path.isEmpty()) {
				continue;
			}
			String stripped = path.startsWith("/") ? path.substring(1) : path; //$NON-NLS-1$
			int lastSlash = stripped.lastIndexOf('/');
			if (lastSlash <= 0) {
				continue;
			}
			String ownerPath = stripped.substring(0, lastSlash);
			if (owner.equalsIgnoreCase(ownerPath)) {
				return true;
			}
			int firstSlash = ownerPath.indexOf('/');
			String first = firstSlash < 0 ? ownerPath
					: ownerPath.substring(0, firstSlash);
			if (owner.equalsIgnoreCase(first)) {
				return true;
			}
		}
		return false;
	}
}

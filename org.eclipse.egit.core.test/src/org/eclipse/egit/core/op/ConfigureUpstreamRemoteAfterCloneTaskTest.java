/*******************************************************************************
 * Copyright (C) 2026 EGit Contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for the JSON parsing logic in
 * {@link ConfigureUpstreamRemoteAfterCloneTask}. These tests exercise the
 * static helper methods with sample API payloads so fork-detection regressions
 * are caught without requiring real network calls.
 */
public class ConfigureUpstreamRemoteAfterCloneTaskTest {

	// -----------------------------------------------------------------------
	// GitHub
	// -----------------------------------------------------------------------

	private static final String GITHUB_FORK_JSON = "{" //$NON-NLS-1$
			+ "\"id\":123," //$NON-NLS-1$
			+ "\"name\":\"my-fork\"," //$NON-NLS-1$
			+ "\"fork\": true," //$NON-NLS-1$
			+ "\"parent\":{" //$NON-NLS-1$
			+ "  \"clone_url\":\"https://github.com/upstream/repo.git\"," //$NON-NLS-1$
			+ "  \"ssh_url\":\"git@github.com:upstream/repo.git\"" //$NON-NLS-1$
			+ "}" //$NON-NLS-1$
			+ "}"; //$NON-NLS-1$

	private static final String GITHUB_NOT_FORK_JSON = "{" //$NON-NLS-1$
			+ "\"id\":456," //$NON-NLS-1$
			+ "\"name\":\"not-a-fork\"," //$NON-NLS-1$
			+ "\"fork\": false" //$NON-NLS-1$
			+ "}"; //$NON-NLS-1$

	@Test
	public void testGitHubFork_HttpsUrl() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitHubForkParentUrl(GITHUB_FORK_JSON, false);
		assertEquals("https://github.com/upstream/repo.git", result); //$NON-NLS-1$
	}

	@Test
	public void testGitHubFork_SshUrl() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitHubForkParentUrl(GITHUB_FORK_JSON, true);
		assertEquals("git@github.com:upstream/repo.git", result); //$NON-NLS-1$
	}

	@Test
	public void testGitHubNotFork_ReturnsNull() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitHubForkParentUrl(GITHUB_NOT_FORK_JSON, false);
		assertNull(result);
	}

	@Test
	public void testGitHubNullJson_ReturnsNull() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitHubForkParentUrl(null, false);
		assertNull(result);
	}

	/** Regression: the fork field must be matched even when it has whitespace. */
	@Test
	public void testGitHubFork_WhitespaceTolerant() {
		// GitHub API responses include whitespace around the colon
		String json = "{\"fork\":true,\"parent\":{\"clone_url\":\"https://github.com/up/repo.git\",\"ssh_url\":\"git@github.com:up/repo.git\"}}"; //$NON-NLS-1$
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitHubForkParentUrl(json, false);
		assertEquals("https://github.com/up/repo.git", result); //$NON-NLS-1$
	}

	// -----------------------------------------------------------------------
	// GitLab
	// -----------------------------------------------------------------------

	private static final String GITLAB_FORK_JSON = "{" //$NON-NLS-1$
			+ "\"id\":789," //$NON-NLS-1$
			+ "\"name\":\"my-fork\"," //$NON-NLS-1$
			+ "\"forked_from_project\":{" //$NON-NLS-1$
			+ "  \"http_url_to_repo\":\"https://gitlab.com/upstream/repo.git\"," //$NON-NLS-1$
			+ "  \"ssh_url_to_repo\":\"git@gitlab.com:upstream/repo.git\"" //$NON-NLS-1$
			+ "}" //$NON-NLS-1$
			+ "}"; //$NON-NLS-1$

	private static final String GITLAB_NOT_FORK_JSON = "{" //$NON-NLS-1$
			+ "\"id\":999," //$NON-NLS-1$
			+ "\"name\":\"not-a-fork\"" //$NON-NLS-1$
			+ "}"; //$NON-NLS-1$

	@Test
	public void testGitLabFork_HttpsUrl() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitLabForkParentUrl(GITLAB_FORK_JSON, false);
		assertEquals("https://gitlab.com/upstream/repo.git", result); //$NON-NLS-1$
	}

	@Test
	public void testGitLabFork_SshUrl() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitLabForkParentUrl(GITLAB_FORK_JSON, true);
		assertEquals("git@gitlab.com:upstream/repo.git", result); //$NON-NLS-1$
	}

	@Test
	public void testGitLabNotFork_ReturnsNull() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitLabForkParentUrl(GITLAB_NOT_FORK_JSON, false);
		assertNull(result);
	}

	@Test
	public void testGitLabNullJson_ReturnsNull() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseGitLabForkParentUrl(null, false);
		assertNull(result);
	}

	// -----------------------------------------------------------------------
	// Bitbucket
	// -----------------------------------------------------------------------

	private static final String BITBUCKET_FORK_JSON = "{" //$NON-NLS-1$
			+ "\"scm\":\"git\"," //$NON-NLS-1$
			+ "\"parent\":{" //$NON-NLS-1$
			+ "  \"links\":{" //$NON-NLS-1$
			+ "    \"clone\":[" //$NON-NLS-1$
			+ "      {\"href\":\"https://bitbucket.org/upstream/repo.git\",\"name\":\"https\"}," //$NON-NLS-1$
			+ "      {\"href\":\"ssh://git@bitbucket.org/upstream/repo.git\",\"name\":\"ssh\"}" //$NON-NLS-1$
			+ "    ]" //$NON-NLS-1$
			+ "  }" //$NON-NLS-1$
			+ "}" //$NON-NLS-1$
			+ "}"; //$NON-NLS-1$

	private static final String BITBUCKET_NOT_FORK_JSON = "{" //$NON-NLS-1$
			+ "\"scm\":\"git\"," //$NON-NLS-1$
			+ "\"name\":\"not-a-fork\"" //$NON-NLS-1$
			+ "}"; //$NON-NLS-1$

	@Test
	public void testBitbucketFork_HttpsUrl() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseBitbucketForkParentUrl(BITBUCKET_FORK_JSON, false);
		assertEquals("https://bitbucket.org/upstream/repo.git", result); //$NON-NLS-1$
	}

	@Test
	public void testBitbucketFork_SshUrl() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseBitbucketForkParentUrl(BITBUCKET_FORK_JSON, true);
		assertEquals("ssh://git@bitbucket.org/upstream/repo.git", result); //$NON-NLS-1$
	}

	@Test
	public void testBitbucketNotFork_ReturnsNull() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseBitbucketForkParentUrl(BITBUCKET_NOT_FORK_JSON, false);
		assertNull(result);
	}

	@Test
	public void testBitbucketNullJson_ReturnsNull() {
		String result = ConfigureUpstreamRemoteAfterCloneTask
				.parseBitbucketForkParentUrl(null, false);
		assertNull(result);
	}
}

/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel <lars.vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.hosts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

import org.eclipse.egit.core.internal.hosts.GitHosts.ServerType;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;

import com.google.gson.JsonParseException;

/**
 * Tests for {@link PullRequestInfo}.
 */
public class PullRequestInfoTest {

	private static String url(ServerType server, String remote, long number)
			throws URISyntaxException {
		return PullRequestInfo.apiUrl(server, new URIish(remote), number);
	}

	@Test
	public void githubHttpsUrl() throws Exception {
		assertEquals("https://api.github.com/repos/eclipse-egit/egit/pulls/42",
				url(ServerType.GITHUB, "https://github.com/eclipse-egit/egit.git",
						42));
	}

	@Test
	public void githubScpLikeUrl() throws Exception {
		assertEquals("https://api.github.com/repos/eclipse-egit/egit/pulls/42",
				url(ServerType.GITHUB, "git@github.com:eclipse-egit/egit.git",
						42));
	}

	@Test
	public void githubEnterpriseUrl() throws Exception {
		assertEquals("https://ghe.example.com/api/v3/repos/org/repo/pulls/7",
				url(ServerType.GITHUB, "ssh://git@ghe.example.com/org/repo/",
						7));
	}

	@Test
	public void githubRejectsNestedPath() throws Exception {
		assertNull(url(ServerType.GITHUB,
				"https://github.com/a/b/c.git", 1));
	}

	@Test
	public void gitlabNestedGroupsAreEncoded() throws Exception {
		assertEquals(
				"https://gitlab.example.com:8443/api/v4/projects/group%2Fsub%2Frepo/merge_requests/3",
				url(ServerType.GITLAB,
						"https://gitlab.example.com:8443/group/sub/repo.git",
						3));
	}

	@Test
	public void giteaUrl() throws Exception {
		assertEquals("https://gitea.com/api/v1/repos/owner/repo/pulls/12",
				url(ServerType.GITEA, "https://gitea.com/owner/repo", 12));
	}

	@Test
	public void rejectsInvalidInput() throws Exception {
		assertNull(url(ServerType.GITHUB, "https://github.com/owner/repo", -1));
		assertNull(url(ServerType.GITHUB, "https://github.com/", 1));
		assertNull(url(ServerType.GITHUB, "https://github.com/../repo", 1));
		assertNull(url(ServerType.GITHUB, "/local/path/repo.git", 1));
		assertNull(url(ServerType.GITLAB, "https://gitlab.com/repo", 1));
		assertNull(url(ServerType.GITEA, "https://gitea.com/a/b/c", 1));
	}

	@Test
	public void githubHostIsCaseInsensitive() throws Exception {
		assertEquals("https://api.github.com/repos/owner/repo/pulls/0",
				url(ServerType.GITHUB, "https://GitHub.com/owner/repo", 0));
	}

	@Test
	public void repositoryNameMayStartWithDot() throws Exception {
		assertEquals("https://api.github.com/repos/org/.github/pulls/5",
				url(ServerType.GITHUB, "https://github.com/org/.github.git",
						5));
	}

	@Test
	public void httpRemoteKeepsSchemeAndPort() throws Exception {
		assertEquals("http://gitea.local:3000/api/v1/repos/owner/repo/pulls/2",
				url(ServerType.GITEA, "http://gitea.local:3000/owner/repo", 2));
	}

	@Test
	public void sshPortIsNotUsedForTheApi() throws Exception {
		assertEquals("https://gitea.local/api/v1/repos/owner/repo/pulls/2",
				url(ServerType.GITEA, "ssh://git@gitea.local:2222/owner/repo",
						2));
	}

	@Test
	public void credentialsOnlyGoToTheSameService() {
		assertTrue(PullRequestInfo.isSameService("gitlab.com", "GitLab.com"));
		assertTrue(PullRequestInfo.isSameService("github.com",
				"api.github.com"));
		assertFalse(PullRequestInfo.isSameService("ghe.example.com",
				"api.github.com"));
		assertFalse(PullRequestInfo.isSameService("example.com",
				"api.example.com"));
		assertFalse(PullRequestInfo.isSameService(null, "github.com"));
	}

	@Test
	public void githubSourceBranch() {
		PullRequestInfo info = PullRequestInfo.fromJson(ServerType.GITHUB,
				"{\"number\": 42, \"user\": {\"login\": \"x\"}, \"head\": {"
						+ "\"label\": \"x:fix\\u002Fnpe\", \"ref\": \"fix/npe\","
						+ "\"repo\": {\"name\": \"egit\", \"fork\": true}},"
						+ "\"base\": {\"ref\": \"master\"}, \"draft\": false,"
						+ "\"labels\": [], \"milestone\": null}");
		assertNotNull(info);
		assertEquals("fix/npe", info.getSourceBranch());
	}

	@Test
	public void gitlabSourceBranch() {
		PullRequestInfo info = PullRequestInfo.fromJson(ServerType.GITLAB,
				"{\"iid\": 3, \"source_branch\": \"feature-x\","
						+ "\"target_branch\": \"main\", \"upvotes\": 1.5e1}");
		assertNotNull(info);
		assertEquals("feature-x", info.getSourceBranch());
	}

	@Test
	public void missingSourceBranch() {
		assertNull(PullRequestInfo.fromJson(ServerType.GITHUB,
				"{\"message\": \"Not Found\", \"head\": {\"ref\": \"\"}}"));
		assertNull(PullRequestInfo.fromJson(ServerType.GITEA,
				"{\"head\": null}"));
	}

	@Test
	public void giteaDeletedSourceBranch() {
		assertNull(PullRequestInfo.fromJson(ServerType.GITEA,
				"{\"head\": {\"ref\": \"refs/pull/1/head\"}}"));
		PullRequestInfo info = PullRequestInfo.fromJson(ServerType.GITEA,
				"{\"head\": {\"ref\": \"refs/heads/topic\"}}");
		assertNotNull(info);
		assertEquals("topic", info.getSourceBranch());
	}

	@Test(expected = JsonParseException.class)
	public void invalidJson() {
		PullRequestInfo.fromJson(ServerType.GITHUB, "{\"head\": {\"ref\": }");
	}

	@Test(expected = JsonParseException.class)
	public void trailingGarbage() {
		PullRequestInfo.fromJson(ServerType.GITHUB, "{} x");
	}
}

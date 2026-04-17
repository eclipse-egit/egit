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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;

public class RemoteBranchInputTest {

	@Test
	public void plainBranchNameIsReturnedUnchangedWithoutOwner() {
		RemoteBranchInput result = RemoteBranchInput.parse("my-branch");
		assertNotNull(result);
		assertEquals("my-branch", result.getBranchName());
		assertNull(result.getOwner());
	}

	@Test
	public void leadingAndTrailingWhitespaceIsTrimmed() {
		RemoteBranchInput result = RemoteBranchInput.parse("  feature/x  ");
		assertNotNull(result);
		assertEquals("feature/x", result.getBranchName());
		assertNull(result.getOwner());
	}

	@Test
	public void nullInputReturnsNull() {
		assertNull(RemoteBranchInput.parse(null));
	}

	@Test
	public void blankInputReturnsNull() {
		assertNull(RemoteBranchInput.parse("   "));
	}

	@Test
	public void githubOwnerBranchFormatIsParsed() {
		RemoteBranchInput result = RemoteBranchInput
				.parse("vogella:product-scoped-theme-preference");
		assertNotNull(result);
		assertEquals("product-scoped-theme-preference", result.getBranchName());
		assertEquals("vogella", result.getOwner());
	}

	@Test
	public void githubOwnerBranchWithSlashInBranch() {
		RemoteBranchInput result = RemoteBranchInput
				.parse("alice:feature/foo-bar");
		assertNotNull(result);
		assertEquals("feature/foo-bar", result.getBranchName());
		assertEquals("alice", result.getOwner());
	}

	@Test
	public void ownerBranchRejectedWhenBranchIsInvalidRefName() {
		// ".." is not a valid ref name segment
		RemoteBranchInput result = RemoteBranchInput.parse("alice:..");
		assertNotNull(result);
		// Falls back to input as branch with no owner
		assertEquals("alice:..", result.getBranchName());
		assertNull(result.getOwner());
	}

	@Test
	public void gitlabOwnerRepoBranchFormatIsParsed() {
		// Format shown on GitLab MR pages: "<owner>/<repo>:<branch>"
		RemoteBranchInput result = RemoteBranchInput
				.parse("GNOME/gnome-shell:wip/foo");
		assertNotNull(result);
		assertEquals("wip/foo", result.getBranchName());
		assertEquals("GNOME", result.getOwner());
	}

	@Test
	public void gitlabNestedGroupRepoBranchFormatIsParsed() {
		RemoteBranchInput result = RemoteBranchInput
				.parse("eclipse-wg/asciidoc-wg/asciidoc.org:feature-y");
		assertNotNull(result);
		assertEquals("feature-y", result.getBranchName());
		assertEquals("eclipse-wg/asciidoc-wg", result.getOwner());
	}

	@Test
	public void githubTreeUrlIsParsed() {
		RemoteBranchInput result = RemoteBranchInput.parse(
				"https://github.com/vogella/egit/tree/product-scoped-theme-preference");
		assertNotNull(result);
		assertEquals("product-scoped-theme-preference", result.getBranchName());
		assertEquals("vogella", result.getOwner());
	}

	@Test
	public void githubTreeUrlWithQueryIsParsed() {
		RemoteBranchInput result = RemoteBranchInput.parse(
				"https://github.com/vogella/egit/tree/feature-x?diff=split");
		assertNotNull(result);
		assertEquals("feature-x", result.getBranchName());
		assertEquals("vogella", result.getOwner());
	}

	@Test
	public void gitlabTreeUrlIsParsed() {
		RemoteBranchInput result = RemoteBranchInput.parse(
				"https://gitlab.gnome.org/GNOME/gnome-shell/-/tree/wip/foo");
		assertNotNull(result);
		assertEquals("wip/foo", result.getBranchName());
		assertEquals("GNOME", result.getOwner());
	}

	@Test
	public void gitlabTreeUrlWithNestedGroupIsParsed() {
		RemoteBranchInput result = RemoteBranchInput.parse(
				"https://gitlab.eclipse.org/eclipse-wg/asciidoc-wg/asciidoc.org/-/tree/feature-y");
		assertNotNull(result);
		assertEquals("feature-y", result.getBranchName());
		assertEquals("eclipse-wg/asciidoc-wg", result.getOwner());
	}

	@Test
	public void giteaSrcBranchUrlIsParsed() {
		RemoteBranchInput result = RemoteBranchInput
				.parse("https://gitea.com/alice/repo/src/branch/dev");
		assertNotNull(result);
		assertEquals("dev", result.getBranchName());
		assertEquals("alice", result.getOwner());
	}

	@Test
	public void trailingSlashInUrlBranchIsStripped() {
		RemoteBranchInput result = RemoteBranchInput
				.parse("https://github.com/vogella/egit/tree/foo/");
		assertNotNull(result);
		assertEquals("foo", result.getBranchName());
		assertEquals("vogella", result.getOwner());
	}

	@Test
	public void unrecognizedUrlIsReturnedAsBranch() {
		RemoteBranchInput result = RemoteBranchInput
				.parse("https://example.com/unknown");
		assertNotNull(result);
		assertEquals("https://example.com/unknown", result.getBranchName());
		assertNull(result.getOwner());
	}

	@Test
	public void findRemoteByOwnerReturnsEmptyForNullOwner() {
		Optional<RemoteConfig> match = RemoteBranchInput
				.findRemoteByOwner(Collections.emptyList(), null);
		assertFalse(match.isPresent());
	}

	@Test
	public void findRemoteByOwnerMatchesByName() throws Exception {
		RemoteConfig origin = newRemote("origin",
				"https://github.com/eclipse-egit/egit.git");
		RemoteConfig vogella = newRemote("vogella",
				"https://github.com/vogella/egit.git");
		Optional<RemoteConfig> match = RemoteBranchInput
				.findRemoteByOwner(Arrays.asList(origin, vogella), "vogella");
		assertTrue(match.isPresent());
		assertEquals("vogella", match.get().getName());
	}

	@Test
	public void findRemoteByOwnerMatchesByHttpsUrl() throws Exception {
		RemoteConfig origin = newRemote("origin",
				"https://github.com/eclipse-egit/egit.git");
		RemoteConfig fork = newRemote("myfork",
				"https://github.com/vogella/egit.git");
		Optional<RemoteConfig> match = RemoteBranchInput.findRemoteByOwner(
				Arrays.asList(origin, fork), "vogella");
		assertTrue(match.isPresent());
		assertEquals("myfork", match.get().getName());
	}

	@Test
	public void findRemoteByOwnerMatchesBySshUrl() throws Exception {
		RemoteConfig fork = newRemote("myfork",
				"git@github.com:vogella/egit.git");
		Optional<RemoteConfig> match = RemoteBranchInput
				.findRemoteByOwner(Collections.singletonList(fork), "vogella");
		assertTrue(match.isPresent());
	}

	@Test
	public void findRemoteByOwnerMatchesGitlabNestedGroup() throws Exception {
		RemoteConfig remote = newRemote("gnome",
				"https://gitlab.eclipse.org/eclipse-wg/asciidoc-wg/asciidoc.org.git");
		Optional<RemoteConfig> byFirstSegment = RemoteBranchInput
				.findRemoteByOwner(Collections.singletonList(remote),
						"eclipse-wg");
		assertTrue(byFirstSegment.isPresent());
		Optional<RemoteConfig> byFullGroup = RemoteBranchInput.findRemoteByOwner(
				Collections.singletonList(remote),
				"eclipse-wg/asciidoc-wg");
		assertTrue(byFullGroup.isPresent());
	}

	@Test
	public void findRemoteByOwnerReturnsEmptyWhenNoMatch() throws Exception {
		RemoteConfig origin = newRemote("origin",
				"https://github.com/eclipse-egit/egit.git");
		Optional<RemoteConfig> match = RemoteBranchInput.findRemoteByOwner(
				Collections.singletonList(origin), "someone-else");
		assertFalse(match.isPresent());
	}

	@Test
	public void nameMatchWinsOverUrlMatch() throws Exception {
		// Remote "vogella" has an unrelated URL; another remote's URL owner
		// is "vogella". Name match must take priority.
		RemoteConfig vogella = newRemote("vogella",
				"https://github.com/other/repo.git");
		RemoteConfig byUrl = newRemote("upstream",
				"https://github.com/vogella/repo.git");
		Optional<RemoteConfig> match = RemoteBranchInput.findRemoteByOwner(
				Arrays.asList(byUrl, vogella), "vogella");
		assertTrue(match.isPresent());
		assertEquals("vogella", match.get().getName());
	}

	private static RemoteConfig newRemote(String name, String url)
			throws URISyntaxException {
		Config config = new Config();
		RemoteConfig rc = new RemoteConfig(config, name);
		rc.addURI(new URIish(url));
		return rc;
	}
}

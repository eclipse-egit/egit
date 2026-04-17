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
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.eclipse.egit.core.internal.hosts.GitHosts.BranchRef;
import org.eclipse.egit.core.internal.hosts.GitHosts.ServerType;
import org.junit.Test;

/**
 * Exercises the branch URL and branch text parsing added to
 * {@link ServerType}.
 */
public class GitHostsBranchParsingTest {

	@Test
	public void githubParsesOwnerBranchText() {
		Optional<BranchRef> ref = ServerType.GITHUB
				.parseBranchText("vogella:feature-x");
		assertTrue(ref.isPresent());
		assertEquals("vogella", ref.get().getOwner());
		assertEquals("feature-x", ref.get().getBranchName());
	}

	@Test
	public void githubDoesNotParseGitlabText() {
		Optional<BranchRef> ref = ServerType.GITHUB
				.parseBranchText("GNOME/gnome-shell:wip/foo");
		assertFalse(ref.isPresent());
	}

	@Test
	public void gitlabParsesOwnerRepoBranchText() {
		Optional<BranchRef> ref = ServerType.GITLAB
				.parseBranchText("GNOME/gnome-shell:wip/foo");
		assertTrue(ref.isPresent());
		assertEquals("GNOME", ref.get().getOwner());
		assertEquals("wip/foo", ref.get().getBranchName());
	}

	@Test
	public void gitlabParsesNestedGroupText() {
		Optional<BranchRef> ref = ServerType.GITLAB
				.parseBranchText("eclipse-wg/asciidoc-wg/asciidoc.org:feature-y");
		assertTrue(ref.isPresent());
		assertEquals("eclipse-wg/asciidoc-wg", ref.get().getOwner());
		assertEquals("feature-y", ref.get().getBranchName());
	}

	@Test
	public void githubParsesTreeUrl() {
		Optional<BranchRef> ref = ServerType.GITHUB.parseBranchUrl(
				"https://github.com/vogella/egit/tree/feature-x");
		assertTrue(ref.isPresent());
		assertEquals("vogella", ref.get().getOwner());
		assertEquals("feature-x", ref.get().getBranchName());
	}

	@Test
	public void gitlabParsesTreeUrlWithNestedGroup() {
		Optional<BranchRef> ref = ServerType.GITLAB.parseBranchUrl(
				"https://gitlab.eclipse.org/eclipse-wg/asciidoc-wg/asciidoc.org/-/tree/foo");
		assertTrue(ref.isPresent());
		assertEquals("eclipse-wg/asciidoc-wg", ref.get().getOwner());
		assertEquals("foo", ref.get().getBranchName());
	}

	@Test
	public void giteaParsesSrcBranchUrl() {
		Optional<BranchRef> ref = ServerType.GITEA
				.parseBranchUrl("https://gitea.com/alice/repo/src/branch/dev");
		assertTrue(ref.isPresent());
		assertEquals("alice", ref.get().getOwner());
		assertEquals("dev", ref.get().getBranchName());
	}

	@Test
	public void githubDoesNotParseGitlabTreeUrl() {
		Optional<BranchRef> ref = ServerType.GITHUB.parseBranchUrl(
				"https://gitlab.example.com/group/repo/-/tree/foo");
		assertFalse(ref.isPresent());
	}

	@Test
	public void emptyAndNullInputsReturnEmpty() {
		assertFalse(ServerType.GITHUB.parseBranchText(null).isPresent());
		assertFalse(ServerType.GITHUB.parseBranchUrl(null).isPresent());
		assertFalse(ServerType.GITHUB.parseBranchText("").isPresent());
	}
}

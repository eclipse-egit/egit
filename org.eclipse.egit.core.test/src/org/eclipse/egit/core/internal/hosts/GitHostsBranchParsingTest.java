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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
		BranchRef ref = ServerType.GITHUB.parseBranchText("vogella:feature-x");
		assertNotNull(ref);
		assertEquals("vogella", ref.getOwner());
		assertEquals("feature-x", ref.getBranchName());
	}

	@Test
	public void githubDoesNotParseGitlabText() {
		BranchRef ref = ServerType.GITHUB
				.parseBranchText("GNOME/gnome-shell:wip/foo");
		assertNull(ref);
	}

	@Test
	public void gitlabParsesOwnerRepoBranchText() {
		BranchRef ref = ServerType.GITLAB
				.parseBranchText("GNOME/gnome-shell:wip/foo");
		assertNotNull(ref);
		assertEquals("GNOME", ref.getOwner());
		assertEquals("wip/foo", ref.getBranchName());
	}

	@Test
	public void gitlabParsesNestedGroupText() {
		BranchRef ref = ServerType.GITLAB
				.parseBranchText("eclipse-wg/asciidoc-wg/asciidoc.org:feature-y");
		assertNotNull(ref);
		assertEquals("eclipse-wg/asciidoc-wg", ref.getOwner());
		assertEquals("feature-y", ref.getBranchName());
	}

	@Test
	public void githubParsesTreeUrl() {
		BranchRef ref = ServerType.GITHUB.parseBranchUrl(
				"https://github.com/vogella/egit/tree/feature-x");
		assertNotNull(ref);
		assertEquals("vogella", ref.getOwner());
		assertEquals("feature-x", ref.getBranchName());
	}

	@Test
	public void gitlabParsesTreeUrlWithNestedGroup() {
		BranchRef ref = ServerType.GITLAB.parseBranchUrl(
				"https://gitlab.eclipse.org/eclipse-wg/asciidoc-wg/asciidoc.org/-/tree/foo");
		assertNotNull(ref);
		assertEquals("eclipse-wg/asciidoc-wg", ref.getOwner());
		assertEquals("foo", ref.getBranchName());
	}

	@Test
	public void giteaParsesSrcBranchUrl() {
		BranchRef ref = ServerType.GITEA
				.parseBranchUrl("https://gitea.com/alice/repo/src/branch/dev");
		assertNotNull(ref);
		assertEquals("alice", ref.getOwner());
		assertEquals("dev", ref.getBranchName());
	}

	@Test
	public void githubDoesNotParseGitlabTreeUrl() {
		BranchRef ref = ServerType.GITHUB.parseBranchUrl(
				"https://gitlab.example.com/group/repo/-/tree/foo");
		assertNull(ref);
	}

	@Test
	public void emptyAndNullInputsReturnNull() {
		assertNull(ServerType.GITHUB.parseBranchText(null));
		assertNull(ServerType.GITHUB.parseBranchUrl(null));
		assertNull(ServerType.GITHUB.parseBranchText(""));
	}
}

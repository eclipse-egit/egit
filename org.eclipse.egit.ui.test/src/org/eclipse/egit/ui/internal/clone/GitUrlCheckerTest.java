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
package org.eclipse.egit.ui.internal.clone;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GitUrlCheckerTest {

	@Test
	public void testUrl() {
		assertEquals("https://some.example.org/things-and-stuff/my_repo.git",
				GitUrlChecker.sanitizeAsGitUrl(
						"  https://some.example.org/things-and-stuff/my_repo.git "));
	}

	@Test
	public void testFileUrlWithSpace() {
		assertEquals("file:///C:/User/Archibald Thor/git/repo.git",
				GitUrlChecker.sanitizeAsGitUrl(
						"file:///C:/User/Archibald Thor/git/repo.git"));
	}

	@Test
	public void testGitClonePrefix() {
		assertEquals("https://git.eclipse.org/r/egit/egit",
				GitUrlChecker.sanitizeAsGitUrl(
						"  git clone https://git.eclipse.org/r/egit/egit  "));
	}

	@Test
	public void testGitClonePrefixWithQuotes() {
		assertEquals("https://git.eclipse.org/r/egit/egit",
				GitUrlChecker.sanitizeAsGitUrl(
						"  git clone \"https://git.eclipse.org/r/egit/egit\"  "));
	}

	@Test
	public void testBrokenUrlWithSpace() {
		assertEquals("https://git.eclipse.org/r/e", GitUrlChecker
				.sanitizeAsGitUrl("https://git.eclipse.org/r/e git/egit"));
	}

	@Test
	public void testMultiline() {
		assertEquals("https://git.eclipse.org/r/egit/egit",
				GitUrlChecker.sanitizeAsGitUrl(
						"  git clone \"https://git.eclipse.org/r/egit/egit\"  \n"
								+ "https://some.example.org/a/b.git"));
	}

}

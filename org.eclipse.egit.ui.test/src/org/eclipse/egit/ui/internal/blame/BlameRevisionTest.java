/******************************************************************************
 *  Copyright (c) 2022 Simeon Andreev and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.blame.BlameRevision.Diff;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link BlameRevision}.
 */
public class BlameRevisionTest extends LocalRepositoryTestCase {

	private Repository repo;

	@Before
	public void createRepo() throws Exception {
		repo = createLocalTestRepository(REPO1);
	}

	@After
	public void closeRepo() throws Exception {
		if (repo != null) {
			repo.close();
			repo = null;
		}
	}

	@Test
	public void testBlameRevisionForMovedFile() throws Exception {
		try (Git git = new Git(repo)) {
			git.commit().setMessage("initial commit").call();

			String defaultContent = "line1";
			String originalLine = defaultContent + "\n";
			String changedLine = defaultContent + " changed " + "\n";

			File workingTree = repo.getWorkTree();
			Path file1 = workingTree.toPath().resolve("file1.txt");
			Files.writeString(file1, originalLine);
			git.add().addFilepattern(".").call();
			RevCommit commit1 = git.commit().setMessage("file1 added").call();

			Files.writeString(file1, changedLine);
			git.add().addFilepattern(".").call();
			RevCommit commit2 = git.commit().setMessage("file1 changed").call();

			Files.delete(file1);
			Files.writeString(file1.getParent().resolve("file2.txt"),
					changedLine);

			git.add().addFilepattern(".").call();
			git.commit().setMessage("file1 renamed to file2").call();

			BlameRevision blameRevision = new BlameRevision();
			blameRevision.setRepository(repo);
			blameRevision.setCommit(commit2);
			blameRevision.setSourcePath(file1.getFileName().toString());

			Diff diff = blameRevision.getDiffToParent(commit1);
			assertNotNull("Expected a diff for a moved file", diff);

			RawText diffOldText = diff.getOldText();
			RawText diffNewText = diff.getNewText();
			assertEquals("Unexpected diff old text for a moved file",
					originalLine, toString(diffOldText));
			assertEquals("Unexpected diff new text for a moved file",
					changedLine, toString(diffNewText));
		}
	}

	private String toString(RawText diffOldText) {
		byte[] rawContent = diffOldText.getRawContent();
		return new String(rawContent);
	}
}
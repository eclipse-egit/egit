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

import org.eclipse.egit.ui.internal.blame.BlameRevision.Diff;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

/**
 * Tests for {@link BlameRevision}.
 */
public class BlameRevisionTest extends RepositoryTestCase {

	@Test
	public void testBlameRevisionForMovedFile() throws Exception {
		try (Git git = new Git(db)) {
			git.commit().setMessage("initial commit").call();

			String defaultContent = "line1";
			String originalLine = defaultContent + "\n";
			String changedLine = defaultContent + " changed " + "\n";

			File file1 = writeTrashFile("file1.txt", originalLine);
			git.add().addFilepattern(".").call();
			RevCommit commit1 = git.commit().setMessage("file1 added").call();

			writeTrashFile("file1.txt", changedLine);
			git.add().addFilepattern(".").call();
			RevCommit commit2 = git.commit().setMessage("file1 changed").call();

			file1.delete();
			writeTrashFile("file2.txt", changedLine);
			git.add().addFilepattern(".").call();
			git.commit().setMessage("file1 renamed to file2").call();

			BlameRevision blameRevision = new BlameRevision();
			blameRevision.setRepository(db);
			blameRevision.setCommit(commit2);
			blameRevision.setSourcePath(file1.getName());

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
/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.commit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter;
import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter.DiffStyleRange;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link DiffStyleRangeFormatter}
 */
public class DiffStyleRangeFormatterTest extends LocalRepositoryTestCase {

	private static Repository repository;

	private static RevCommit commit;

	@BeforeClass
	public static void setup() throws Exception {
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(repoFile);
		assertNotNull(repository);

		RevWalk walk = new RevWalk(repository);
		try {
			commit = walk.parseCommit(repository.resolve(Constants.HEAD));
			assertNotNull(commit);
			walk.parseBody(commit.getParent(0));
		} finally {
			walk.release();
		}
	}

	@Test
	public void testRanges() throws Exception {
		IDocument document = new Document();
		DiffStyleRangeFormatter formatter = new DiffStyleRangeFormatter(
				document);
		formatter.setRepository(repository);
		formatter.format(commit.getTree(), commit.getParent(0).getTree());
		assertTrue(document.getLength() > 0);
		DiffStyleRange[] ranges = formatter.getRanges();
		assertNotNull(ranges);
		assertTrue(ranges.length > 0);
		for (DiffStyleRange range : ranges) {
			assertNotNull(range);
			assertNotNull(range.diffType);
			assertTrue(range.start >= 0);
			assertTrue(range.length >= 0);
			assertTrue(range.start < document.getLength());
		}

	}

}

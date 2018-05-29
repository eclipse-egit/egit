/*******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.commit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.DiffRegion;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DiffRegionFormatter}.
 */
public class DiffRegionFormatterTest extends LocalRepositoryTestCase {

	private Repository repository;

	private RevCommit commit;

	@Before
	public void setup() throws Exception {
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(repoFile);
		assertNotNull(repository);

		try (RevWalk walk = new RevWalk(repository)) {
			commit = walk.parseCommit(repository.resolve(Constants.HEAD));
			assertNotNull(commit);
			walk.parseBody(commit.getParent(0));
		}
	}

	@Test
	public void testRanges() throws Exception {
		IDocument document = new Document();
		DiffRegion[] regions;
		try (DiffRegionFormatter formatter = new DiffRegionFormatter(
				document)) {
			formatter.setRepository(repository);
			formatter.format(commit.getTree(), commit.getParent(0).getTree());
			assertTrue(document.getLength() > 0);
			regions = formatter.getRegions();
		}
		assertNotNull(regions);
		assertTrue(regions.length > 0);
		for (DiffRegion region : regions) {
			assertNotNull(region);
			assertTrue(region.getOffset() >= 0);
			assertTrue(region.getLength() >= 0);
			assertTrue(region.getOffset() < document.getLength());
		}
	}

}

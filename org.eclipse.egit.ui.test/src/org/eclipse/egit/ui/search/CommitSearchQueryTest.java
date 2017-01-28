/*******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.search.CommitSearchQuery;
import org.eclipse.egit.ui.internal.search.CommitSearchResult;
import org.eclipse.egit.ui.internal.search.CommitSearchSettings;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.search.ui.ISearchResult;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link CommitSearchQuery}
 */
public class CommitSearchQueryTest extends LocalRepositoryTestCase {

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
		}
	}

	private CommitSearchSettings createSettings() {
		CommitSearchSettings settings = new CommitSearchSettings();
		settings.addRepository(repository.getDirectory().getAbsolutePath());
		settings.setMatchTree(false);
		settings.setMatchAuthor(false);
		settings.setMatchCommit(false);
		settings.setMatchCommitter(false);
		settings.setMatchParents(false);
		settings.setMatchMessage(false);
		settings.setCaseSensitive(true);
		settings.setRegExSearch(false);
		return settings;
	}

	private void validateResult(RevCommit expectedCommit,
			Repository expectedRepository, ISearchResult result) {
		assertNotNull(result);
		assertTrue(result instanceof CommitSearchResult);
		CommitSearchResult commitResult = (CommitSearchResult) result;
		assertEquals(1, commitResult.getMatchCount());
		Object[] elements = commitResult.getElements();
		assertNotNull(elements);
		assertEquals(1, elements.length);
		assertTrue(elements[0] instanceof RepositoryCommit);
		RepositoryCommit repoCommit = (RepositoryCommit) elements[0];
		assertEquals(expectedRepository.getDirectory(), repoCommit
				.getRepository().getDirectory());
		assertEquals(expectedCommit, repoCommit.getRevCommit());
	}

	private void validateEmpty(ISearchResult result) {
		assertNotNull(result);
		assertTrue(result instanceof CommitSearchResult);
		CommitSearchResult commitResult = (CommitSearchResult) result;
		assertEquals(0, commitResult.getMatchCount());
		Object[] elements = commitResult.getElements();
		assertNotNull(elements);
		assertEquals(0, elements.length);
	}

	@Test
	public void testQuery() {
		CommitSearchQuery query = new CommitSearchQuery(createSettings());
		assertTrue(query.canRerun());
		assertTrue(query.canRunInBackground());
		assertNotNull(query.getLabel());
		assertTrue(query.getLabel().length() > 0);
	}

	@Test
	public void testMatchCommit() throws Exception {
		CommitSearchSettings settings = createSettings();
		settings.setMatchCommit(true);
		settings.setTextPattern(commit.name());
		CommitSearchQuery query = new CommitSearchQuery(settings);
		IStatus status = query.run(new NullProgressMonitor());
		assertNotNull(status);
		assertTrue(status.isOK());
		validateResult(commit, repository, query.getSearchResult());
	}

	@Test
	public void testEmptyMatches() {
		CommitSearchSettings settings = createSettings();
		settings.setMatchCommit(true);
		settings.setTextPattern("badcommitid");
		CommitSearchQuery query = new CommitSearchQuery(settings);
		IStatus status = query.run(new NullProgressMonitor());
		assertNotNull(status);
		assertTrue(status.isOK());
		validateEmpty(query.getSearchResult());
	}

	@Test
	public void testCaseInsensitive() {
		CommitSearchSettings settings = createSettings();
		settings.setMatchCommit(true);
		settings.setCaseSensitive(true);
		settings.setTextPattern(commit.name().toUpperCase(Locale.ROOT));
		CommitSearchQuery query = new CommitSearchQuery(settings);
		IStatus status = query.run(new NullProgressMonitor());
		assertNotNull(status);
		assertTrue(status.isOK());
		validateEmpty(query.getSearchResult());
		settings.setCaseSensitive(false);
		status = query.run(new NullProgressMonitor());
		assertNotNull(status);
		assertTrue(status.isOK());
		validateResult(commit, repository, query.getSearchResult());
	}

	@Test
	public void testMatchTree() throws Exception {
		CommitSearchSettings settings = createSettings();
		settings.setMatchTree(true);
		settings.setTextPattern(commit.getTree().name());
		CommitSearchQuery query = new CommitSearchQuery(settings);
		IStatus status = query.run(new NullProgressMonitor());
		assertNotNull(status);
		assertTrue(status.isOK());
		validateResult(commit, repository, query.getSearchResult());
	}

	@Test
	public void testMatchParent() throws Exception {
		CommitSearchSettings settings = createSettings();
		settings.setMatchParents(true);
		settings.setTextPattern(commit.getParent(0).name());
		CommitSearchQuery query = new CommitSearchQuery(settings);
		IStatus status = query.run(new NullProgressMonitor());
		assertNotNull(status);
		assertTrue(status.isOK());
		validateResult(commit, repository, query.getSearchResult());
	}

	@Test
	public void testMatchMessage() {
		CommitSearchSettings settings = createSettings();
		settings.setMatchMessage(true);
		settings.setTextPattern(commit.getFullMessage());
		CommitSearchQuery query = new CommitSearchQuery(settings);
		IStatus status = query.run(new NullProgressMonitor());
		assertNotNull(status);
		assertTrue(status.isOK());
		validateResult(commit, repository, query.getSearchResult());
	}
}

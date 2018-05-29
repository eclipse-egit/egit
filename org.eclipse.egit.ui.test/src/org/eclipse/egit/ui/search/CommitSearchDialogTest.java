/******************************************************************************
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
 *****************************************************************************/
package org.eclipse.egit.ui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.search.CommitSearchPage;
import org.eclipse.egit.ui.internal.search.RepositoryMatch;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search2.internal.ui.InternalSearchUI;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link CommitSearchPage}
 */
@SuppressWarnings("restriction")
public class CommitSearchDialogTest extends LocalRepositoryTestCase {

	private Repository repository;

	private RevCommit commit;

	@Before
	public void setup() throws Exception {
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(repoFile);
		assertNotNull(repository);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repository.getDirectory());

		try (RevWalk walk = new RevWalk(repository)) {
			commit = walk.parseCommit(repository.resolve(Constants.HEAD));
			assertNotNull(commit);
			walk.parseBody(commit.getParent(0));
		}
	}

	@Test
	public void openCommitTabOnSearchDialog() throws Exception {
		bot.menu("Search").menu("Search...").click();
		SWTBotShell shell = bot.shell("Search");
		if (!shell.isActive()) {
			shell.activate();
		}
		TestUtil.processUIEvents();
		shell.bot().tabItem("Git Search").activate();
		shell.bot().comboBox().setText(commit.name());
		SWTBotButton search = shell.bot().button("Search");
		assertTrue(search.isEnabled());
		search.click();
		TestUtil.waitForJobs(500, 5000);

		TestUtil.joinJobs(InternalSearchUI.FAMILY_SEARCH);
		bot.viewById(NewSearchUI.SEARCH_VIEW_ID).show();
		TestUtil.processUIEvents();

		final SWTBotTreeItem[] repos = bot.activeView().bot().tree()
				.getAllItems();
		assertEquals(1, repos.length);
		Object repoData = UIThreadRunnable.syncExec(new Result<Object>() {

			@Override
			public Object run() {
				return repos[0].widget.getData();
			}
		});
		assertTrue(repoData instanceof RepositoryMatch);
		assertEquals(repository.getDirectory(), ((RepositoryMatch) repoData)
				.getRepository().getDirectory());
		final SWTBotTreeItem[] commits = TestUtil.expandAndWait(repos[0])
				.getItems();
		assertEquals(1, commits.length);
		Object commitData = UIThreadRunnable.syncExec(new Result<Object>() {

			@Override
			public Object run() {
				return commits[0].widget.getData();
			}
		});
		assertTrue(commitData instanceof RepositoryCommit);
		assertEquals(commit, ((RepositoryCommit) commitData).getRevCommit());
	}
}

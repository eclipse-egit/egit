/*******************************************************************************
 * Copyright (C) 2026, Lars Vogel <Lars.Vogel@vogella.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class StagingViewSubmoduleTest extends AbstractStagingViewTestCase {

	private static final String SUBMODULE_PATH = "sub";

	@Test
	public void updateSubmoduleFromContextMenu() throws Exception {
		SubmoduleAddCommand add = new SubmoduleAddCommand(repository);
		add.setPath(SUBMODULE_PATH);
		add.setURI(new URIish(repository.getDirectory().toURI().toString())
				.toString());
		add.call().close();
		ObjectId recorded;
		try (Git git = new Git(repository)) {
			git.commit().setMessage("Add submodule").call();
		}
		try (Repository sub = SubmoduleWalk.getSubmoduleRepository(repository,
				SUBMODULE_PATH)) {
			assertNotNull(sub);
			try (Git subGit = new Git(sub)) {
				recorded = sub.resolve(Constants.HEAD);
				// Move the submodule away from the commit recorded in the parent
				subGit.commit().setAllowEmpty(true).setMessage("Moved").call();
				assertNotEquals(recorded, sub.resolve(Constants.HEAD));
			}
		}
		// The parent cache does not listen to submodules added later
		IndexDiffCache.INSTANCE.getIndexDiffCacheEntry(repository).refresh();
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);

		StagingViewTester stagingView = StagingViewTester.openStagingView();
		SWTBot viewBot = stagingView.getView().bot();
		SWTBotTree unstagedTree = viewBot.tree(0);
		TestUtil.waitUntilTreeHasNodeContainsText(viewBot, unstagedTree,
				SUBMODULE_PATH, 10000);
		TestUtil.getNode(unstagedTree.getAllItems(), SUBMODULE_PATH).select();

		ContextMenuHelper.clickContextMenuSync(unstagedTree,
				UIText.StagingView_UpdateSubmodule);
		TestUtil.joinJobs(
				org.eclipse.egit.ui.JobFamilies.SUBMODULE_UPDATE);

		try (Repository sub = SubmoduleWalk.getSubmoduleRepository(repository,
				SUBMODULE_PATH)) {
			assertEquals(recorded, sub.resolve(Constants.HEAD));
		}
	}
}

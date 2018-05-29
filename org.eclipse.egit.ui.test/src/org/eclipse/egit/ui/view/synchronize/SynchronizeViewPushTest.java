/*******************************************************************************
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for "Push" action in Synchronize view.
 */
public class SynchronizeViewPushTest extends AbstractSynchronizeViewTest {

	@Before
	public void prepare() throws Exception {
		Repository childRepository = lookupRepository(childRepositoryFile);

		Repository repository = lookupRepository(repositoryFile);
		StoredConfig config = repository.getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		remoteConfig.addURI(new URIish(childRepository.getDirectory().getParentFile().toURI().toURL()));
		remoteConfig.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
		remoteConfig.update(config);

		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "master", ConfigConstants.CONFIG_KEY_REMOTE, "origin");
		config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "master", ConfigConstants.CONFIG_KEY_MERGE, "refs/heads/master");
		config.save();

		FetchOperation fetchOperation = new FetchOperation(repository, remoteConfig, 60, false);
		fetchOperation.run(null);
	}

	@Test
	public void shouldUpdateTrackingBranchOnPush() throws Exception {
		makeChangesAndCommit(PROJ1);
		Repository repository = lookupRepository(repositoryFile);
		ObjectId headId = repository.resolve(Constants.HEAD);

		String trackingBranch = Constants.R_REMOTES + "origin/master";
		launchSynchronization(Constants.HEAD, trackingBranch, false);

		SWTBotView viewBot = bot.viewById(ISynchronizeView.VIEW_ID);
		SWTBotToolbarButton pushButton = viewBot.toolbarButton(UIText.GitActionContributor_Push);
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.PUSH, 30, TimeUnit.SECONDS);
		pushButton.click();
		TestUtil.openJobResultDialog(jobJoiner.join());

		String destinationString = repositoryFile.getParentFile().getName() + " - " + "origin";
		SWTBotShell resultDialog = bot.shell(NLS.bind(UIText.PushResultDialog_title, destinationString));
		resultDialog.close();

		Repository remoteRepository = lookupRepository(childRepositoryFile);
		ObjectId masterOnRemote = remoteRepository.resolve("master");
		assertThat("Expected push to update branch on remote repository", masterOnRemote, is(headId));

		ObjectId trackingId = repository.resolve(trackingBranch);
		assertThat("Expected tracking branch to be updated", trackingId, is(headId));
	}
}

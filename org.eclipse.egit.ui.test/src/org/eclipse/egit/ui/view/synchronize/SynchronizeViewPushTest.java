/*******************************************************************************
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;

import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.test.JobJoiner;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for "Push" action in Synchronize view.
 */
public class SynchronizeViewPushTest extends AbstractSynchronizeViewTest {

	@Before
	public void prepare() throws Exception {
		FileRepository childRepository = lookupRepository(childRepositoryFile);

		FileRepository repository = lookupRepository(repositoryFile);
		FileBasedConfig config = repository.getConfig();
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
		FileRepository repository = lookupRepository(repositoryFile);
		ObjectId headId = repository.resolve(Constants.HEAD);

		String trackingBranch = Constants.R_REMOTES + "origin/master";
		launchSynchronization(Constants.HEAD, trackingBranch, false);

		SWTBotView viewBot = bot.viewByTitle("Synchronize");
		SWTBotToolbarButton pushButton = viewBot.toolbarButton(UIText.GitActionContributor_Push);
		JobJoiner jobJoiner = JobJoiner.startListening(JobFamilies.PUSH, 30, TimeUnit.SECONDS);
		pushButton.click();
		jobJoiner.join();

		String destinationString = repositoryFile.getParentFile().getName() + " - " + "origin";
		SWTBotShell resultDialog = bot.shell(NLS.bind(UIText.ResultDialog_title, destinationString));
		resultDialog.close();

		FileRepository remoteRepository = lookupRepository(childRepositoryFile);
		ObjectId masterOnRemote = remoteRepository.resolve("master");
		assertThat("Expected push to update branch on remote repository", masterOnRemote, is(headId));

		ObjectId trackingId = repository.resolve(trackingBranch);
		assertThat("Expected tracking branch to be updated", trackingId, is(headId));
	}
}

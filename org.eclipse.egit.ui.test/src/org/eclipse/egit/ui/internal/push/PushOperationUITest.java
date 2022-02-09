/*******************************************************************************
 * Copyright (c) 2022 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.junit.Before;
import org.junit.Test;

public class PushOperationUITest extends LocalRepositoryTestCase {

	private Repository repository;

	private Repository remoteRepository;

	@Before
	public void createRepositories() throws Exception {
		File repositoryFile = createProjectAndCommitToRepository();
		File remoteRepositoryFile = createSimpleRemoteRepository(
				repositoryFile);
		repository = lookupRepository(repositoryFile);
		remoteRepository = lookupRepository(remoteRepositoryFile);
	}

	@Test
	public void pushDefaultBranch() throws Exception {
		touchAndSubmit("new content", "Changed");
		ObjectId head = repository.resolve(Constants.HEAD);
		ObjectId remote = repository
				.resolve(Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME
						+ '/' + Constants.MASTER);
		assertNotEquals(head, remote);
		PushOperationUI op = new PushOperationUI(repository,
				Constants.DEFAULT_REMOTE_NAME, false);
		assertSuccess(op.execute(null));
		assertEquals(head,
				repository.resolve(
						Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME
								+ '/' + Constants.MASTER));
		assertEquals(head,
				repository.resolve(Constants.R_HEADS + Constants.MASTER));
	}

	@Test
	public void pushToSpecificRemoteBranch() throws Exception {
		ObjectId initialHead = repository.resolve(Constants.HEAD);
		touchAndSubmit("new content", "Changed");
		ObjectId head = repository.resolve(Constants.HEAD);
		// Set remote tracking branch
		StoredConfig cfg = repository.getConfig();
		cfg.setString(ConfigConstants.CONFIG_BRANCH_SECTION, Constants.MASTER,
				ConfigConstants.CONFIG_KEY_REMOTE,
				Constants.DEFAULT_REMOTE_NAME);
		cfg.setString(ConfigConstants.CONFIG_BRANCH_SECTION, Constants.MASTER,
				ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + "foobar");
		cfg.save();
		RemoteConfig config = SimpleConfigurePushDialog
				.getConfiguredRemote(repository);
		if (config == null) {
			config = new RemoteConfig(cfg, Constants.DEFAULT_REMOTE_NAME);
		}
		PushOperationUI op = new PushOperationUI(repository,
				repository.getFullBranch(), config, false);
		assertSuccess(op.execute(null));
		assertEquals(head,
				repository.resolve(Constants.R_REMOTES
						+ Constants.DEFAULT_REMOTE_NAME + '/' + "foobar"));
		assertEquals(head,
				remoteRepository.resolve(Constants.R_HEADS + "foobar"));
		assertEquals(initialHead,
				remoteRepository.resolve(Constants.R_HEADS + Constants.MASTER));
	}

	private void assertSuccess(PushOperationResult result) {
		result.getURIs().forEach(u -> {
			PushResult inner = result.getPushResult(u);
			inner.getRemoteUpdates().forEach(up -> {
				assertEquals(Status.OK, up.getStatus());
			});
		});
	}
}

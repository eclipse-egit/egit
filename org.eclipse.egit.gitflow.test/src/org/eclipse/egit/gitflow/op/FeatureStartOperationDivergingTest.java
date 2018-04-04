/*******************************************************************************
 * Copyright (C) 2018, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.core.op.PullOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FeatureStartOperationDivergingTest extends AbstractDualRepositoryTestCase {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testFeatureStart_localDevelopBehind() throws Exception {
		GitFlowRepository gfRepo1 = new GitFlowRepository(
				repository1.getRepository());
		GitFlowRepository gfRepo2 = new GitFlowRepository(
				repository2.getRepository());

		fetch(gfRepo2);
		init(gfRepo2);
		setDevelopRemote(gfRepo1, gfRepo2);

		repository1.commit("origin/develop is 1 commit ahead");

		FeatureStartOperation featureStartOperation = new FeatureStartOperation(
				gfRepo2, MY_FEATURE);

		expectedException.expect(CoreException.class);
		expectedException.expectMessage(
				"Branches 'develop' and 'origin/develop' have diverged."
						+ "\nAnd branch 'develop' may be fast-forwarded.");
		featureStartOperation.execute(null);
	}

	@Test
	public void testFeatureStart_localDevelopAhead() throws Exception {
		GitFlowRepository gfRepo1 = new GitFlowRepository(
				repository1.getRepository());
		GitFlowRepository gfRepo2 = new GitFlowRepository(
				repository2.getRepository());

		fetch(gfRepo2);
		init(gfRepo2);
		setDevelopRemote(gfRepo1, gfRepo2);

		new PullOperation(Collections.singleton(repository2.getRepository()),
				-1).execute(null);

		repository2.commit("develop is 1 commit ahead");

		FeatureStartOperation featureStartOperation = new FeatureStartOperation(
				gfRepo2, MY_FEATURE);
		featureStartOperation.execute(null);

		assertEquals("feature branch successfully created and checked out",
				gfRepo2.getConfig().getFullFeatureBranchName(MY_FEATURE),
				repository2.getRepository().getFullBranch());
	}

	private void fetch(GitFlowRepository gfRepo2)
			throws InvocationTargetException {
		RemoteConfig config = gfRepo2.getConfig().getDefaultRemoteConfig();
		FetchOperation fetchOperation = new FetchOperation(
				gfRepo2.getRepository(), config,
				-1, false);
		fetchOperation.run(null);
	}

	private void setDevelopRemote(GitFlowRepository remote,
			GitFlowRepository local) throws IOException {
		String develop = local.getConfig().getDevelop();
		String developFull = remote.getConfig().getDevelopFull();
		local.setRemote(develop, DEFAULT_REMOTE_NAME);
		local.setUpstreamBranchName(develop, developFull);
	}

	private void init(GitFlowRepository gfRepo2) throws CoreException {
		InitOperation initOperation = new InitOperation(
				gfRepo2.getRepository());
		initOperation.execute(null);
	}

}

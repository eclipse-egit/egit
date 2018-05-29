/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eclipse.egit.gitflow.GitFlowRepository;
import static org.eclipse.jgit.lib.Constants.*;
import org.eclipse.jgit.lib.Ref;
import org.junit.Test;

import static org.eclipse.egit.gitflow.op.AbstractFeatureOperation.*;

public class FeatureListOperationTest extends AbstractDualRepositoryTestCase {
	@Test
	public void testFeatureList() throws Exception {
		GitFlowRepository gfRepo1 = new GitFlowRepository(
				repository1.getRepository());
		GitFlowRepository gfRepo2 = new GitFlowRepository(
				repository2.getRepository());
		new FeatureStartOperation(gfRepo1, MY_FEATURE).execute(null);

		FeatureListOperation featureListOperation = new FeatureListOperation(
				gfRepo2, 0);
		featureListOperation.execute(null);
		String name = gfRepo2.getConfig().getFullFeatureBranchName(MY_FEATURE);
		assertNotNull(featureListOperation.getOperationResult()
				.getAdvertisedRef(name));
		List<Ref> result = featureListOperation.getResult();
		assertEquals(1, result.size());
		assertEquals(
				R_REMOTES + DEFAULT_REMOTE_NAME + SEP
				+ gfRepo2.getConfig().getFeatureBranchName(MY_FEATURE),
				result.get(0).getName());
	}
}

/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.common.RepoPropertiesPage;
import org.eclipse.egit.ui.common.RepoRemoteBranchesPage;
import org.eclipse.egit.ui.test.TestUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class GitCloneWizardHttpTest extends GitCloneWizardTestBase {

	@BeforeClass
	public static void setup() throws Exception {
		TestUtil.disableProxy();
		r = new SampleTestRepository(NUMBER_RANDOM_COMMITS, true);
	}

	@Test
	public void canCloneARemoteRepo() throws Exception {
		destRepo = new File(ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toFile(), "test" + System.nanoTime());

		importWizard.openWizard();
		RepoPropertiesPage propertiesPage = importWizard.openRepoPropertiesPage();
		propertiesPage.setURI(r.getUri());
		propertiesPage.setUser("agitter");
		propertiesPage.setPassword("letmein");
		propertiesPage.setStoreInSecureStore(false);

		RepoRemoteBranchesPage remoteBranches = propertiesPage
				.nextToRemoteBranches();

		cloneRepo(destRepo, remoteBranches);
	}
}

/*******************************************************************************
 * Copyright (C) 2010, 2012 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.junit.After;
import org.junit.Before;

public abstract class DualRepositoryTestCase {

	protected TestUtils testUtils = new TestUtils();

	protected TestRepository repository1;

	protected TestRepository repository2;

	protected IProject testProject;

	@Before
	public void beforeTestCase() throws Exception {
		// ensure there are no shared Repository instances left
		// when starting a new test
		Activator.getDefault().getRepositoryCache().clear();
	}

	@After
	public void afterTestCase() throws Exception {
		Activator.getDefault().getRepositoryCache().clear();
		if (repository1 != null)
			repository1.dispose();
		if (repository2 != null)
			repository2.dispose();
		if (testProject != null) {
			testProject.close(null);
			testProject.delete(false, false, null);
		}
		testUtils.deleteTempDirs();
	}

	protected IProject importProject(TestRepository repo, String projectName)
			throws Exception {
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		if (firstProject.exists())
			firstProject.delete(false, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(projectName);
		File parentFile = repo.getRepository().getWorkTree();
		desc.setLocation(new Path(new File(parentFile, projectName).getPath()));
		firstProject.create(desc, null);
		firstProject.open(null);
		ConnectProviderOperation cop = new ConnectProviderOperation(
				firstProject, repo.getRepository().getDirectory());
		cop.execute(null);
		return firstProject;
	}



}

/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.ConnectProviderOperation;

public abstract class DualRepositoryTestCase {

	protected TestUtils testUtils = new TestUtils();

	protected TestRepository repository1;

	protected TestRepository repository2;

	protected IProject testProject;

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

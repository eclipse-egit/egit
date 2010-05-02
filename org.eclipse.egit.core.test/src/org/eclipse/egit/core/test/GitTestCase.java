/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.SystemReader;
import org.junit.After;
import org.junit.Before;

public abstract class GitTestCase {

	protected TestProject project;

	protected File gitDir;

	@Before
	public void setUp() throws Exception {
		MockSystemReader mockSystemReader = new MockSystemReader();
		SystemReader.setInstance(mockSystemReader);
		mockSystemReader.setProperty(Constants.GIT_CEILING_DIRECTORIES_KEY,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
						.getAbsoluteFile().toString());
		project = new TestProject(true);
		gitDir = new File(project.getProject().getWorkspace().getRoot()
				.getRawLocation().toFile(), Constants.DOT_GIT);
		rmrf(gitDir);
	}

	@After
	public void tearDown() throws Exception {
		project.dispose();
		rmrf(gitDir);
	}

	private void rmrf(File d) throws IOException {
		if (!d.exists())
			return;

		File[] files = d.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; ++i) {
				if (files[i].isDirectory())
					rmrf(files[i]);
				else if (!files[i].delete())
					throw new IOException(files[i] + " in use or undeletable");
			}
		}
		if (!d.delete())
			throw new IOException(d + " in use or undeletable");
		assert !d.exists();
	}

}

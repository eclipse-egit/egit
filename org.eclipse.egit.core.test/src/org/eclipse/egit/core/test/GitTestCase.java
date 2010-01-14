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
import org.eclipse.jgit.lib.Constants;

import junit.framework.TestCase;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.util.SystemReader;

public abstract class GitTestCase extends TestCase {

	protected TestProject project;

	protected File gitDir;

	protected void setUp() throws Exception {
		super.setUp();
		((MockSystemReader) SystemReader.getInstance()).setProperty(
				Constants.GIT_CEILING_DIRECTORIES_KEY, ResourcesPlugin
						.getWorkspace().getRoot().getLocation().toFile()
						.getAbsoluteFile().toString());
		project = new TestProject(true);
		gitDir = new File(project.getProject().getWorkspace().getRoot()
				.getRawLocation().toFile(), Constants.DOT_GIT);
		rmrf(gitDir);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
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

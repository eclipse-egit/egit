/*******************************************************************************
 * Copyright (C) 2007, 2013 Robin Rosenberg <robin.rosenberg@dewire.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.GitCorePreferences;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.SystemReader;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class GitTestCase {

	protected final TestUtils testUtils = new TestUtils();

	protected TestProject project;

	protected File gitDir;

	@BeforeClass
	public static void setUpClass() {
		// suppress auto-ignoring and auto-sharing to avoid interference
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(Activator
				.getPluginId());
		p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources, false);
		p.putBoolean(GitCorePreferences.core_autoShareProjects, false);
	}

	@Before
	public void setUp() throws Exception {
		// ensure there are no shared Repository instances left
		// when starting a new test
		Activator.getDefault().getRepositoryCache().clear();
		MockSystemReader mockSystemReader = new MockSystemReader();
		SystemReader.setInstance(mockSystemReader);
		mockSystemReader.setProperty(Constants.GIT_CEILING_DIRECTORIES_KEY,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
						.getAbsoluteFile().toString());
		project = new TestProject(true);
		gitDir = new File(project.getProject().getWorkspace().getRoot()
				.getRawLocation().toFile(), Constants.DOT_GIT);
		if (gitDir.exists())
			FileUtils.delete(gitDir, FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	@After
	public void tearDown() throws Exception {
		project.dispose();
		Activator.getDefault().getRepositoryCache().clear();
		if (gitDir.exists())
			FileUtils.delete(gitDir, FileUtils.RECURSIVE | FileUtils.RETRY);
	}

	protected ObjectId createFile(Repository repository, IProject actProject, String name, String content) throws IOException {
		File file = new File(actProject.getProject().getLocation().toFile(), name);
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(content);
		fileWriter.close();
		byte[] fileContents = IO.readFully(file);
		ObjectInserter inserter = repository.newObjectInserter();
		try {
			ObjectId objectId = inserter.insert(Constants.OBJ_BLOB, fileContents);
			inserter.flush();
			return objectId;
		} finally {
			inserter.release();
		}
	}

	protected ObjectId createFileCorruptShort(Repository repository,
			IProject actProject, String name, String content)
			throws IOException {
		ObjectId id = createFile(repository, actProject, name, content);
		File file = new File(repository.getDirectory(), "objects/"
				+ id.name().substring(0, 2) + "/" + id.name().substring(2));
		byte[] readFully = IO.readFully(file);
		FileUtils.delete(file);
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		byte[] truncatedData = new byte[readFully.length - 1];
		System.arraycopy(readFully, 0, truncatedData, 0, truncatedData.length);
		fileOutputStream.write(truncatedData);
		fileOutputStream.close();
		return id;
	}
}

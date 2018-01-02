/*******************************************************************************
 * Copyright (C) 2007, 2013 Robin Rosenberg <robin.rosenberg@dewire.com> and others.
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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
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
		File configFile = File.createTempFile("gitconfigtest", "config");
		MockSystemReader mockSystemReader = new MockSystemReader() {
			@Override
			public FileBasedConfig openUserConfig(Config parent, FS fs) {
				return new FileBasedConfig(parent, configFile, fs);
			}
		};
		configFile.deleteOnExit();
		SystemReader.setInstance(mockSystemReader);
		mockSystemReader.setProperty(Constants.GIT_CEILING_DIRECTORIES_KEY,
				ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
						.getParentFile().getAbsoluteFile().toString());
		FileBasedConfig userConfig = mockSystemReader.openUserConfig(null,
				FS.DETECTED);
		// We have to set autoDetach to false for tests, because tests expect to
		// be able to clean up by recursively removing the repository, and
		// background GC might be in the middle of writing or deleting files,
		// which would disrupt this.
		userConfig.setBoolean(ConfigConstants.CONFIG_GC_SECTION, null,
				ConfigConstants.CONFIG_KEY_AUTODETACH, false);
		userConfig.save();
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
		SystemReader.setInstance(null);
	}

	protected ObjectId createFile(Repository repository, IProject actProject, String name, String content) throws IOException {
		File file = new File(actProject.getProject().getLocation().toFile(), name);
		try (Writer fileWriter = new OutputStreamWriter(
				Files.newOutputStream(file.toPath()), "UTF-8")) {
			fileWriter.write(content);
		}
		byte[] fileContents = IO.readFully(file);
		try (ObjectInserter inserter = repository.newObjectInserter()) {
			ObjectId objectId = inserter.insert(Constants.OBJ_BLOB, fileContents);
			inserter.flush();
			return objectId;
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
		try (OutputStream fileOutputStream = Files
				.newOutputStream(file.toPath())) {
			byte[] truncatedData = new byte[readFully.length - 1];
			System.arraycopy(readFully, 0, truncatedData, 0,
					truncatedData.length);
			fileOutputStream.write(truncatedData);
		}
		return id;
	}
}

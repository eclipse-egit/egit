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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.jgit.util.IO;
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
		TestUtils.rmrf(gitDir);
	}

	@After
	public void tearDown() throws Exception {
		project.dispose();
		TestUtils.rmrf(gitDir);
	}

	protected ObjectId createFile(Repository repository, IProject project, String name, String content) throws IOException {
		File file = new File(project.getProject().getLocation().toFile(), name);
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(content);
		fileWriter.close();
		ObjectWriter objectWriter = new ObjectWriter(repository);
		return objectWriter.writeBlob(file);
	}

	protected ObjectId createFileCorruptShort(Repository repository, IProject project, String name, String content) throws IOException {
		ObjectId id = createFile(repository, project, name, content);
		File file = new File(repository.getDirectory(), "objects/" + id.name().substring(0,2) + "/" + id.name().substring(2));
		byte[] readFully = IO.readFully(file);
		file.delete();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		byte[] truncatedData = new byte[readFully.length - 1];
		System.arraycopy(readFully, 0, truncatedData, 0, truncatedData.length);
		fileOutputStream.write(truncatedData);
		fileOutputStream.close();
		return id;
	}

	protected ObjectId createEmptyTree(Repository repository) throws IOException {
		ObjectWriter objectWriter = new ObjectWriter(repository);
		Tree tree = new Tree(repository);
		return objectWriter.writeTree(tree);
	}

	protected String slurpAndClose(InputStream inputStream) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		try {
			int ch;
			while ((ch = inputStream.read()) != -1) {
				stringBuilder.append((char)ch);
			}
		} finally {
			inputStream.close();
		}
		return stringBuilder.toString();
	}

}

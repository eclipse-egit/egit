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
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
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
		gitDir = new File(project.getProject().getWorkspace()
				.getRoot().getRawLocation().toFile(),
				Constants.DOT_GIT);
		rmrf(gitDir);
	}

	@After
	public void tearDown() throws Exception {
		project.dispose();
		rmrf(gitDir);
	}

	/**
	 * Delete the file d recursively. <strong>Be very careful when executing
	 * this function. It is intended to cleanup between test case, but could
	 * really destroy anything.</strong>
	 *
	 * @param d
	 *            the directory to delete
	 * @throws IOException
	 */
	protected void rmrf(File d) throws IOException {
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

	protected ObjectId createFile(Repository repository, IProject project, String name, String content) throws IOException {
		File file = new File(project.getProject().getLocation().toFile(), name);
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(content);
		fileWriter.close();
		ObjectWriter objectWriter = new ObjectWriter(repository);
		return objectWriter.writeBlob(file);
	}

	protected ObjectId createFileCorruptShort(Repository repository, IProject project, String name, String content) throws IOException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
		Method setWritable = File.class.getMethod("setWritable", new Class[] { boolean.class });
		ObjectId id = createFile(repository, project, name, content);
		File file = new File(repository.getDirectory(), "objects" + id.name().substring(0,2) + "/" + id.name().substring(3));
		setWritable.invoke(file, new Object[] { Boolean.TRUE });
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
		randomAccessFile.setLength(file.length()-1);
		randomAccessFile.close();
		return id;
	}
	protected ObjectId createEmptyTree(Repository repository) throws IOException {
		ObjectWriter objectWriter = new ObjectWriter(repository);
		Tree tree = new Tree(repository);
		return objectWriter.writeTree(tree);
	}
}

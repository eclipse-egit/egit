/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.util.FS;

public class TestUtils {

	public final static String AUTHOR = "The Author <The.author@some.com>";

	public final static String COMMITTER = "The Commiter <The.committer@some.com>";

	/**
	 * This method deletes a file / subtree
	 *
	 * @param d
	 *            file / folder to delete
	 * @throws IOException
	 *             if file can not be deleted
	 */
	public void deleteRecursive(File d) throws IOException {
		if (!d.exists())
			return;

		File[] files = d.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; ++i) {
				if (files[i].isDirectory())
					deleteRecursive(files[i]);
				else if (!files[i].delete())
					throw new IOException(files[i] + " in use or undeletable");
			}
		}
		if (!d.delete())
			throw new IOException(d + " in use or undeletable");
		assert !d.exists();
	}

	/**
	 * Create a "temporary" directory
	 *
	 * @param name
	 *            the name of the directory
	 * @return a directory as child of a "temporary" folder in the user home
	 *         directory; may or may not exist
	 * @throws IOException
	 */
	public File getTempDir(String name) throws IOException {
		File userHome = FS.DETECTED.userHome();
		File rootDir = new File(userHome, "EGitCoreTestTempDir");
		File result = new File(rootDir, name);
		if (result.exists())
			deleteRecursive(result);
		return result;
	}

	/**
	 * Cleanup: delete the "temporary" folder and all children
	 *
	 * @throws IOException
	 */
	public void deleteTempDirs() throws IOException {
		File userHome = FS.DETECTED.userHome();
		File rootDir = new File(userHome, "EGitCoreTestTempDir");
		if (rootDir.exists())
			deleteRecursive(rootDir);
	}

	/**
	 * Read the stream into a String
	 *
	 * @param inputStream
	 * @return the contents of the stream
	 * @throws IOException
	 */
	public String slurpAndClose(InputStream inputStream) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		try {
			int ch;
			while ((ch = inputStream.read()) != -1) {
				stringBuilder.append((char) ch);
			}
		} finally {
			inputStream.close();
		}
		return stringBuilder.toString();
	}

	/**
	 * Add a file to an existing project
	 *
	 * @param project
	 *            the project
	 * @param path
	 *            e.g. "folder1/folder2/test.txt"
	 * @param content
	 *            the contents
	 * @return the file
	 * @throws Exception
	 *             if the file can not be created
	 */
	public IFile addFileToProject(IProject project, String path, String content)
			throws Exception {
		IPath filePath = new Path(path);
		IFolder folder = null;
		for (int i = 0; i < filePath.segmentCount() - 1; i++) {
			if (folder == null) {
				folder = project.getFolder(filePath.segment(i));
			} else {
				folder = folder.getFolder(filePath.segment(i));
			}
			folder.create(false, true, null);
		}
		IFile file = project.getFile(filePath);
		file.create(new ByteArrayInputStream(content.getBytes(project
				.getDefaultCharset())), true, null);
		return file;
	}

	/**
	 * Create a project in the local file system
	 *
	 * @param parentFile
	 *            the parent
	 * @param projectName
	 *            project name
	 * @return the project with a location pointing to the local file system
	 * @throws Exception
	 */
	public IProject createProjectInLocalFileSystem(File parentFile,
			String projectName) throws Exception {
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		if (firstProject.exists())
			firstProject.delete(false, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(projectName);
		desc.setLocation(new Path(new File(parentFile, projectName).getPath()));
		firstProject.create(desc, null);
		firstProject.open(null);
		return firstProject;
	}
}

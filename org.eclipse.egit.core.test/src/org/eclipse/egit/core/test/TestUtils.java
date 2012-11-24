/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;

public class TestUtils {

	public final static String AUTHOR = "The Author <The.author@some.com>";

	public final static String COMMITTER = "The Commiter <The.committer@some.com>";

	/**
	 * Return the base directory in which temporary directories are created.
	 * Current implementation returns a "temporary" folder in the user home.
	 *
	 * @return a "temporary" folder in the user home that may not exist.
	 * @throws IOException
	 */
	public File getBaseTempDir() throws IOException {
		File userHome = FS.DETECTED.userHome();
		File rootDir = new File(userHome, "EGitCoreTestTempDir");
		return rootDir;
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
	public File createTempDir(String name) throws IOException {
		File result = new File(getBaseTempDir(), name);
		if (result.exists())
			FileUtils.delete(result, FileUtils.RECURSIVE | FileUtils.RETRY);
		return result;
	}

	/**
	 * Cleanup: delete the "temporary" folder and all children
	 *
	 * @throws IOException
	 */
	public void deleteTempDirs() throws IOException {
		File rootDir = getBaseTempDir();
		if (rootDir.exists())
			FileUtils.delete(rootDir, FileUtils.RECURSIVE | FileUtils.RETRY);
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
	 * @throws CoreException
	 *             if the file can not be created
	 * @throws UnsupportedEncodingException
	 */
	public IFile addFileToProject(IProject project, String path, String content) throws CoreException, UnsupportedEncodingException {
		IPath filePath = new Path(path);
		IFolder folder = null;
		for (int i = 0; i < filePath.segmentCount() - 1; i++) {
			if (folder == null) {
				folder = project.getFolder(filePath.segment(i));
			} else {
				folder = folder.getFolder(filePath.segment(i));
			}
			if (!folder.exists())
				folder.create(false, true, null);
		}
		IFile file = project.getFile(filePath);
		file.create(new ByteArrayInputStream(content.getBytes(project
				.getDefaultCharset())), true, null);
		return file;
	}

	/**
	 * Change the content of a file
	 *
	 * @param project
	 * @param file
	 * @param newContent
	 * @return the file
	 * @throws CoreException
	 * @throws UnsupportedEncodingException
	 */
	public IFile changeContentOfFile(IProject project, IFile file, String newContent) throws UnsupportedEncodingException, CoreException {
		file.setContents(new ByteArrayInputStream(newContent.getBytes(project
				.getDefaultCharset())), 0, null);
		return file;
	}

	/**
	 * Create a project in the base directory of temp dirs
	 *
	 * @param projectName
	 *            project name
	 * @return the project with a location pointing to the local file system
	 * @throws Exception
	 */
	public IProject createProjectInLocalFileSystem(
			String projectName) throws Exception {
		return createProjectInLocalFileSystem(getBaseTempDir(), projectName);
	}

	/**
	 * Create a project in the local file system
	 *
	 * @param parentFile
	 *            the parent directory
	 * @param projectName
	 *            project name
	 * @return the project with a location pointing to the local file system
	 * @throws Exception
	 */
	public IProject createProjectInLocalFileSystem(File parentFile,
			String projectName) throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);
		if (project.exists()) {
			project.delete(true, null);
		}
		File testFile = new File(parentFile, projectName);
		if (testFile.exists())
			FileUtils.delete(testFile, FileUtils.RECURSIVE | FileUtils.RETRY);

		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(projectName);
		desc.setLocation(new Path(new File(parentFile, projectName).getPath()));
		project.create(desc, null);
		project.open(null);
		return project;
	}

	/**
	 * verifies that repository contains exactly the given files.
	 * @param repository
	 * @param paths
	 * @throws Exception
	 */
	public void assertRepositoryContainsFiles(Repository repository,
			String[] paths) throws Exception {
		Set<String> expectedfiles = new HashSet<String>();
		for (String path : paths)
			expectedfiles.add(path);
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(repository.resolve("HEAD^{tree}"));
		treeWalk.setRecursive(true);
		while (treeWalk.next()) {
			String path = treeWalk.getPathString();
			if (!expectedfiles.contains(path))
				fail("Repository contains unexpected expected file " + path);
			expectedfiles.remove(path);
		}
		if (expectedfiles.size() > 0) {
			StringBuilder message = new StringBuilder(
					"Repository does not contain expected files: ");
			for (String path : expectedfiles) {
				message.append(path);
				message.append(" ");
			}
			fail(message.toString());
		}
	}

	/**
	 * verifies that repository contains exactly the given files with the given
	 * content. Usage example:<br>
	 *
	 * <code>
	 * assertRepositoryContainsFiles(repository, "foo/a.txt", "content of A",
	 *                                           "foo/b.txt", "content of B")
	 * </code>
	 * @param repository
	 * @param args
	 * @throws Exception
	 */
	public void assertRepositoryContainsFilesWithContent(Repository repository,
			String... args) throws Exception {
		HashMap<String, String> expectedfiles = mkmap(args);
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(repository.resolve("HEAD^{tree}"));
		treeWalk.setRecursive(true);
		while (treeWalk.next()) {
			String path = treeWalk.getPathString();
			assertTrue(expectedfiles.containsKey(path));
			ObjectId objectId = treeWalk.getObjectId(0);
			byte[] expectedContent = expectedfiles.get(path).getBytes();
			byte[] repoContent = treeWalk.getObjectReader().open(objectId)
					.getBytes();
			if (!Arrays.equals(repoContent, expectedContent)) {
				fail("File " + path + " has repository content "
						+ new String(repoContent)
						+ " instead of expected content "
						+ new String(expectedContent));
			}
			expectedfiles.remove(path);
		}
		if (expectedfiles.size() > 0) {
			StringBuilder message = new StringBuilder(
					"Repository does not contain expected files: ");
			for (String path : expectedfiles.keySet()) {
				message.append(path);
				message.append(" ");
			}
			fail(message.toString());
		}
	}

	private static HashMap<String, String> mkmap(String... args) {
		if ((args.length % 2) > 0)
			throw new IllegalArgumentException("needs to be pairs");
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < args.length; i += 2) {
			map.put(args[i], args[i+1]);
		}
		return map;
	}

	File getWorkspaceSupplement() throws IOException {
		return createTempDir("wssupplement");
	}

}

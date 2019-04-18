/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;

public class TestUtils {

	public final static String AUTHOR = "The Author <The.author@some.com>";

	public final static String COMMITTER = "The Commiter <The.committer@some.com>";

	private final static File rootDir = customTestDirectory();

	/**
	 * Allow to set a custom directory for running tests
	 *
	 * @return custom directory defined by system property
	 *         {@code egit.test.tmpdir} or {@code ~/egit.test.tmpdir} if this
	 *         property isn't defined
	 */
	private static File customTestDirectory() {
		final String p = System.getProperty("egit.test.tmpdir"); //$NON-NLS-1$
		File testDir = null;
		boolean isDefault = true;
		if (p == null || p.length() == 0)
			testDir = new File(FS.DETECTED.userHome(), "egit.test.tmpdir"); //$NON-NLS-1$
		else {
			isDefault = false;
			testDir = new File(p).getAbsoluteFile();
		}
		System.out.println("egit.test.tmpdir" //$NON-NLS-1$
				+ (isDefault ? "[default]: " : ": ") //$NON-NLS-1$ $NON-NLS-2$
				+ testDir.getAbsolutePath());
		return testDir;
	}

	private File baseTempDir;

	public TestUtils() {
		// ensure that concurrent test runs don't use the same directory
		baseTempDir = new File(rootDir, UUID.randomUUID().toString()
				.replace("-", ""));
	}

	/**
	 * Return the base directory in which temporary directories are created.
	 * Current implementation returns a "temporary" folder in the user home.
	 *
	 * @return a "temporary" folder in the user home that may not exist.
	 */
	public File getBaseTempDir() {
		return baseTempDir;
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
		FileUtils.mkdirs(result, true);
		return result;
	}

	/**
	 * Cleanup: delete the "temporary" folder and all children
	 *
	 * @throws IOException
	 */
	public void deleteTempDirs() throws IOException {
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
	 *             if the file cannot be created
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
		Set<String> expectedfiles = new HashSet<>();
		expectedfiles.addAll(Arrays.asList(paths));
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(repository.resolve("HEAD^{tree}"));
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				String path = treeWalk.getPathString();
				if (!expectedfiles.contains(path))
					fail("Repository contains unexpected expected file "
							+ path);
				expectedfiles.remove(path);
			}
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
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(repository.resolve("HEAD^{tree}"));
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				String path = treeWalk.getPathString();
				assertTrue(expectedfiles.containsKey(path));
				ObjectId objectId = treeWalk.getObjectId(0);
				byte[] expectedContent = expectedfiles.get(path)
						.getBytes("UTF-8");
				byte[] repoContent = treeWalk.getObjectReader().open(objectId)
						.getBytes();
				if (!Arrays.equals(repoContent, expectedContent)) {
					fail("File " + path + " has repository content "
							+ new String(repoContent, "UTF-8")
							+ " instead of expected content "
							+ new String(expectedContent, "UTF-8"));
				}
				expectedfiles.remove(path);
			}
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

	/**
	 * Waits at least 100 milliseconds until no jobs of given family are running
	 *
	 * @param maxWaitTime
	 * @param family
	 * @throws InterruptedException
	 */
	public static void waitForJobs(long maxWaitTime, Object family)
			throws InterruptedException {
		waitForJobs(100, maxWaitTime, family);
	}

	/**
	 * Waits at least <code>minWaitTime</code> milliseconds until no jobs of
	 * given family are running
	 *
	 * @param maxWaitTime
	 * @param minWaitTime
	 * @param family
	 *            can be null which means all job families
	 * @throws InterruptedException
	 */
	public static void waitForJobs(long minWaitTime, long maxWaitTime,
			Object family)
			throws InterruptedException {
		long start = System.currentTimeMillis();
		Thread.sleep(minWaitTime);
		IJobManager jobManager = Job.getJobManager();
		Job[] jobs = jobManager.find(family);
		while (busy(jobs)) {
			Thread.sleep(50);
			jobs = jobManager.find(family);
			if (System.currentTimeMillis() - start > maxWaitTime) {
				if (busy(jobs)) {
					System.out.println("Following jobs were still running: "
							+ getJobNames(jobs));
				}
				return;
			}
		}
	}

	private static String getJobNames(Job[] jobs) {
		StringBuilder sb = new StringBuilder();
		for (Job job : jobs) {
			sb.append(job.getName()).append(" / ").append(job.toString())
					.append(", ");
		}
		return sb.toString();
	}

	private static boolean busy(Job[] jobs) {
		for (Job job : jobs) {
			int state = job.getState();
			if (state == Job.RUNNING || state == Job.WAITING) {
				return true;
			}
		}
		return false;
	}

	private static HashMap<String, String> mkmap(String... args) {
		if ((args.length % 2) > 0)
			throw new IllegalArgumentException("needs to be pairs");
		HashMap<String, String> map = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			map.put(args[i], args[i+1]);
		}
		return map;
	}

	public static String dumpThreads() {
		final StringBuilder dump = new StringBuilder();
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		final ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(
				threadMXBean.isObjectMonitorUsageSupported(),
				threadMXBean.isSynchronizerUsageSupported());
		for (ThreadInfo threadInfo : threadInfos) {
			dump.append("Thread ").append(threadInfo.getThreadId()).append(' ')
					.append(threadInfo.getThreadName()).append(' ')
					.append(threadInfo.getThreadState()).append('\n');
			LockInfo blocked = threadInfo.getLockInfo();
			if (blocked != null) {
				dump.append("  Waiting for ").append(blocked);
				String lockOwner = threadInfo.getLockOwnerName();
				if (lockOwner != null && !lockOwner.isEmpty()) {
					dump.append(" held by ").append(lockOwner).append("(id=")
							.append(threadInfo.getLockOwnerId()).append(')');
				}
				dump.append('\n');
			}
			for (LockInfo lock : threadInfo.getLockedSynchronizers()) {
				dump.append("  Holding ").append(lock).append('\n');
			}
			for (StackTraceElement s : threadInfo.getStackTrace()) {
				dump.append("  at ").append(s).append('\n');
			}
		}
		return dump.toString();
	}

}

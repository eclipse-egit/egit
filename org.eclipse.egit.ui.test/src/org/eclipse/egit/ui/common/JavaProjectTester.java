/*******************************************************************************
 * Copyright (C) 2019, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.test.TestUtils;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jgit.lib.Repository;

public class JavaProjectTester {

	public static final String JAVA_PROJECT_NAME = "javatestProject";

	public static final String SRC_FOLDER_NAME = "src";

	public static final String BIN_FOLDER_NAME = "bin";

	public static final String PACKAGE_NAME = "p";

	public static final String JAVA_CLASS_NAME = "A";

	public static final String JAVA_FILE_NAME = JAVA_CLASS_NAME + ".java";

	public static final String JAVA_FILE_PATH = JAVA_PROJECT_NAME + '/'
			+ SRC_FOLDER_NAME + '/' + PACKAGE_NAME + '/' + JAVA_FILE_NAME;

	public static final String INITIAL_FILE_CONTENT = "package " + PACKAGE_NAME
			+ ";\nclass " + JAVA_CLASS_NAME + " {\n\n}";

	private final LocalRepositoryTestCase testCase;

	public JavaProjectTester(LocalRepositoryTestCase testCase) {
		this.testCase = testCase;
	}

	public static boolean setAutobuild(boolean value) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		boolean isAutoBuilding = desc.isAutoBuilding();
		if (isAutoBuilding != value) {
			desc.setAutoBuilding(value);
			workspace.setDescription(desc);
		}
		return isAutoBuilding;
	}

	public IJavaProject createJavaProjectAndCommitToRepository()
			throws Exception {
		Repository myRepository = testCase
				.createLocalTestRepository(LocalRepositoryTestCase.REPO1);
		File gitDir = myRepository.getDirectory();
		IJavaProject jProject = createJavaProject(myRepository,
				JAVA_PROJECT_NAME);
		IProject project = jProject.getProject();
		try {
			new ConnectProviderOperation(project, gitDir).execute(null);
		} catch (Exception e) {
			Activator.logError("Failed to connect project to repository", e);
		}
		testCase.assertConnected(project);
		// Check in at least the java file
		IFolder folder = project.getFolder(SRC_FOLDER_NAME)
				.getFolder(PACKAGE_NAME);
		IFile file = folder.getFile(JAVA_FILE_NAME);

		IFile[] committableFiles = new IFile[] { file };
		ArrayList<IFile> untracked = new ArrayList<>();
		untracked.addAll(Arrays.asList(committableFiles));
		// commit to master
		CommitOperation op = new CommitOperation(committableFiles, untracked,
				TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER, "Initial commit");
		op.execute(null);

		// Make sure cache entry is already listening for changes
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		cache.getIndexDiffCacheEntry(Activator.getDefault().getRepositoryCache()
				.lookupRepository(gitDir));
		return jProject;
	}

	private IJavaProject createJavaProject(final Repository repository,
			final String projectName) throws Exception {
		final IJavaProject[] jProjectHolder = new IJavaProject[] { null };
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				IProject project = root.getProject(projectName);
				if (project.exists()) {
					project.delete(true, null);
					TestUtil.waitForJobs(100, 5000);
				}
				IProjectDescription desc = ResourcesPlugin.getWorkspace()
						.newProjectDescription(projectName);
				desc.setLocation(
						new Path(new File(repository.getWorkTree(), projectName)
								.getPath()));
				project.create(desc, null);
				project.open(null);
				TestUtil.waitForJobs(50, 5000);
				// Create a "bin" folder
				IFolder bin = project.getFolder(BIN_FOLDER_NAME);
				if (!bin.exists()) {
					bin.create(IResource.FORCE | IResource.DERIVED, true, null);
				}
				IPath outputLocation = bin.getFullPath();
				// Create a "src" folder
				IFolder src = project.getFolder(SRC_FOLDER_NAME);
				if (!src.exists()) {
					src.create(IResource.FORCE, true, null);
				}
				addNatureToProject(project, JavaCore.NATURE_ID);
				// Set up the IJavaProject
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot srcContainer = jProject
						.getPackageFragmentRoot(src);
				IClasspathEntry srcEntry = JavaCore
						.newSourceEntry(srcContainer.getPath());
				// Create a JRE classpath entry using the default JRE
				IClasspathEntry jreEntry = JavaRuntime
						.getDefaultJREContainerEntry();
				jProject.setRawClasspath(
						new IClasspathEntry[] { srcEntry, jreEntry },
						outputLocation, true, null);
				// Create a package with a single test class
				IPackageFragment javaPackage = srcContainer
						.createPackageFragment(PACKAGE_NAME, true, null);
				javaPackage
						.createCompilationUnit(JAVA_FILE_NAME,
								INITIAL_FILE_CONTENT, true, null);
				jProjectHolder[0] = jProject;
			}
		};
		ResourcesPlugin.getWorkspace().run(runnable, null);
		return jProjectHolder[0];
	}

	private void addNatureToProject(IProject proj, String natureId)
			throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, null);
	}

	public void removeJavaProject(IJavaProject javaProject)
			throws CoreException {
		if (javaProject == null) {
			return;
		}
		final IProject project = javaProject.getProject();
		TestUtils.deleteProject(project);
	}

}

/*******************************************************************************
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.actions.ShowBlameActionHandler;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * JUnit plugin test (non-SWTBot) to verify that {@link ShowBlameActionHandler}
 * is enabled not only for {@link IResource} but also for other things that
 * might be visible in the package explorer and that adapt to {@link IResource}.
 * We verify by setting up a Java project with a simple test class and then
 * checking for {@link IFile}, {@link ICompilationUnit}, and {@link IType}.
 *
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=401156">Bug
 *      401156</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ShowBlameActionHandlerTest extends LocalRepositoryTestCase {

	private static final String JAVA_PROJECT_NAME = "javatestProject";

	private static final String SRC_FOLDER_NAME = "src";

	private static final String BIN_FOLDER_NAME = "bin";

	private static final String PACKAGE_NAME = "p";

	private static final String JAVA_CLASS_NAME = "A";

	private static final String JAVA_FILE_NAME = JAVA_CLASS_NAME + ".java";

	private static final int MAX_DELETE_RETRY = 5;

	private static final int DELETE_RETRY_DELAY = 1000; // ms

	private static boolean initialAutobuild;

	private IJavaProject javaProject = null;

	@BeforeClass
	public static void setupAutobuildOff() throws CoreException {
		// Switch off autobuild -- we don't need it, and a build job might
		// interfere with our removing the Java project at the end.
		initialAutobuild = setAutobuild(false);
	}

	@AfterClass
	public static void teardownAutobuildReset() throws CoreException {
		setAutobuild(initialAutobuild);
	}

	@Before
	public void setup() throws Exception {
		javaProject = createJavaProjectAndCommitToRepository();
	}

	@After
	public void teardown() throws CoreException {
		removeJavaProject();
	}

	@Test
	public void testShowAnnotationsFromProjectExplorer() throws Exception {
		IProject project = javaProject.getProject();
		// Find the file
		IFile file = project.getFolder(SRC_FOLDER_NAME).getFolder(PACKAGE_NAME)
				.getFile(JAVA_FILE_NAME);
		assertBlameEnabled(file, true);
		// Now repeat the same with the ICompilationUnit.
		IJavaElement element = JavaCore.create(file, javaProject);
		assertTrue("Expected an ICompilationUnit",
				element instanceof ICompilationUnit);
		assertBlameEnabled(element, true);
		// And with IType...
		IType type = javaProject.findType(PACKAGE_NAME, JAVA_CLASS_NAME);
		assertBlameEnabled(type, true);
		// ... and finally with something that doesn't adapt to IResource:
		assertBlameEnabled(this, false);
	}

	@SuppressWarnings("boxing")
	private void assertBlameEnabled(Object selected, boolean expected) {
		assertNotNull("Nothing selected", selected);
		IStructuredSelection selection = mock(IStructuredSelection.class);
		when(selection.getFirstElement()).thenReturn(selected);
		when(selection.size()).thenReturn(1);
		ShowBlameActionHandler blame = new ShowBlameActionHandler();
		blame.setSelection(selection);
		assertEquals("Unexpected enablement of blame action", expected,
				blame.isEnabled());
	}

	// Java stuff below

	private static boolean setAutobuild(boolean value) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		boolean isAutoBuilding = desc.isAutoBuilding();
		if (isAutoBuilding != value) {
			desc.setAutoBuilding(value);
			workspace.setDescription(desc);
		}
		return isAutoBuilding;
	}

	private IJavaProject createJavaProjectAndCommitToRepository()
			throws Exception {
		Repository myRepository = createLocalTestRepository(REPO1);
		File gitDir = myRepository.getDirectory();
		IJavaProject jProject = createJavaProject(myRepository,
				JAVA_PROJECT_NAME);
		IProject project = jProject.getProject();
		try {
			new ConnectProviderOperation(project, gitDir).execute(null);
		} catch (Exception e) {
			Activator.logError("Failed to connect project to repository", e);
		}
		assertConnected(project);
		// Check in at least the java file
		IFolder folder = project.getFolder(SRC_FOLDER_NAME)
				.getFolder(PACKAGE_NAME);
		IFile file = folder.getFile(JAVA_FILE_NAME);

		IFile[] commitables = new IFile[] { file };
		ArrayList<IFile> untracked = new ArrayList<IFile>();
		untracked.addAll(Arrays.asList(commitables));
		// commit to master
		CommitOperation op = new CommitOperation(commitables, untracked,
				TestUtil.TESTAUTHOR, TestUtil.TESTCOMMITTER, "Initial commit");
		op.execute(null);

		// Make sure cache entry is already listening for changes
		IndexDiffCache cache = Activator.getDefault().getIndexDiffCache();
		cache.getIndexDiffCacheEntry(lookupRepository(gitDir));
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
								"package " + PACKAGE_NAME + ";\nclass "
										+ JAVA_CLASS_NAME + " {\n\n}",
								true, null);
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

	private void removeJavaProject() throws CoreException {
		if (javaProject == null) {
			return;
		}
		final IProject project = javaProject.getProject();
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				// Following code inspired by {@link
				// org.eclipse.jdt.testplugin.JavaProjectHelper#delete(IResource)}.
				// I don't like all this sleeping at all, but apparently it's
				// needed because the Java indexer might still run and hold on
				// to some resources.
				for (int i = 0; i < MAX_DELETE_RETRY; i++) {
					try {
						project.delete(
								IResource.FORCE
										| IResource.ALWAYS_DELETE_PROJECT_CONTENT,
								null);
						break;
					} catch (CoreException e) {
						if (i == MAX_DELETE_RETRY - 1) {
							throw e;
						}
						try {
							Activator.logInfo(
									"Sleep before retrying to delete project "
											+ project.getLocationURI());
							// Give other threads the time to close and release
							// the resource.
							Thread.sleep(DELETE_RETRY_DELAY);
						} catch (InterruptedException e1) {
							// Ignore and retry to delete
						}
					}
				}

			}
		};
		ResourcesPlugin.getWorkspace().run(runnable, null);
	}
}

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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.common.JavaProjectTester;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.actions.ShowBlameActionHandler;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

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

	private static boolean initialAutobuild;

	private IJavaProject javaProject = null;

	private JavaProjectTester javaTestHelper;

	@BeforeClass
	public static void setupAutobuildOff() throws CoreException {
		// Switch off autobuild -- we don't need it, and a build job might
		// interfere with our removing the Java project at the end.
		initialAutobuild = JavaProjectTester.setAutobuild(false);
	}

	@AfterClass
	public static void teardownAutobuildReset() throws CoreException {
		JavaProjectTester.setAutobuild(initialAutobuild);
	}

	@Before
	public void setup() throws Exception {
		javaTestHelper = new JavaProjectTester(this);
		javaProject = javaTestHelper.createJavaProjectAndCommitToRepository();
	}

	@After
	public void teardown() throws CoreException {
		javaTestHelper.removeJavaProject(javaProject);
	}

	@Test
	public void testShowAnnotationsFromProjectExplorer() throws Exception {
		IProject project = javaProject.getProject();
		// Find the file
		IFile file = project.getFolder(JavaProjectTester.SRC_FOLDER_NAME)
				.getFolder(JavaProjectTester.PACKAGE_NAME)
				.getFile(JavaProjectTester.JAVA_FILE_NAME);
		assertBlameEnabled(file, true);
		// Now repeat the same with the ICompilationUnit.
		IJavaElement element = JavaCore.create(file, javaProject);
		assertTrue("Expected an ICompilationUnit",
				element instanceof ICompilationUnit);
		assertBlameEnabled(element, true);
		// And with IType...
		IType type = javaProject.findType(JavaProjectTester.PACKAGE_NAME,
				JavaProjectTester.JAVA_CLASS_NAME);
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

}

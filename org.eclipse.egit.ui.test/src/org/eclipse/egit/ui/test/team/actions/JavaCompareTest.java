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
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.common.CompareEditorTester;
import org.eclipse.egit.ui.common.JavaProjectTester;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JavaCompareTest extends LocalRepositoryTestCase {

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
	public void testJavaCompareWithIndex() throws Exception {
		IProject project = javaProject.getProject();
		IFile file = project.getFolder(JavaProjectTester.SRC_FOLDER_NAME)
				.getFolder(JavaProjectTester.PACKAGE_NAME)
				.getFile(JavaProjectTester.JAVA_FILE_NAME);
		String newContent = JavaProjectTester.INITIAL_FILE_CONTENT
				+ "\n// Comment\n";
		ResourcesPlugin.getWorkspace().run(monitor -> {
			try (InputStream s = new ByteArrayInputStream(
					newContent.getBytes(StandardCharsets.UTF_8))) {
				file.setContents(s, IResource.FORCE, monitor);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, file, IResource.NONE, null);
		TestUtil.joinJobs(JobFamilies.INDEX_DIFF_CACHE_UPDATE);
		// Select the project to ensure the staging view does have a repo.
		SWTBotTree projectExplorerTree = TestUtil.getExplorerTree();
		getProjectItem(projectExplorerTree, project.getName()).select();
		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.compareWithIndex(JavaProjectTester.JAVA_FILE_PATH);
		CompareEditorTester editor = CompareEditorTester
				.forTitleContaining(JavaProjectTester.JAVA_FILE_NAME);
		String actualContent = editor.getLeftEditor().getText();
		boolean isDirty = editor.isDirty();
		if (isDirty) {
			editor.save();
		}
		editor.close();
		assertEquals(newContent, actualContent);
		assertFalse(isDirty);
	}
}

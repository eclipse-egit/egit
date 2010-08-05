/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test.op;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.core.test.DualRepositoryTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DiscardChangesOperationTest extends DualRepositoryTestCase {

	File workdir;

	IProject project;

	String projectName = "DiscardChangesTest";

	@Before
	public void setUp() throws Exception {

		workdir = testUtils.createTempDir("Repository1");

		repository1 = new TestRepository(new File(workdir, Constants.DOT_GIT));

		// now we create a project in repo1
		project = testUtils
				.createProjectInLocalFileSystem(workdir, projectName);
		testUtils.addFileToProject(project, "folder1/file1.txt", "Hello world");

		repository1.connect(project);

		project.accept(new IResourceVisitor() {

			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile) {
					try {
						repository1
								.track(EFS.getStore(resource.getLocationURI())
										.toLocalFile(0, null));
					} catch (IOException e) {
						throw new CoreException(Activator.error(e.getMessage(),
								e));
					}
				}
				return true;
			}
		});
		repository1.commit("Initial commit");
	}

	@After
	public void tearDown() throws Exception {
		project.close(null);
		project.delete(false, false, null);
		repository1.dispose();
		repository1 = null;
		testUtils.deleteTempDirs();
	}

	@Test
	public void testDiscardChanges() throws Exception {
		IFile file = project.getFile(new Path("folder1/file1.txt"));
		String contents = testUtils.slurpAndClose(file.getContents());
		assertEquals("Hello world", contents);

		file.setContents(new ByteArrayInputStream("Changed".getBytes(project
				.getDefaultCharset())), 0, null);

		contents = testUtils.slurpAndClose(file.getContents());
		assertEquals("Changed", contents);

		DiscardChangesOperation dcop = new DiscardChangesOperation(
				new IResource[] { file });
		dcop.execute(new NullProgressMonitor());

		contents = testUtils.slurpAndClose(file.getContents());
		assertEquals("Hello world", contents);

	}
}

/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.performance;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.PROJ1;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.REPO1;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.getTestDirectory;
import static org.eclipse.jgit.lib.Constants.DOT_GIT;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.storage.file.FileRepository;

public class SynchronizeWithStagedChangesPerformanceTest extends
		AbstractSynchronizeViewPerformanceTest {

	public SynchronizeWithStagedChangesPerformanceTest() {
		super(true);
	}

	protected void fillRepository() throws Exception {
		File repoRoot = new File(getTestDirectory(), REPO1);
		FileRepository db = new FileRepository(new File(repoRoot, DOT_GIT));

		// create some files and commit them
		int fileCount = 300;
		Git git = new Git(db);
		IProject project = getWorkspace().getRoot().getProject(PROJ1);
		for (int i = 0; i < fileCount; i++) {
			String name = "t" + i;
			IFile file = project.getFile(name);
			byte[] content = ("x " + i).getBytes();
			file.create(new ByteArrayInputStream(content), true, null);
			git.add().addFilepattern(PROJ1 + "/" + name).call();
		}
		git.commit().setMessage("initial commit").call();

		// modify and stage existing files
		ByteArrayInputStream content = new ByteArrayInputStream("u".getBytes());
		for (int i = fileCount / 2; i < fileCount; i++) {
			String name = "t" + i;
			project.getFile(name).appendContents(content, true, false, null);
			git.add().addFilepattern(PROJ1 + "/" + name).call();
		}

		// stage some new files
		int newFiles = 150;
		for (int i = 0; i < newFiles; i++) {
			String name = "n" + i;
			IFile newFile = project.getFile(name);
			newFile.create(content, true, null);
			git.add().addFilepattern(PROJ1 + "/" + name).call();
		}
	}

}

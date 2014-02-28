/*******************************************************************************
 * Copyright (C) 2015 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.variants.IResourceVariant;
import org.junit.After;
import org.junit.Before;

public abstract class VariantsTestCase extends GitTestCase {
	protected final String INITIAL_CONTENT_1 = "some content for the first file";

	protected final String INITIAL_CONTENT_2 = "some content for the second file";

	protected static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	protected static final String BRANCH = Constants.R_HEADS + "branch";

	protected Repository repo;

	protected IProject iProject;

	protected TestRepository testRepo;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		new Git(repo).commit().setAuthor("JUnit", "junit@jgit.org")
				.setMessage("Initial commit").call();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;

		super.tearDown();
	}

	protected RevCommit setContentsAndCommit(TestRepository testRepository,
			IFile targetFile, String newContents, String commitMessage)
			throws Exception {
		targetFile.setContents(
				new ByteArrayInputStream(newContents.getBytes()),
				IResource.FORCE, new NullProgressMonitor());
		testRepository.addToIndex(targetFile);
		return testRepository.commit(commitMessage);
	}

	protected void assertContentEquals(IResourceVariant variant,
			String expectedContents) throws Exception {
		Scanner scanner = null;
		try {
			final IStorage storage = variant
					.getStorage(new NullProgressMonitor());
			scanner = new Scanner(storage.getContents()).useDelimiter("\\A");

			String fileContent = "";
			if (scanner.hasNext()) {
				fileContent = scanner.next();
			}

			assertEquals(expectedContents, fileContent);
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}
}

/*******************************************************************************
 * Copyright (C) 2015, Laurent Delaigue <laurent.delaigue@obeo.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.core.test.models.ModelTestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CompareUtilsTest extends ModelTestCase {

	private static final String MASTER = Constants.R_HEADS + Constants.MASTER;

	private static final String BRANCH = Constants.R_HEADS + "branch";

	private Repository repo;

	private IProject iProject;

	private TestRepository testRepo;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		iProject = project.project;
		testRepo = new TestRepository(gitDir);
		testRepo.connect(iProject);
		repo = RepositoryMapping.getMapping(iProject).getRepository();

		// make initial commit
		try (Git git = new Git(repo)) {
			git.commit().setAuthor("JUnit", "junit@jgit.org")
					.setMessage("Initial commit").call();
		}
	}

	@After
	public void clearGitResources() throws Exception {
		testRepo.disconnect(iProject);
		testRepo.dispose();
		repo = null;
		super.tearDown();
	}

	/**
	 * All files with sample file extension that are under the same parent are
	 * linked together (@see
	 * {@link org.eclipse.egit.core.test.models.SampleResourceMapping}). Thus,
	 * the canDirectlyOpenInCompare should not return true when two sample file
	 * have been modified between two commits.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCanDirectlyOpenInCompare() throws Exception {
		File file1 = testRepo.createFile(iProject, "file1."
				+ SAMPLE_FILE_EXTENSION);
		File file2 = testRepo.createFile(iProject, "file2."
				+ SAMPLE_FILE_EXTENSION);

		String initialContent1 = "some content for the first file";
		String initialContent2 = "some content for the second file";
		testRepo.appendContentAndCommit(iProject, file1, initialContent1,
				"first file - initial commit");
		testRepo.appendContentAndCommit(iProject, file2, initialContent2,
				"second file - initial commit");

		IFile iFile1 = testRepo.getIFile(iProject, file1);
		IFile iFile2 = testRepo.getIFile(iProject, file2);

		testRepo.createAndCheckoutBranch(MASTER, BRANCH);

		final String branchChanges = "branch changes\n";
		iFile1.setContents(new ByteArrayInputStream(
				(branchChanges + initialContent1).getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());
		iFile2.setContents(new ByteArrayInputStream(
				(branchChanges + initialContent2).getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());
		testRepo.addToIndex(iFile1);
		testRepo.addToIndex(iFile2);
		testRepo.commit("branch commit");

		testRepo.checkoutBranch(MASTER);

		final String masterChanges = "some changes\n";
		iFile1.setContents(new ByteArrayInputStream(
				(initialContent1 + masterChanges).getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());
		iFile2.setContents(new ByteArrayInputStream(
				(initialContent2 + masterChanges).getBytes("UTF-8")),
				IResource.FORCE, new NullProgressMonitor());
		testRepo.addToIndex(iFile1);
		testRepo.addToIndex(iFile2);
		testRepo.commit("master commit");

		iProject.refreshLocal(IResource.DEPTH_INFINITE,
				new NullProgressMonitor());
		// end setup

		IMergeContext modelContext = prepareModelContext(repo, iFile1, MASTER,
				BRANCH);
		if (modelContext instanceof GitSubscriberMergeContext) {
			ISynchronizationScopeManager manager = ((GitSubscriberMergeContext) modelContext)
					.getScopeManager();
			if (manager instanceof SubscriberScopeManager) {
				ResourceMappingContext context = ((SubscriberScopeManager) manager)
						.getContext();
				boolean canDirectlyOpenInCompare = CompareUtils
						.canDirectlyOpenInCompare(iFile1, context);
				assertFalse(canDirectlyOpenInCompare);
			}
		}
	}

}

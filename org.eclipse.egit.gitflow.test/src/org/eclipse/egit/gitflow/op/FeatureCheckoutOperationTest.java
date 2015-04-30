/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureCheckoutOperationTest extends AbstractFeatureOperationTest {
	@Test
	public void testFeatureCheckout() throws Exception {
		testRepository
				.createInitialCommit("testFeatureCheckout\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		new BranchOperation(repository, gfRepo.getDevelop()).execute(null);

		new FeatureCheckoutOperation(gfRepo, MY_FEATURE).execute(null);

		assertEquals(gfRepo.getFullFeatureBranchName(MY_FEATURE),
				repository.getFullBranch());
	}

	@Test
	public void testFeatureCheckoutConflicts() throws Exception {
		testRepository
				.createInitialCommit("testFeatureCheckoutConflicts\n\nfirst commit\n");

		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		GitFlowRepository gfRepo = new GitFlowRepository(repository);

		// setup something we can modify later
		IFile file = testUtils.addFileToProject(project.getProject(),
				"folder1/file1.txt", "Hello world");
		testRepository.connect(project.getProject());
		testRepository.trackAllFiles(project.getProject());
		testRepository.commit("Initial commit");

		new FeatureStartOperation(gfRepo, MY_FEATURE).execute(null);
		// modify on first branch
		testUtils.changeContentOfFile(project.getProject(), file,
				"Hello Feature");
		testRepository.addToIndex(file);
		testRepository.commit("Feature commit");
		new BranchOperation(repository, gfRepo.getDevelop()).execute(null);
		assertEquals(gfRepo.getDevelopFull(), repository.getFullBranch());

		// modify on second branch
		testUtils.changeContentOfFile(project.getProject(), file,
				"Hello Develop");
		testRepository.addToIndex(file);

		FeatureCheckoutOperation featureCheckoutOperation = new FeatureCheckoutOperation(
				gfRepo, MY_FEATURE);
		featureCheckoutOperation.execute(null);

		assertEquals(Status.CONFLICTS, featureCheckoutOperation.getResult()
				.getStatus());
		assertEquals(gfRepo.getDevelopFull(), repository.getFullBranch());
	}
}

/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.performance;

import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.PROJ1;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.afterClassBase;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.beforeClassBase;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.createProjectAndCommitToRepository;
import static org.eclipse.egit.ui.performance.LocalRepositoryTestCase.lookupRepository;
import static org.eclipse.jface.dialogs.MessageDialogWithToggle.NEVER;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.eclipse.team.internal.ui.IPreferenceIds.SYNCHRONIZING_COMPLETE_PERSPECTIVE;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronize;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.test.performance.PerformanceTestCase;

public abstract class AbstractSynchronizeViewPerformanceTest extends
		PerformanceTestCase {

	private static final String INITIAL_TAG = "v0.0";

	protected GitSynchronizeData synchronizeData;

	protected IResource[] resources;

	private final boolean includeLocal;

	public AbstractSynchronizeViewPerformanceTest(boolean includeLocal) {
		this.includeLocal = includeLocal;
	}

	public void testCheckSynchronizeViewPerformance() throws Exception {
		for (int i = 0; i < 15; i++) {
			startMeasuring();
			runSynchronizeView();
			stopMeasuring();
		}
		commitMeasurements();
		assertPerformance();
	}

	@SuppressWarnings("restriction") @Override protected void setUp()
			throws Exception {
		super.setUp();
		beforeClassBase();
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(
						SYNCHRONIZING_COMPLETE_PERSPECTIVE, NEVER);

		File repositoryFile = createProjectAndCommitToRepository();
		FileRepository repository = lookupRepository(repositoryFile);
		new Git(repository).tag().setMessage(INITIAL_TAG)
				.setName(INITIAL_TAG).call();

		fillRepository();

		if (includeLocal)
			synchronizeData = new GitSynchronizeData(repository,
					HEAD, HEAD, true);
		else
			synchronizeData = new GitSynchronizeData(
					repository, R_TAGS + INITIAL_TAG,
					HEAD, false);

		resources = new IResource[] { ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1) };
	}

	@Override protected void tearDown() throws Exception {
		afterClassBase();
		super.tearDown();
	}

	protected void runSynchronizeView() throws Exception {
		GitModelSynchronize.launch(synchronizeData, resources);
		Job.getJobManager().join(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, null);
	}

	protected abstract void fillRepository() throws Exception;

}

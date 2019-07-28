/*******************************************************************************
 * Copyright (C) 2019, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.GitFlowOperation;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;

class PostBranchCheckoutUiTask extends JobChangeAdapter {

	private final GitFlowRepository gfRepo;
	private final String fullBranchName;
	private final GitFlowOperation operation;

	PostBranchCheckoutUiTask(GitFlowRepository gfRepo,
			String fullBranchName, GitFlowOperation operation) {
		this.gfRepo = gfRepo;
		this.fullBranchName = fullBranchName;
		this.operation = operation;
	}

	@Override
	public void done(IJobChangeEvent jobChangeEvent) {
		BranchOperationUI.handleSingleRepositoryCheckoutOperationResult(
				gfRepo.getRepository(),
				operation.getCheckoutResult(),
				fullBranchName);
	}
}

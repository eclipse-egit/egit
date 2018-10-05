/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.lib.Repository;

/**
 * git flow feature checkout
 */
public final class FeatureCheckoutOperation extends AbstractFeatureOperation {
	private CheckoutResult result;

	/**
	 * @param repository
	 * @param featureName
	 */
	public FeatureCheckoutOperation(GitFlowRepository repository,
			String featureName) {
		super(repository, featureName);
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String branchName = repository.getConfig().getFeatureBranchName(featureName);

		boolean dontCloseProjects = false;
		Repository gitRepo = repository.getRepository();
		BranchOperation branchOperation = new BranchOperation(
				gitRepo, branchName, dontCloseProjects);
		branchOperation.execute(monitor);
		result = branchOperation.getResult(gitRepo);
	}

	/**
	 * @return result set after operation was executed
	 */
	public CheckoutResult getResult() {
		return result;
	}
}

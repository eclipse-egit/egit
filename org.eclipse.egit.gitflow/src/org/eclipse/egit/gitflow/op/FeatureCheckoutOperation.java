/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.CheckoutResult;

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
		BranchOperation branchOperation = new BranchOperation(
				repository.getRepository(), branchName, dontCloseProjects);
		branchOperation.execute(monitor);
		result = branchOperation.getResult();
	}

	/**
	 * @return result set after operation was executed
	 */
	public CheckoutResult getResult() {
		return result;
	}
}

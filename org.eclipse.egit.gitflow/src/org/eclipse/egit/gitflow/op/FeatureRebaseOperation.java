/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.RebaseOperation;

import static org.eclipse.egit.gitflow.Activator.error;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * git flow feature rebase
 */
public final class FeatureRebaseOperation extends GitFlowOperation {
	private RebaseResult operationResult;

	/**
	 * @param repository
	 */
	public FeatureRebaseOperation(GitFlowRepository repository) {
		super(repository);
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			if (!repository.isFeature()) {
				throw new WrongGitFlowStateException(
						CoreText.FeatureRebaseOperation_notOnAFeatureBranch);
			}

			Repository jgitRepo = repository.getRepository();
			Ref develop = jgitRepo.exactRef(repository.getConfig().getDevelopFull());
			RebaseOperation op = new RebaseOperation(jgitRepo, develop);
			op.execute(monitor);

			operationResult = op.getResult();
		} catch (WrongGitFlowStateException | IOException e) {
			throw new CoreException(error(e.getMessage(), e));
		}
	}

	/**
	 * @return result set after operation was executed
	 */
	public RebaseResult getOperationResult() {
		return operationResult;
	}
}

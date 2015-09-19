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
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;

/**
 * git flow feature finish
 */
public final class FeatureFinishOperation extends AbstractFeatureOperation {
	private boolean squash = false;

	private boolean keepBranch = false;

	/**
	 * Finish given feautre.
	 *
	 * @param repository
	 * @param featureName
	 */
	public FeatureFinishOperation(GitFlowRepository repository,
			String featureName) {
		super(repository, featureName);
	}

	/**
	 * Finish current feature.
	 *
	 * @param repository
	 * @throws CoreException
	 * @throws WrongGitFlowStateException
	 * @throws IOException
	 */
	public FeatureFinishOperation(GitFlowRepository repository)
			throws CoreException, WrongGitFlowStateException, IOException {
		this(repository, getFeatureName(repository));
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		finish(monitor, repository.getConfig().getFeatureBranchName(featureName), squash, keepBranch, true);
	}

	/**
	 * @param squash
	 * @since 4.1
	 */
	public void setSquash(boolean squash) {
		this.squash = squash;
	}

	/**
	 * @param keepBranch Whether or not the branch will be kept after the operation is finished
	 * @since 4.1
	 */
	public void setKeepBranch(boolean keepBranch) {
		this.keepBranch = keepBranch;
	}

}

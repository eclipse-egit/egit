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

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;

/**
 * Common logic for feature branch operations.
 */
abstract public class AbstractFeatureOperation extends GitFlowOperation {
	/** */
	protected String featureName;

	/**
	 * @param repository
	 * @param featureName
	 */
	public AbstractFeatureOperation(GitFlowRepository repository,
			String featureName) {
		super(repository);
		this.featureName = featureName;
	}

	/**
	 * @param repository
	 * @return current feature branch name
	 * @throws WrongGitFlowStateException
	 * @throws CoreException
	 * @throws IOException
	 */
	protected static String getFeatureName(GitFlowRepository repository)
			throws WrongGitFlowStateException, CoreException, IOException {
		String currentBranch = repository.getRepository().getBranch();
		if (currentBranch == null || !repository.isFeature()) {
			throw new WrongGitFlowStateException(
					CoreText.AbstractFeatureOperation_notOnAFeatureBranch);
		}
		return currentBranch.substring(repository.getConfig()
				.getFeaturePrefix().length());
	}

}

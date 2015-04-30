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
		if (!repository.isFeature()) {
			throw new WrongGitFlowStateException(
					CoreText.AbstractFeatureOperation_notOnAFeautreBranch);
		}
		String currentBranch = repository.getRepository().getBranch();
		return currentBranch.substring(repository.getFeaturePrefix().length());
	}

}

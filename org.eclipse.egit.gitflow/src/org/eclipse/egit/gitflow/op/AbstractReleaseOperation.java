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
 * Common logic for release branch operations.
 */
abstract public class AbstractReleaseOperation extends
		AbstractVersionFinishOperation {
	/**
	 * @param repository
	 * @param releaseName
	 */
	public AbstractReleaseOperation(GitFlowRepository repository,
			String releaseName) {
		super(repository, releaseName);
	}

	/**
	 * @param repository
	 * @return current release branch name
	 * @throws WrongGitFlowStateException
	 * @throws CoreException
	 * @throws IOException
	 */
	protected static String getReleaseName(GitFlowRepository repository)
			throws WrongGitFlowStateException, CoreException, IOException {
		if (!repository.isRelease()) {
			throw new WrongGitFlowStateException(
					CoreText.AbstractReleaseOperation_notOnAReleaseBranch);
		}
		String currentBranch = repository.getRepository().getBranch();
		return currentBranch.substring(repository.getReleasePrefix().length());
	}
}

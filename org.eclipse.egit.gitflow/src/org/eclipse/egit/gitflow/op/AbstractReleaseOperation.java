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
		String currentBranch = repository.getRepository().getBranch();
		if (currentBranch == null || !repository.isRelease()) {
			throw new WrongGitFlowStateException(
					CoreText.AbstractReleaseOperation_notOnAReleaseBranch);
		}
		return currentBranch.substring(repository.getConfig().getReleasePrefix().length());
	}
}

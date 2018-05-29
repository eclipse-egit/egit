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
 * Common logic for hotfix branch operations.
 */
abstract public class AbstractHotfixOperation extends
		AbstractVersionFinishOperation {
	/**
	 * @param repository
	 * @param hotfixName
	 */
	public AbstractHotfixOperation(GitFlowRepository repository,
			String hotfixName) {
		super(repository, hotfixName);
	}

	/**
	 * @param repository
	 * @return current hotfix branch name
	 * @throws WrongGitFlowStateException
	 * @throws CoreException
	 * @throws IOException
	 */
	protected static String getHotfixName(GitFlowRepository repository)
			throws WrongGitFlowStateException, CoreException, IOException {
		String currentBranch = repository.getRepository().getBranch();
		if (currentBranch == null || !repository.isHotfix()) {
			throw new WrongGitFlowStateException(
					CoreText.AbstractHotfixOperation_notOnAHotfixBranch);
		}
		return currentBranch.substring(repository.getConfig().getHotfixPrefix().length());
	}
}

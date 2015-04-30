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
		if (!repository.isHotfix()) {
			throw new WrongGitFlowStateException(
					CoreText.AbstractHotfixOperation_notOnAHotfixBranch);
		}
		String currentBranch = repository.getRepository().getBranch();
		return currentBranch.substring(repository.getHotfixPrefix().length());
	}
}

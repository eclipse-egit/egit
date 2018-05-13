/*******************************************************************************
 * Copyright (C) 2019, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.validation;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jface.dialogs.IInputValidator;

/**
 * Validates Gitflow branch names.
 */
abstract public class BranchNameInputValidator implements IInputValidator {

	/**
	 * Gitflow repository to perform validation on.
	 */
	protected final GitFlowRepository repository;

	/**
	 * @param repository
	 *            Gitflow repository to perform validation on.
	 */
	protected BranchNameInputValidator(GitFlowRepository repository) {
		this.repository = repository;
	}

	@Override
	public String isValid(String newText) {
		String fullBranchName = getFullBranchName(newText);
		IStatus status = Utils.validateNewRefName(fullBranchName,
				repository.getRepository(), "", //$NON-NLS-1$
				false);

		if (status.isOK()) {
			return null;
		}

		return status.getMessage();
	}

	/**
	 *
	 * @param gitFlowBranch
	 *            The name segment of e.g. a feature branch.
	 * @return Full branch name of the given gitFlowBranch. E.g.:
	 *         refs/heads/feature/name
	 */
	abstract protected String getFullBranchName(String gitFlowBranch);
}

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

import org.eclipse.egit.gitflow.GitFlowRepository;

/**
 * Validate release branch name.
 */
public class ReleaseNameValidator extends BranchNameInputValidator {

	/**
	 * @param repository
	 */
	public ReleaseNameValidator(GitFlowRepository repository) {
		super(repository);
	}

	@Override
	protected String getFullBranchName(String newText) {
		return repository.getConfig().getReleaseBranchName(newText);
	}
}

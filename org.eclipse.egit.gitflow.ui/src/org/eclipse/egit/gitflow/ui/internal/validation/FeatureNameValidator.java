/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.validation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.gitflow.BranchNameValidator;
import org.eclipse.egit.gitflow.GitFlowRepository;

/**
 * Validate feature branch name.
 */
public class FeatureNameValidator extends BranchNameInputValidator {
	private final GitFlowRepository repository;

	/**
	 * @param gfRepo
	 */
	public FeatureNameValidator(GitFlowRepository gfRepo) {
		this.repository = gfRepo;
	}

	@Override
	protected boolean branchExists(String newText) throws CoreException {
		return BranchNameValidator.featureExists(repository, newText);
	}
}

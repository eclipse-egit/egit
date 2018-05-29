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
package org.eclipse.egit.gitflow.ui.internal.validation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.gitflow.BranchNameValidator;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jface.dialogs.IInputValidator;

/**
 * Validates Git Flow branch names.
 */
abstract public class BranchNameInputValidator implements IInputValidator {
	@Override
	public String isValid(String newText) {
		try {
			if (branchExists(newText)) {
				return String.format(UIText.NameValidator_nameAlreadyExists,
						newText);
			}
			if (!BranchNameValidator.isBranchNameValid(newText)) {
				return String.format(UIText.NameValidator_invalidName, newText,
						BranchNameValidator.ILLEGAL_CHARS);
			}
		} catch (CoreException e) {
			return null;
		}
		return null;
	}

	/**
	 * @param newText
	 * @return Whether or not newText corresponds to an existing branch.
	 * @throws CoreException
	 */
	abstract protected boolean branchExists(String newText)
			throws CoreException;
}

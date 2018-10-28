/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.ui.internal;

import static org.eclipse.egit.core.internal.Utils.validateNewRefName;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jgit.lib.Repository;

/**
 * A collection of validators
 */
public class ValidationUtils {

	/**
	 * Creates and returns input validator for refNames
	 *
	 * @param repo
	 * @param refPrefix
	 * @param errorOnEmptyName
	 * @return input validator for refNames
	 */
	public static IInputValidator getRefNameInputValidator(
			final Repository repo, final String refPrefix,
			final boolean errorOnEmptyName) {
		return new IInputValidator() {
			@Override
			public String isValid(String newText) {
				IStatus validationStatus = validateNewRefName(newText, repo,
						refPrefix, errorOnEmptyName);
				if (validationStatus.isOK()) {
					return null;
				}
				if (validationStatus.getException() != null) {
					Activator.handleStatus(validationStatus, false);
				}
				return validationStatus.getMessage();
			}
		};
	}
}

/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * A collection of validators
 */
public class ValidationUtils {
	/**
	 * Creates and returns input validator for refNames
	 *
	 * @param repo
	 * @param refPrefix
	 * @return input validator for refNames
	 */
	public static IInputValidator getRefNameInputValidator(
			final Repository repo, final String refPrefix) {
		return new IInputValidator() {
			public String isValid(String newText) {
				String testFor = refPrefix + newText;
				try {
					if (repo.resolve(testFor) != null)
						return NLS.bind(
								UIText.ValidationUtils_RefAlreadyExistsMessage,
								testFor);
				} catch (IOException e1) {
					Activator.logError(NLS.bind(
							UIText.ValidationUtils_CanNotResolveRefMessage,
							testFor), e1);
					return e1.getMessage();
				}
				if (!Repository.isValidRefName(testFor))
					return NLS.bind(
							UIText.ValidationUtils_InvalidRefNameMessage,
							testFor);
				return null;
			}
		};
	}
}

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
	 * @param errorOnEmptyName
	 * @return input validator for refNames
	 */
	public static IInputValidator getRefNameInputValidator(final Repository repo, final String refPrefix, final boolean errorOnEmptyName) {
		return new IInputValidator() {
			public String isValid(String newText) {
				if (newText.length() == 0) {
					if (errorOnEmptyName)
						return UIText.ValidationUtils_PleaseEnterNameMessage;
					else
						// ignore this
						return null;
				}

				String testFor = refPrefix + newText;
				try {
					if (repo.resolve(testFor) != null)
						return UIText.BranchSelectionDialog_ErrorAlreadyExists;
				} catch (IOException e1) {
					Activator.logError(NLS.bind(
							UIText.BranchSelectionDialog_ErrorCouldNotResolve, testFor), e1);
					return e1.getMessage();
				}
				if (!Repository.isValidRefName(testFor))
					return UIText.ValidationUtils_InvalidTagName;
				return null;
			}
		};
	}

}

/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.StringUtils;
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
	public static IInputValidator getRefNameInputValidator(
			final Repository repo, final String refPrefix, final boolean errorOnEmptyName) {
		return new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText.length() == 0) {
					if (errorOnEmptyName)
						return UIText.ValidationUtils_PleaseEnterNameMessage;
					else
						// ignore this
						return null;
				}
				String testFor = refPrefix + newText;
				if (!Repository.isValidRefName(testFor))
					return NLS.bind(
							UIText.ValidationUtils_InvalidRefNameMessage,
							testFor);
				try {
					if (repo.resolve(testFor) != null)
						return NLS.bind(
								UIText.ValidationUtils_RefAlreadyExistsMessage,
								testFor);
					RefDatabase refDatabase = repo.getRefDatabase();
					Collection<String> conflictingNames = refDatabase.getConflictingNames(testFor);
					if (!conflictingNames.isEmpty()) {
						ArrayList<String> names = new ArrayList<>(conflictingNames);
						Collections.sort(names);
						String joined = StringUtils.join(names, ", "); //$NON-NLS-1$
						return NLS.bind(
								UIText.ValidationUtils_RefNameConflictsWithExistingMessage,
								joined);
					}
				} catch (IOException e) {
					Activator.logError(NLS.bind(
							UIText.ValidationUtils_CanNotResolveRefMessage,
							testFor), e);
					return e.getMessage();
				} catch (RevisionSyntaxException e) {
					String m = MessageFormat.format(
							UIText.ValidationUtils_InvalidRevision,
							testFor);
					Activator.logError(m, e);
					return m;
				}
				return null;
			}
		};
	}
}

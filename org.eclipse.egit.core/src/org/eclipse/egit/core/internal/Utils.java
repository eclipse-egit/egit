/*******************************************************************************
 * Copyright (C) 2010, 2019 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static java.util.stream.Collectors.joining;
import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;

/**
 * General utilities, mostly related to strings.
 */
public final class Utils {

	private static final int SHORT_OBJECT_ID_LENGTH = 7;
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final char CR_CHAR = '\r';
	private static final char LF_CHAR = '\n';

	private static final Pattern VERTICAL_SPACES = Pattern.compile("\\v+"); //$NON-NLS-1$

	private Utils() {
		// No instantiation
	}

	/**
	 * Reduce a multi-line text to the first line only.
	 *
	 * @param text
	 *            to reduce
	 * @return the first line of {@text}, may be empty.
	 */
	public static String firstLine(String text) {
		Matcher m = VERTICAL_SPACES.matcher(text);
		if (m.find()) {
			return text.substring(0, m.start());
		}
		return text;
	}

	/**
	 * Returns whether the given text contains any vertical whitespace.
	 *
	 * @param text
	 *            to check
	 * @return whether {@code text} contains vertical whitespace
	 */
	public static boolean isMultiLine(String text) {
		return VERTICAL_SPACES.matcher(text).find();
	}

	/**
	 * Converts a multi-line text to a single line by replacing all sequences of
	 * vertical whitespace by a single blank.
	 *
	 * @param text
	 *            to convert
	 * @return the converted text
	 */
	public static String toSingleLine(String text) {
		return VERTICAL_SPACES.matcher(text).replaceAll(" "); //$NON-NLS-1$
	}

	/**
	 * @param id
	 * @return a shortened ObjectId (first {@value #SHORT_OBJECT_ID_LENGTH}
	 *         digits)
	 */
	public static String getShortObjectId(ObjectId id) {
		return id.abbreviate(SHORT_OBJECT_ID_LENGTH).name();
	}

	/**
	 * The method replaces all platform specific line endings
	 * with  <code>\n</code>
	 * @param s
	 * @return String with normalized line endings
	 */
	public static String normalizeLineEndings(String s) {
		if (s == null)
			return null;
		if (s.length() == 0)
			return EMPTY_STRING;
		StringBuilder result = new StringBuilder();
		int length = s.length();
		int i = 0;
		while (i < length) {
			if (s.charAt(i) == CR_CHAR) {
				if (i + 1 < length) {
					if (s.charAt(i + 1) == LF_CHAR) {
						// CRLF -> LF
						result.append(LF_CHAR);
						i += 1;
					} else {
						// CR not followed by LF
						result.append(LF_CHAR);
					}
				} else {
					// CR at end of string
					result.append(LF_CHAR);
				}
			} else
				result.append(s.charAt(i));
			i++;
		}
		return result.toString();
	}

	/**
	 * @param text
	 * @param maxLength
	 * @return {@code text} shortened to {@code maxLength} characters if its
	 *         string length exceeds {@code maxLength} and an ellipsis is
	 *         appended to the shortened text
	 */
	public static String shortenText(final String text, final int maxLength) {
		if (text.length() > maxLength)
			return text.substring(0, maxLength - 1) + "\u2026"; // ellipsis "…" (in UTF-8) //$NON-NLS-1$
		return text;
	}

	/**
	 * Validates a given ref name, including testing whether a ref with that
	 * name already exists, or if the name conflicts with an already existing
	 * ref.
	 *
	 * @param refNameInput
	 *            Short ref name.
	 * @param repo
	 *            Repository {@code refNameInput} should be tested against.
	 * @param refPrefix
	 *            The ref namespace (refs/heads/, refs/tags, ...). Must end with
	 *            a slash. If empty, {@code refNameInput} must be a full ref
	 *            name.
	 * @param errorOnEmptyName
	 *            Whether or not to treat an empty {@code refNameInput} as
	 *            erroneous.
	 *
	 * @return {@link org.eclipse.core.runtime.Status#OK_STATUS} in case of
	 *         successful validation, or an error status with respective error
	 *         message.
	 */
	@NonNull
	public static IStatus validateNewRefName(String refNameInput,
			@NonNull Repository repo, @NonNull String refPrefix,
			final boolean errorOnEmptyName) {
		if (refNameInput == null || refNameInput.isEmpty()) {
			if (errorOnEmptyName) {
				return Activator.error(
						CoreText.ValidationUtils_PleaseEnterNameMessage, null);
			} else {
				// ignore this
				return OK_STATUS;
			}
		}
		String testFor = refPrefix + refNameInput;
		if (!Repository.isValidRefName(testFor)) {
			return Activator.error(MessageFormat.format(
					CoreText.ValidationUtils_InvalidRefNameMessage, testFor),
					null);
		}
		try {
			if (repo.exactRef(testFor) != null) {
				return Activator.error(MessageFormat.format(
						CoreText.ValidationUtils_RefAlreadyExistsMessage,
						testFor), null);
			}
			RefDatabase refDatabase = repo.getRefDatabase();
			Collection<String> conflictingNames = refDatabase
					.getConflictingNames(testFor);
			if (!conflictingNames.isEmpty()) {
				String joined = conflictingNames.stream().sorted()
						.collect(joining(", ")); //$NON-NLS-1$
				return Activator.error(MessageFormat.format(
						CoreText.ValidationUtils_RefNameConflictsWithExistingMessage,
						joined), null);
			}
		} catch (IOException e) {
			return Activator.error(e.getMessage(), e);
		} catch (RevisionSyntaxException e) {
			String m = MessageFormat
					.format(CoreText.ValidationUtils_InvalidRevision, testFor);
			return Activator.error(m, e);
		}
		return OK_STATUS;
	}

	/**
	 * Looks for a "# Conflicts:" line and returns the comment character from
	 * it.
	 *
	 * @param message
	 *            to examine
	 * @return the comment character, or {@code '\0'} if none found
	 */
	public static char commentCharFromMergeMessage(String message) {
		// Luckily git does _not_ localize the # Conflicts: line! So we can look
		// for this and figure out the comment character to use from it.
		Pattern CONFLICT_LINE = Pattern.compile("(?:^|\\R)(.) Conflicts:\\R"); //$NON-NLS-1$
		Matcher m = CONFLICT_LINE.matcher(message);
		if (m.find()) {
			return m.group(1).charAt(0);
		}
		return '\0';
	}
}

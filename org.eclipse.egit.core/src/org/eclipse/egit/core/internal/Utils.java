/*******************************************************************************
 * Copyright (C) 2010, 2015 Jens Baumgart <jens.baumgart@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Utility class
 *
 */
public class Utils {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final char CR_CHAR = '\r';
	private static final char LF_CHAR = '\n';

	/**
	 * @param id
	 * @return a shortened ObjectId (first 6 digits)
	 */
	public static String getShortObjectId(ObjectId id) {
		return id.abbreviate(6).name();
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
	 * Returns the adapter corresponding to the given adapter class.
	 * <p>
	 * Workaround for "Unnecessary cast" errors, see bug 460685. Can be removed
	 * when EGit depends on Eclipse 4.5 or higher.
	 *
	 * @param adaptable
	 *            the adaptable
	 * @param adapterClass
	 *            the adapter class to look up
	 * @return a object of the given class, or <code>null</code> if this object
	 *         does not have an adapter for the given class
	 */
	public static <T> T getAdapter(IAdaptable adaptable, Class<T> adapterClass) {
		Object adapter = adaptable.getAdapter(adapterClass);
		if (adapter == null) {
			return null;
		}
		// Guard against misbehaving IAdaptables...
		if (adapterClass.isInstance(adapter)) {
			return adapterClass.cast(adapter);
		} else {
			Activator.logError(
					MessageFormat.format(CoreText.Utils_InvalidAdapterError,
							adaptable.getClass().getName(),
							adapterClass.getName(),
							adapter.getClass().getName()),
					new IllegalStateException());
			return null;
		}
	}
}

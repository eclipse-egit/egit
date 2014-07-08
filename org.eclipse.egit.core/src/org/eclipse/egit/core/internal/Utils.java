/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

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
}

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

	private static final String CR = "\r"; //$NON-NLS-1$
	private static final String LF = "\n"; //$NON-NLS-1$
	private static final String CRLF = "\r\n"; //$NON-NLS-1$

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
		return s.replaceAll(CRLF, LF).replaceAll(CR, LF);
	}
}

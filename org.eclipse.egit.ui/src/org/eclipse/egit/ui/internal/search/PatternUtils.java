/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.search.internal.core.text.PatternConstructor;

/**
 * Pattern constructor utilities to localize warnings from using internal
 * {@link PatternConstructor} class.
 */
public abstract class PatternUtils {

	/**
	 * Create pattern
	 *
	 * @param pattern
	 * @param isCaseSensitive
	 * @param isRegex
	 * @return pattern
	 * @throws PatternSyntaxException
	 */
	public static Pattern createPattern(String pattern,
			boolean isCaseSensitive, boolean isRegex)
			throws PatternSyntaxException {
		return PatternConstructor.createPattern(pattern, isCaseSensitive,
				isRegex);
	}

}

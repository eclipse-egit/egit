/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

/**
 * Something that can translate physical to logical line numbers.
 */
public interface ILogicalLineNumberProvider {

	/**
	 * Translates a physical line number to a logical one.
	 *
	 * @param lineNumber
	 *            of the physical line
	 * @return the logical line number, or -1 if none
	 */
	int getLogicalLine(int lineNumber);

	/**
	 * Determines the largest line number this
	 * {@link ILogicalLineNumberProvider} will return.
	 *
	 * @return the maximum line number, or -1 if unknown
	 */
	int getMaximum();
}

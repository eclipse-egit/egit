/*******************************************************************************
 * Copyright (C) 2017, Stefan Rademacher <stefan.rademacher@tk.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.api;

/**
 * This class represents a commit message with a caret position.
 *
 * @param message
 *            the commit message
 * @param caretPosition
 *            the caret position within the commit message
 * @since 7.5
 */
public record CommitMessageWithCaretPosition(String message,
		int caretPosition) {

	/**
	 * This constant defines the value for an undefined caret position.
	 */
	public static final int NO_POSITION = -1;
}

/*******************************************************************************
 * Copyright (C) 2017, Stefan Rademacher <stefan.rademacher@tk.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import java.util.Objects;

/**
 * This class represents a commit message with a caret position.
 */
public class CommitMessageWithCaretPosition {

	/**
	 * This constant defines the value for an undefined caret position.
	 */
	public static final int NO_POSITION = -1;

	private final String message;

	private final int caretPosition;

	/**
	 * Constructor for creating an immutable value object, that represents a
	 * commit message and a caret position within that message.
	 *
	 * @param message
	 *            the commit message
	 * @param caretPosition
	 *            the caret position within the commit message
	 */
	public CommitMessageWithCaretPosition(String message,
			int caretPosition) {
		this.message = message;
		this.caretPosition = caretPosition;
	}

	/**
	 * @return the commit message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the desired caret position within the commit message
	 */
	public int getDesiredCaretPosition() {
		return caretPosition;
	}

	@Override
	public int hashCode() {
		return Objects.hash(message, Integer.valueOf(caretPosition));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CommitMessageWithCaretPosition other = (CommitMessageWithCaretPosition) obj;
		return caretPosition == other.caretPosition
				&& Objects.equals(message, other.message);
	}

}
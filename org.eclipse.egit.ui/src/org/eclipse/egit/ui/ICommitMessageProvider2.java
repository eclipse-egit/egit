/*******************************************************************************
 * Copyright (C) 2017, Stefan Rademacher <stefan.rademacher@tk.de>
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;

/**
 * This interface must be implemented to be a commit message provider, that does
 * not only provide the message itself, but also a caret position within this
 * message. This message will be added to the text field in the
 * {@link CommitDialog}. <br/>
 *
 * @see ICommitMessageProvider
 * @see CommitDialog
 */
public interface ICommitMessageProvider2 extends ICommitMessageProvider {

	/**
	 * Unlike {@link #getMessage(IResource[])}, this method provides a way to
	 * retrieve not only a commit message but also a caret position within the
	 * message.
	 *
	 * @param resources
	 *            the selected resources, when this method is called.
	 *
	 * @return an object, containing the commit message and the caret position
	 *         within the message
	 */
	CommitMessageWithCaretPosition getCommitMessageWithPosition(
			IResource[] resources);

	/**
	 * This class represents a commit message with a caret position.
	 */
	public static class CommitMessageWithCaretPosition {

		/**
		 * This constant defines the default caret position
		 */
		public static final int DEFAULT_POSITION = 0;

		private final String message;

		private final int desiredCaretPosition;

		/**
		 * Constructor for creating an immutable value object, that represents a
		 * commit message an a caret position within that message.
		 *
		 * @param message
		 *            the commit message
		 * @param caretPosition
		 *            the caret position within the commit message
		 */
		public CommitMessageWithCaretPosition(String message,
				int caretPosition) {
			this.message = message;
			this.desiredCaretPosition = caretPosition;
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
			return desiredCaretPosition;
		}

		@Override
		public int hashCode() {
			return Objects.hash(message, Integer.valueOf(desiredCaretPosition));
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
			return desiredCaretPosition == other.desiredCaretPosition
					&& Objects.equals(message, other.message);
		}

	}

}

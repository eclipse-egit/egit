package org.eclipse.egit.ui;

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
	 * @param resources
	 *
	 * @return an object, containing the commit message and the caret position
	 *         within the message
	 */
	CommitMessageWithCaretPosition getCommitMessageWithPosition(
			IResource[] resources);

	/**
	 * @author P221676
	 *
	 */
	public static class CommitMessageWithCaretPosition {

		/**
		 * This constant defines the int value for an undefined caret position
		 */
		public static final int DEFAULT_POSITION = 0;

		private final String message;

		private final int desiredCaretPosition;

		/**
		 * @param message
		 * @param caretPosition
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
			final int prime = 31;
			int result = 1;
			result = prime * result + desiredCaretPosition;
			result = prime * result
					+ ((message == null) ? 0 : message.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CommitMessageWithCaretPosition other = (CommitMessageWithCaretPosition) obj;
			if (desiredCaretPosition != other.desiredCaretPosition)
				return false;
			if (message == null) {
				if (other.message != null)
					return false;
			} else if (!message.equals(other.message))
				return false;
			return true;
		}

	}

}

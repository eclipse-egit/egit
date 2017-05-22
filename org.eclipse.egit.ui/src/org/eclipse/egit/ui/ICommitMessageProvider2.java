package org.eclipse.egit.ui;

import org.eclipse.egit.ui.internal.dialogs.CommitDialog;

/**
 * This interface must be implemented to be a commit message provider with
 * cursor positioning. <br/>
 *
 * @see ICommitMessageProvider
 * @see CommitDialog
 */
public interface ICommitMessageProvider2 extends ICommitMessageProvider {

	/**
	 * @return the caret position within the commit message, that is provided by
	 *         this {@link ICommitMessageProvider2}
	 */
	int getCaretPosition();

}

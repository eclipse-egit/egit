package org.eclipse.egit.ui.internal.dialogs;

/**
 * @author d020964
 *
 */
public interface ICommitMessageComponentNotifications {

	/**
	 * @param selection
	 */
	void updateSignedOffToggleSelection(boolean selection);

	/**
	 * @param selection
	 */
	void updateChangeIdToggleSelection(boolean selection);

}

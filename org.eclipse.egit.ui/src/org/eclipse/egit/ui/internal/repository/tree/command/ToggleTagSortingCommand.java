package org.eclipse.egit.ui.internal.repository.tree.command;

/**
 * Toggles the "Sort tags ascending" preference.
 */
public class ToggleTagSortingCommand extends AbstractToggleCommand {

	/**
	 * The toggle tag sorting command id
	 */
	public static final String ID = "org.eclipse.egit.ui.RepositoriesToggleTagSorting"; //$NON-NLS-1$

	/**
	 * The toggle state of this command
	 */
	public static final String TOGGLE_STATE = "org.eclipse.ui.commands.toggleState"; //$NON-NLS-1$

}

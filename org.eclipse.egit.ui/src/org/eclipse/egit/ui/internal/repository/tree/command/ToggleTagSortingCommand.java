package org.eclipse.egit.ui.internal.repository.tree.command;


import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.State;
import org.eclipse.egit.ui.internal.CommonUtils;

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

	@Override
	protected void preReviewerRefresh(ExecutionEvent event) {
		State state = event.getCommand().getState(TOGGLE_STATE);
		boolean currentValue = ((Boolean) state.getValue()).booleanValue();
		CommonUtils.setSortTagsAscending(currentValue);
	}

}

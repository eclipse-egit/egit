package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a local branch.
 *
 */
public class LocalBranchSelectionDialog extends BranchSelectionAndEditDialog {
	/**
	 * @param shell
	 * @param repository
	 * @param branchToMark
	 */
	public LocalBranchSelectionDialog(Shell shell, Repository repository,
			String branchToMark) {
		super(shell, repository, Constants.R_HEADS + branchToMark,
				SHOW_LOCAL_BRANCHES | EXPAND_LOCAL_BRANCHES_NODE
						| SELECT_CURRENT_REF);
	}

}
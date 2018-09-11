package org.eclipse.egit.ui.internal.search;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Opens the Search Dialog and brings the Git search page to front
 */
public class OpenCommitSearchPageAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow fWindow;

	@Override
	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	@Override
	public void run(IAction action) {
		if (fWindow == null || fWindow.getActivePage() == null) {
			return;
		}
		NewSearchUI.openSearchDialog(fWindow, CommitSearchPage.ID);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing since the action isn't selection dependent.
	}

	@Override
	public void dispose() {
		fWindow= null;
	}

}

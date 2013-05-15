package org.eclipse.egit.ui.internal.console;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.console.ConsoleView;

/**
 *
 */
public class GitBashSelectionListener implements ISelectionListener {

	private IResource selected;

	private GitBashConsole console;

	/**
	 * @param console
	 */
	public GitBashSelectionListener(GitBashConsole console) {
		this.console = console;
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part instanceof ConsoleView)
			return; // ignore selection in console
		if (part instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) part).getEditorInput();
			if (input instanceof IFileEditorInput)
				reactOnSelection(new StructuredSelection(
						((IFileEditorInput) input).getFile()));
		} else
			reactOnSelection(selection);

	}

	private void reactOnSelection(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			updateSelectedFileVar(null);
			return;
		}
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() != 1) {
			updateSelectedFileVar(null);
			return;
		}
		Object first = ssel.getFirstElement();
		if (first instanceof IResource) {
			IResource resource = (IResource) ssel.getFirstElement();
			updateSelectedFileVar(resource);
			return;
		}
		if (first instanceof IAdaptable) {
			IResource adapted = (IResource) ((IAdaptable) ssel
					.getFirstElement()).getAdapter(IResource.class);
			if (adapted != null) {
				updateSelectedFileVar(adapted);
				return;
			}
		}
	}

	private void updateSelectedFileVar(IResource resource) {
		this.selected = resource;
		this.console.resetName();
	}

	IResource getSelection() {
		return this.selected;
	}
}

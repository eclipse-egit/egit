/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ilya Ivanov (Intland) - implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.GitFileRevisionReference;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Compares selected file revision with it's parent revision.
 */
public class OpenWorkspaceVersionAction implements IObjectActionDelegate {

	private String absolutePath = null;

	public void run(IAction action) {
		if (absolutePath == null)
			return;

		openFileInEditor(absolutePath);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object element = ss.getFirstElement();
			if (element instanceof GitFileRevisionReference) {
				GitFileRevisionReference fileRev = ((GitFileRevisionReference) element);

				String path = new Path(fileRev.getRepository().getWorkTree()
						.getAbsolutePath()).append(fileRev.getPath()).toOSString();

				File file = new File(path);
				if (file.exists()) {
					absolutePath = path;
					action.setEnabled(true);
					return;
				}

			}
		}
		absolutePath = null;
		action.setEnabled(false);
	}

	private void openFileInEditor(String filePath) {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		File file = new File(filePath);
		if (!file.exists()) {
			String message = NLS.bind(UIText.CommitFileDiffViewer_FileDoesNotExist, filePath);
			Activator.showError(message, null);
		}
		IWorkbenchPage page = window.getActivePage();
		EgitUiEditorUtils.openEditor(file, page);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		//
	}
}

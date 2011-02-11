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


import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.GitFileRevisionReference;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EditableRevision;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.LocalFileRevision;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Compares selected file revision with it's parent revision.
 */
public class CompareSelectedWithWorkspaceAction implements IObjectActionDelegate {

	private final IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();

	private GitFileRevisionReference fileRevision;
	private IFile workspaceResource;

	public void run(IAction action) {
		if (fileRevision == null)
			return;

		ITypedElement left = createEditableWorkspaceElement();
		ITypedElement right = CompareUtils.getFileRevisionTypedElement(
				fileRevision.getPath(), fileRevision.getRevCommit(), fileRevision.getRepository());
		final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
				left, right, null);

		CompareUI.openCompareEditor(in);
		return;
	}

	private ITypedElement createEditableWorkspaceElement() {
		return new EditableRevision(new LocalFileRevision(workspaceResource)) {
			@Override
			public void setContent(final byte[] newContent) {
				try {
					PlatformUI.getWorkbench().getProgressService().run(false, false,
							new IRunnableWithProgress() {

						public void run(IProgressMonitor myMonitor)
								throws InvocationTargetException, InterruptedException {
							try {
								workspaceResource.setContents(new ByteArrayInputStream(newContent),
										false, true, myMonitor);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
				} catch (InvocationTargetException e) {
					Activator.handleError(e.getTargetException()
							.getMessage(), e.getTargetException(), true);
				} catch (InterruptedException e) {
					// ignore here
				}
			}
		};
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Object element = ss.getFirstElement();
			if (element instanceof GitFileRevisionReference) {
				GitFileRevisionReference fileRev = ((GitFileRevisionReference) element);

				String path = new Path(fileRev.getRepository().getWorkTree()
						.getAbsolutePath()).append(fileRev.getPath()).toOSString();

				IFile res = wsRoot.getFileForLocation(new Path(path));

				if (res.exists()) {
					fileRevision = fileRev;
					workspaceResource = res;
					action.setEnabled(true);
					return;
				}
			}
		}
		fileRevision = null;
		workspaceResource = null;
		action.setEnabled(false);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		//
	}
}

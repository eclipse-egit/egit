/*******************************************************************************
 * Copyright (C) 2011, 2015 Greg Amerson <gregory.amerson@liferay.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.action;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.ide.IDE;

/**
 * Action for opening an editor on the currently selected git working file.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenWorkingFileAction extends SelectionListenerAction {

	/**
	 * The workbench page to open the editor in.
	 */
	private IWorkbenchPage workbenchPage;

	/**
	 * Creates a new action that will open editors on the selected working file
	 * resources.
	 *
	 * @param page
	 *            the workbench page in which to open the editor
	 */
	public OpenWorkingFileAction(IWorkbenchPage page) {
		super(null);
		setText(UIText.OpenWorkingFileAction_text);
		setToolTipText(UIText.OpenWorkingFileAction_tooltip);
		this.workbenchPage = page;
	}

	/**
	 * Return the workbench page to open the editor in.
	 *
	 * @return the workbench page to open the editor in
	 */
	protected IWorkbenchPage getWorkbenchPage() {
		return workbenchPage;
	}

	/**
	 * Opens a editor on the given file resource.
	 *
	 * @param file
	 *            the workspace file
	 */
	protected void openWorkspaceFile(IFile file) {
		try {
			boolean activate = OpenStrategy.activateOnOpen();
			IDE.openEditor(getWorkbenchPage(), file, activate);
		} catch (PartInitException e) {
			ErrorDialog.openError(getWorkbenchPage().getWorkbenchWindow()
					.getShell(),
					UIText.OpenWorkingFileAction_openWorkingFileShellTitle, e
							.getMessage(), e.getStatus());
		}
	}

	/**
	 * Opens external file, the editor that is used is based on best guess from
	 * file name.
	 *
	 * @param file
	 *            the external file
	 */
	protected void openExternalFile(File file) {
		try {
			boolean activate = OpenStrategy.activateOnOpen();
			IEditorDescriptor desc = IDE.getEditorDescriptor(file.getName(),
					true, true);
			IDE.openEditor(getWorkbenchPage(), file.toURI(), desc.getId(),
					activate);
		} catch (PartInitException e) {
			ErrorDialog.openError(getWorkbenchPage().getWorkbenchWindow()
					.getShell(),
					UIText.OpenWorkingFileAction_openWorkingFileShellTitle, e
							.getMessage(), e.getStatus());
		}
	}

	/*
	 * (non-Javadoc) Method declared on IAction.
	 */
	@Override
	public void run() {
		IStructuredSelection selection = getStructuredSelection();

		IResource resource = getExistingResource(selection);

		if (resource instanceof IFile)
			openWorkspaceFile((IFile) resource);
		else {
			Object element = selection.getFirstElement();

			if (element instanceof GitModelObject) {
				IPath location = ((GitModelObject) element).getLocation();

				if (location != null && location.toFile().exists())
					openExternalFile(location.toFile());
			}
		}
	}

	@Nullable
	private IResource getExistingResource(IStructuredSelection selection) {
		Object element = selection.getFirstElement();
		IResource resource = Adapters.adapt(element, IResource.class);
		if (resource != null && resource.exists()) {
			return resource;
		}
		return null;
	}

	/**
	 * Enable the action only if the selection contains IFiles
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return super.updateSelection(selection)
				&& selectionIsOfType(IResource.FILE);
	}
}

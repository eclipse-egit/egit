/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.DeletePathsOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.DeleteResourceAction;

/**
 * Common UI for deleting paths (both resources that are part of projects and
 * non-workspace paths).
 */
public class DeletePathsOperationUI {

	private final Collection<IPath> paths;
	private final IShellProvider shellProvider;

	/**
	 * Create the operation with the resources to delete.
	 *
	 * @param paths
	 *            to delete
	 * @param shellProvider
	 */
	public DeletePathsOperationUI(final Collection<IPath> paths,
			final IShellProvider shellProvider) {
		this.paths = paths;
		this.shellProvider = shellProvider;
	}

	/**
	 * Run the operation.
	 */
	public void run() {
		List<IResource> resources = getSelectedResourcesIfAllExist();
		if (resources != null)
			runNormalAction(resources);
		else
			runNonWorkspaceAction();
	}

	private void runNormalAction(List<IResource> resources) {
		DeleteResourceAction action = new DeleteResourceAction(shellProvider);
		IStructuredSelection selection = new StructuredSelection(resources);
		action.selectionChanged(selection);
		action.run();
	}

	private String[] getButtonLabels() {
		return new String[] { UIText.DeletePathsOperationUI_ButtonOK, IDialogConstants.CANCEL_LABEL };
	}

	private boolean openConfirm(Shell parent, String title, String message) {
		MessageDialog dialog = new MessageDialog(parent, title, null, message,
				MessageDialog.CONFIRM, 0, getButtonLabels());
		return dialog.open() == 0;
	}

	private void runNonWorkspaceAction() {
		boolean performAction = openConfirm(
				shellProvider.getShell(),
				UIText.DeleteResourcesOperationUI_confirmActionTitle,
				UIText.DeleteResourcesOperationUI_confirmActionMessage);
		if (!performAction)
			return;

		DeletePathsOperation operation = new DeletePathsOperation(paths);

		try {
			operation.execute(null);
		} catch (CoreException e) {
			Activator.handleError(UIText.DeleteResourcesOperationUI_deleteFailed, e, true);
		}
	}

	private List<IResource> getSelectedResourcesIfAllExist() {
		List<IResource> resources = new ArrayList<>();
		for (IPath path : paths) {
			IResource resource = ResourceUtil.getResourceForLocation(path, false);
			if (resource != null)
				resources.add(resource);
			else
				return null;
		}
		return resources;
	}
}

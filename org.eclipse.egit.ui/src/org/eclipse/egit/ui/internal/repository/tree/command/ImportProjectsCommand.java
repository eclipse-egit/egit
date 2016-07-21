/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Tobias Baumann <tobbaumann@gmail.com> - Bug #494269
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCreateProjectViaWizardWizard;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * Implements "Add Projects" for Repository, Working Directory, and Folder
 */
public class ImportProjectsCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> selectedNodes = getSelectedNodes(event);
		if (selectedNodes == null || selectedNodes.isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					UIText.ImportProjectsWrongSelection,
					UIText.ImportProjectsSelectionInRepositoryRequired);
			return null;
		}
		if (!(((List) selectedNodes).get(0) instanceof RepositoryTreeNode)) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					UIText.ImportProjectsWrongSelection,
					UIText.ImportProjectsSelectionInRepositoryRequired);
			return null;
		}
		RepositoryTreeNode node = selectedNodes.get(0);
		String path;

		switch (node.getType()) {
		case REPO:
			// fall through
		case WORKINGDIR:
			path = node.getRepository().getWorkTree().toString();
			break;
		case FOLDER:
			path = ((FolderNode) node).getObject().getPath().toString();
			break;
		default:
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					UIText.ImportProjectsWrongSelection,
					UIText.ImportProjectsSelectionInRepositoryRequired);
			return null;
		}

		IWizardDescriptor descriptor = findSmartImportWizardDescriptor();
		if (descriptor != null) {
			openSmartImportWizard(event, descriptor, path);
		} else {
			openGitCreateProjectViaWizardWizard(event, node, path);
		}

		return null;
	}

	private IWizardDescriptor findSmartImportWizardDescriptor() {
		final String smartImportWizardId = "org.eclipse.e4.ui.importer.wizard"; //$NON-NLS-1$
		return PlatformUI.getWorkbench().getImportWizardRegistry()
				.findWizard(smartImportWizardId);
	}

	private void openSmartImportWizard(ExecutionEvent event,
			IWizardDescriptor descriptor, String path)
			throws ExecutionException {
		try {
			IWorkbenchWizard wizard = descriptor.createWizard();
			wizard.init(PlatformUI.getWorkbench(),
					new StructuredSelection(new File(path)));
			WizardDialog dlg = new WizardDialog(getShell(event), wizard);
			dlg.setTitle(wizard.getWindowTitle());
			dlg.setHelpAvailable(false);
			dlg.open();
		} catch (CoreException e) {
			throw new ExecutionException(
					"Error during opening smart import wizard.", e); //$NON-NLS-1$
		}
	}

	private void openGitCreateProjectViaWizardWizard(ExecutionEvent event,
			RepositoryTreeNode node, String path) {
		WizardDialog dlg = new WizardDialog(getShell(event),
				new GitCreateProjectViaWizardWizard(node.getRepository(),
						path)) {
			@Override
			protected IDialogSettings getDialogBoundsSettings() {
				// preserve dialog bounds
				return Activator.getDefault().getDialogSettings();
			}
		};
		dlg.setHelpAvailable(false);
		dlg.open();
	}
}

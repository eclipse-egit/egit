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
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCreateProjectViaWizardWizard;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportWizard;
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

		if (isSmartImportWizardAvailable()) {
			openSmartImportWizard(event, path);
		} else {
			openGitCreateProjectViaWizardWizard(event, node, path);
		}

		return null;
	}

	private boolean isSmartImportWizardAvailable() {
		final String smartImportWizardId = "org.eclipse.e4.ui.importer.wizard"; //$NON-NLS-1$
		IWizardDescriptor descriptor = PlatformUI.getWorkbench()
				.getImportWizardRegistry().findWizard(smartImportWizardId);
		return descriptor != null;
	}

	private void openSmartImportWizard(ExecutionEvent event, String path) {
		SmartImportWizard wizard = new SmartImportWizard();
		wizard.setInitialImportSource(new File(path));
		WizardDialog dlg = new WizardDialog(getShell(event), wizard);
		dlg.setTitle(wizard.getWindowTitle());
		dlg.setHelpAvailable(false);
		dlg.open();
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

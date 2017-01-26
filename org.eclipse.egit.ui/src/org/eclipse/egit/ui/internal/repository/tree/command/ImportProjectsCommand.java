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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCreateProjectViaWizardWizard;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
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

		openWizard(event, selectedNodes);
		return null;
	}

	private void openWizard(ExecutionEvent event,
			List<RepositoryTreeNode> selectedNodes) throws ExecutionException {
		IWizardDescriptor descriptor = findSmartImportWizardDescriptor();
		if (descriptor == null || multipleProjectsSelected(selectedNodes)) {
			RepositoryTreeNode node;
			if (multipleProjectsSelected(selectedNodes)) {
				node = findRepoNode(selectedNodes.get(0));
			} else {
				node = selectedNodes.get(0);
			}
			String path = getPathFromNode(node);
			if (path == null) {
				return;
			}
			openGitCreateProjectViaWizardWizard(event, node, path,
					getMultipleSelectedProjects(selectedNodes));
		} else {
			String path = getPathFromNode(selectedNodes.get(0));
			openSmartImportWizard(event, descriptor, path);
		}
	}

	private boolean multipleProjectsSelected(List<?> selectedNodes) {
		return selectedNodes.size() > 1;
	}

	private List<String> getMultipleSelectedProjects(List<RepositoryTreeNode> pSelectedNodes) {
		if (!multipleProjectsSelected(pSelectedNodes)) {
			return Collections.emptyList();
		}
		ArrayList<String> paths = new ArrayList<>();
		for (RepositoryTreeNode node : pSelectedNodes) {
			String path = getPathFromNode(node);
			if (path == null) {
				return null;
			}
			paths.add(path);
		}
		return paths;
	}

	private RepositoryTreeNode findRepoNode(RepositoryTreeNode pNode) {
		RepositoryTreeNode result = pNode;
		while (!result.getType().equals(RepositoryTreeNodeType.REPO)) {
			result = result.getParent();
		}
		return result;
	}

	private String getPathFromNode(RepositoryTreeNode node) {
		switch (node.getType()) {
		case REPO:
			// fall through
		case WORKINGDIR:
			return node.getRepository().getWorkTree().toString();
		case FOLDER:
			return ((FolderNode) node).getObject().getPath().toString();
		default:
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					UIText.ImportProjectsWrongSelection,
					UIText.ImportProjectsSelectionInRepositoryRequired);
			return null;
		}
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
			RepositoryTreeNode node, String path, List<String> pPaths) {
		if (pPaths.size() == 1) {
			path = pPaths.get(0);
		}
		GitCreateProjectViaWizardWizard wizard = new GitCreateProjectViaWizardWizard(
				node.getRepository(), path);
		if (pPaths.size() > 1) {
			wizard.setFilter(pPaths);
		}
		WizardDialog dlg = new WizardDialog(getShell(event), wizard) {
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

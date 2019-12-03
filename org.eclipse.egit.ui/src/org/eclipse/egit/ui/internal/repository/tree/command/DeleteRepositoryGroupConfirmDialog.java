/*******************************************************************************
 * Copyright (C) 2019, Alexander Nittka <alex@nittka.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNodeLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Confirm the deletion of one or more repository groups.
 */
public class DeleteRepositoryGroupConfirmDialog extends TitleAreaDialog {

	private List<RepositoryGroupNode> groupsToDelete;

	private boolean shouldShowAgain;

	private Button dontShowAgain;

	/**
	 * @param parentShell
	 * @param groupsToDelete
	 *            list of repository groups to delete
	 */
	public DeleteRepositoryGroupConfirmDialog(Shell parentShell,
			List<RepositoryGroupNode> groupsToDelete) {
		super(parentShell);
		setHelpAvailable(false);
		this.groupsToDelete = groupsToDelete;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.RepositoriesView_RepoGroup_Delete_Title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(1, false));
		TreeViewer groupsViewer = new TreeViewer(main,
				SWT.BORDER | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(groupsViewer.getControl());
		groupsViewer
				.setLabelProvider(new RepositoryTreeNodeLabelProvider(true));
		groupsViewer.setContentProvider(
				new DeleteRepositoryGroupTreeContentProvider());
		groupsViewer.setInput(groupsToDelete);
		dontShowAgain = new Button(main, SWT.CHECK);
		dontShowAgain.setText(UIText.RepositoriesView_RepoGroup_DeleteDontShowAgain);
		setTitle(UIText.RepositoriesView_RepoGroup_Delete_Title);
		setMessage(UIText.RepositoriesView_RepoGroup_Delete_Confirm);
		return main;
	}

	@Override
	protected void okPressed() {
		shouldShowAgain = !dontShowAgain.getSelection();
		super.okPressed();
	}

	/**
	 * @return whether to show this dialog again in the future
	 */
	public boolean showAgain() {
		return shouldShowAgain;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.DeleteRepositoryConfirmDialog_DeleteRepositoryConfirmButton,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	private static class DeleteRepositoryGroupTreeContentProvider
			implements ITreeContentProvider {

		private List<RepositoryGroupNode> groupsToDelete;

		@Override
		@SuppressWarnings("unchecked")
		public void inputChanged(Viewer viewer, Object oldInput,
				Object newInput) {
			groupsToDelete = (List<RepositoryGroupNode>) newInput;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof RepositoryGroupNode) {
				return !((RepositoryGroupNode) element).getObject()
						.getRepositoryDirectories().isEmpty();
			}
			return false;
		}

		@Override
		public Object getParent(Object element) {
			return ((RepositoryTreeNode<?>) element).getParent();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return groupsToDelete.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof RepositoryGroupNode) {
				RepositoryCache cache = Activator.getDefault()
						.getRepositoryCache();
				RepositoryGroupNode groupNode = ((RepositoryGroupNode) parentElement);
				final List<RepositoryNode> result = new ArrayList<>();
				groupNode.getObject().getRepositoryDirectories().stream()
						.forEach(repoDir -> {
							try {
								result.add(new RepositoryNode(groupNode,
										cache.lookupRepository(repoDir)));
							} catch (IOException e) {
								// ignore
							}
						});
				return result.toArray();
			}
			return null;
		}
	}
}

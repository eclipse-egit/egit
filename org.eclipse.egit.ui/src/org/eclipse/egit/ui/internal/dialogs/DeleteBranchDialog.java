/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.DeleteBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for deleting a branch
 *
 */
public class DeleteBranchDialog extends AbstractBranchSelectionDialog {

	private static final class BranchLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			RefNode refNode = (RefNode) element;
			return refNode.getObject().getName();
		}

		@Override
		public Image getImage(Object element) {
			return RepositoryTreeNodeType.REF.getIcon();
		}
	}

	private static final class BranchMessageDialog extends MessageDialog {
		private final List<RefNode> nodes;

		private BranchMessageDialog(Shell parentShell, List<RefNode> nodes) {
			super(parentShell, UIText.RepositoriesView_ConfirmDeleteTitle,
					null, UIText.RepositoriesView_ConfirmBranchDeletionMessage,
					MessageDialog.QUESTION, new String[] {
							IDialogConstants.OK_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			this.nodes = nodes;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite area = new Composite(parent, SWT.NONE);
			area.setLayoutData(new GridData(GridData.FILL_BOTH));
			area.setLayout(new FillLayout());

			TableViewer branchesList = new TableViewer(area);
			branchesList.setContentProvider(ArrayContentProvider.getInstance());
			branchesList.setLabelProvider(new BranchLabelProvider());
			branchesList.setInput(nodes);
			return area;
		}

	}

	private String currentBranch;

	/**
	 * @param parentShell
	 * @param repo
	 */
	public DeleteBranchDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo);
		setRootsToShow(true, true, false, false);
		try {
			currentBranch = repo.getFullBranch();
		} catch (IOException e) {
			// just ignore here
		}
	}

	@Override
	protected String getMessageText() {
		return UIText.DeleteBranchDialog_DialogMessage;
	}

	@Override
	protected String getTitle() {
		return UIText.DeleteBranchDialog_DialogTitle;
	}

	@Override
	protected String getWindowTitle() {
		return UIText.DeleteBranchDialog_WindowTitle;
	}

	@Override
	protected void refNameSelected(String refName) {
		getButton(Window.OK).setEnabled(
				refName != null && !refName.equals(currentBranch));
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == Window.OK) {
			Ref ref = refFromDialog();
			try {
				int result = deleteBranch(ref, false);
				if (result == DeleteBranchOperation.REJECTED_UNMERGED) {
					List<RefNode> nodes = new ArrayList<RefNode>();
					nodes
							.add((RefNode) ((IStructuredSelection) super.branchTree
									.getSelection()).getFirstElement());
					MessageDialog messageDialog = new BranchMessageDialog(
							getShell(), nodes);
					if (messageDialog.open() == Window.OK) {
						deleteBranch(ref, false);
					} else {
						return;
					}
				} else if (result == DeleteBranchOperation.REJECTED_CURRENT) {
					Activator
							.handleError(
									UIText.DeleteBranchCommand_CannotDeleteCheckedOutBranch,
									null, true);
				}

			} catch (CoreException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}

		super.buttonPressed(buttonId);
	}

	private int deleteBranch(final Ref ref, boolean force) throws CoreException {
		DeleteBranchOperation dbop = new DeleteBranchOperation(repo, ref, force);
		dbop.execute(null);
		return dbop.getStatus();
	}
}

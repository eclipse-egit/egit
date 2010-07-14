/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Deletes a branch.
 */
public class DeleteBranchCommand extends
		RepositoriesViewCommandHandler<RefNode> {

	private final class BranchMessageDialog extends MessageDialog {
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

	private final class BranchLabelProvider extends LabelProvider {
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

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final List<RefNode> nodes = getSelectedNodes(event);
		final List<Ref> refs = new ArrayList<Ref>();
		for (RefNode refNode : nodes) {
			refs.add(refNode.getObject());
		}

		Shell shell = getShell(event);
		MessageDialog messageDialog = new BranchMessageDialog(shell, nodes);
		if (messageDialog.open() != Window.OK)
			return null;

		try {
			new ProgressMonitorDialog(shell).run(false, false,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								for (RefNode refNode : nodes)
									deleteBranch(refNode, refNode.getObject());
							} catch (IOException ioe) {
								throw new InvocationTargetException(ioe);
							}
						}
					});
		} catch (InvocationTargetException e1) {
			Activator.handleError(
					UIText.RepositoriesView_BranchDeletionFailureMessage,
					e1.getCause(), true);
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// ignore
		}

		return null;
	}

	private void deleteBranch(final RefNode node, final Ref ref)
			throws IOException {
		RefUpdate op = node.getRepository().updateRef(ref.getName());
		op.setRefLogMessage("branch deleted", //$NON-NLS-1$
				false);
		// TODO: This uses the force option always, so a warning pop-up is shown to the
		// user; instead this should check if deletion can be performed without data
		// loss and in this case the deletion should be done quietly; the warning pop-up
		// should only be shown if the force option is really needed.
		op.setForceUpdate(true);
		op.delete();
	}
}

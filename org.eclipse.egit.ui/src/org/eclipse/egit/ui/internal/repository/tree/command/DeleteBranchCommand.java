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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.DeleteBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.UnmergedBranchDialog;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.widgets.Shell;

/**
 * Deletes a branch.
 */
public class DeleteBranchCommand extends
		RepositoriesViewCommandHandler<RefNode> {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final List<RefNode> nodes = getSelectedNodes(event);
		final List<RefNode> unmergedNodes = new ArrayList<RefNode>();

		final Shell shell = getShell(event);

		try {
			new ProgressMonitorDialog(shell).run(false, false,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								monitor.beginTask(UIText.DeleteBranchCommand_DeletingBranchesProgress, nodes.size());
								for (RefNode refNode : nodes) {
									int result = deleteBranch(refNode, refNode
											.getObject(), false);
									if (result == DeleteBranchOperation.REJECTED_CURRENT) {
										throw new CoreException(
												Activator
														.createErrorStatus(
																UIText.DeleteBranchCommand_CannotDeleteCheckedOutBranch,
																null));
									} else if (result == DeleteBranchOperation.REJECTED_UNMERGED) {
										unmergedNodes.add(refNode);
									} else
										monitor.worked(1);
								}
								if (!unmergedNodes.isEmpty()) {
									MessageDialog messageDialog = new UnmergedBranchDialog<RefNode>(
											shell, unmergedNodes);
									if (messageDialog.open() == Window.OK) {
										for (RefNode node : unmergedNodes) {
											deleteBranch(node,
													node.getObject(), true);
											monitor.worked(1);
										}
									}
								}
							} catch (CoreException ex) {
								throw new InvocationTargetException(ex);
							} finally {
								monitor.done();
							}
						}
					});
		} catch (InvocationTargetException e1) {
			Activator.handleError(
					UIText.RepositoriesView_BranchDeletionFailureMessage, e1
							.getCause(), true);
		} catch (InterruptedException e1) {
			// ignore
		}

		return null;
	}

	private int deleteBranch(final RefNode node, final Ref ref, boolean force)
			throws CoreException {
		DeleteBranchOperation dbop = new DeleteBranchOperation(node
				.getRepository(), ref, force);
		dbop.execute(null);
		return dbop.getStatus();
	}
}

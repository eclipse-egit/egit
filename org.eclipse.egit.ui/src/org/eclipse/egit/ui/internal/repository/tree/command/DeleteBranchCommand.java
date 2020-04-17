/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.DeleteBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.UnmergedBranchDialog;
import org.eclipse.egit.ui.internal.repository.tree.BranchHierarchyNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * Deletes a branch.
 */
public class DeleteBranchCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final List<RepositoryTreeNode> nodes = getSelectedNodes(event);
		final Map<Ref, Repository> refs = getRefsToDelete(nodes);
		final AtomicReference<Map<Ref, Repository>> unmergedNodesRef = new AtomicReference<>(
				Collections.emptyMap());
		final Shell shell = getShell(event);

		try {
			new ProgressMonitorDialog(shell).run(true, false,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							Map<Ref, Repository> unmergedNodes = deleteBranches(
									refs, false, monitor);
							unmergedNodesRef.set(unmergedNodes);
						}
					});
		} catch (InvocationTargetException e1) {
			Activator.handleError(
					UIText.RepositoriesView_BranchDeletionFailureMessage, e1
							.getCause(), true);
		} catch (InterruptedException e1) {
			// ignore
		}
		if (unmergedNodesRef.get().isEmpty())
			return null;
		MessageDialog messageDialog = new UnmergedBranchDialog(
					shell, new ArrayList<>(unmergedNodesRef.get().keySet()));
		if (messageDialog.open() != Window.OK)
			return null;
		try {
			new ProgressMonitorDialog(shell).run(true, false,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							deleteBranches(unmergedNodesRef.get(), true, monitor);
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

	private Map<Ref, Repository> getRefsToDelete(
			List<RepositoryTreeNode> nodes) {
		LinkedHashMap<Ref, Repository> refs = new LinkedHashMap<>();
		for (RepositoryTreeNode node : nodes) {
			if (node instanceof BranchHierarchyNode) {
				try {
					for (Ref ref : ((BranchHierarchyNode) node)
							.getChildRefsRecursive()) {
						refs.put(ref, node.getRepository());
					}
				} catch (IOException e) {
					Activator.logError(MessageFormat.format(
							UIText.RepositoriesView_BranchCollectionError,
							node.getPath(),
							node.getRepository().getDirectory()), e);
				}
			} else if (node instanceof RefNode) {
				refs.put((Ref) node.getObject(), node.getRepository());
			}
		}
		return refs;
	}

	private Map<Ref, Repository> deleteBranches(final Map<Ref, Repository> refs,
			final boolean forceDeletionOfUnmergedBranches,
			IProgressMonitor progressMonitor) throws InvocationTargetException {
		final Map<Ref, Repository> unmergedNodes = new LinkedHashMap<>();
		try {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					monitor.beginTask(
							UIText.DeleteBranchCommand_DeletingBranchesProgress,
							refs.size());
					for (Entry<Ref, Repository> entry : refs.entrySet()) {
						Repository repository = entry.getValue();
						Ref ref = entry.getKey();
						int result = deleteBranch(repository, ref,
								forceDeletionOfUnmergedBranches);
						if (result == DeleteBranchOperation.REJECTED_CURRENT) {
							throw new CoreException(
									Activator
									.createErrorStatus(
											UIText.DeleteBranchCommand_CannotDeleteCheckedOutBranch,
											null));
						} else if (result == DeleteBranchOperation.REJECTED_UNMERGED) {
							unmergedNodes.put(ref, repository);
						} else
							monitor.worked(1);
					}
				}
			}, progressMonitor);

		} catch (CoreException ex) {
			throw new InvocationTargetException(ex);
		} finally {
			progressMonitor.done();
		}
		return unmergedNodes;
	}

	private int deleteBranch(final Repository repo, final Ref ref,
			boolean force) throws CoreException {
		DeleteBranchOperation dbop = new DeleteBranchOperation(repo, ref,
				force);
		dbop.execute(null);
		return dbop.getStatus();
	}

}

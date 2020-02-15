/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Alexander Nittka <alex@nittka.de> - Bug 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.groups.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

/**
 * Drop Adapter Assistant for the Repositories View
 */
public class DropAdapterAssistant extends CommonDropAdapterAssistant {
	/**
	 * Default constructor
	 */
	public DropAdapterAssistant() {
		// nothing
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter aDropAdapter,
			DropTargetEvent aDropTargetEvent, Object aTarget) {
		if (aTarget instanceof RepositoryGroupNode) {
			return handleRepositoryGroupNodeDrop((RepositoryGroupNode) aTarget,
					aDropTargetEvent);
		} else if (aTarget instanceof IWorkspaceRoot) {
			return handleWorkspaceRootDrop(aDropTargetEvent);
		}
		String[] data = (String[]) aDropTargetEvent.data;
		for (String folder : data) {
			File repoFile = new File(folder);
			if (FileKey.isGitRepository(repoFile, FS.DETECTED))
				Activator.getDefault().getRepositoryUtil()
						.addConfiguredRepository(repoFile);
			// also a direct parent of a .git dir is allowed
			else if (!repoFile.getName().equals(Constants.DOT_GIT)) {
				File dotgitfile = new File(repoFile, Constants.DOT_GIT);
				if (FileKey.isGitRepository(dotgitfile, FS.DETECTED))
					Activator.getDefault().getRepositoryUtil()
							.addConfiguredRepository(dotgitfile);
			}
		}
		// the returned Status is not consumed anyway
		return Status.OK_STATUS;
	}

	@Override
	public IStatus validateDrop(Object target, int operation,
			TransferData transferData) {
		if (target instanceof RepositoryGroupNode) {
			return validateRepositoryGroupNodeDrop();
		} else if (target instanceof IWorkspaceRoot) {
			return validateRepositoryGroupNodeDrop();
		}
		// check that all paths are valid repository paths
		String[] folders = (String[]) FileTransfer.getInstance().nativeToJava(
				transferData);
		if (folders == null)
			return Status.CANCEL_STATUS;
		for (String folder : folders) {
			File repoFile = new File(folder);
			if (FileKey.isGitRepository(repoFile, FS.DETECTED)) {
				continue;
			}
			// convenience: also allow the direct parent of .git
			if (!repoFile.getName().equals(Constants.DOT_GIT)) {
				File dotgitfile = new File(repoFile, Constants.DOT_GIT);
				if (FileKey.isGitRepository(dotgitfile, FS.DETECTED))
					continue;
			}
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	private IStatus validateRepositoryGroupNodeDrop() {
		ISelection selection = LocalSelectionTransfer.getTransfer()
				.getSelection();
		if (onlyRepositoryNodesSelected(selection)) {
			return Status.OK_STATUS;
		} else {
			return Status.CANCEL_STATUS;
		}
	}

	private IStatus handleRepositoryGroupNodeDrop(RepositoryGroupNode group,
			DropTargetEvent event) {
		if (event.data instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) event.data;
			if (onlyRepositoryNodesSelected(selection)) {
				List<File> reposToAdd = new ArrayList<>();
				for (Object treeNode : selection.toList()) {
					RepositoryNode repo = (RepositoryNode) treeNode;
					reposToAdd.add(repo.getRepository().getDirectory());
				}
				RepositoryGroups.getInstance().addRepositoriesToGroup(
						group.getObject(), reposToAdd);
				refreshRepositoriesView(group);
				return Status.OK_STATUS;
			}
		}
		return Status.CANCEL_STATUS;
	}

	private IStatus handleWorkspaceRootDrop(DropTargetEvent event) {
		if (event.data instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) event.data;
			if (onlyRepositoryNodesSelected(selection)) {
				List<File> reposToRemove = new ArrayList<>();
				for (Object treeNode : selection.toList()) {
					RepositoryNode repo = (RepositoryNode) treeNode;
					reposToRemove.add(repo.getRepository().getDirectory());
				}
				RepositoryGroups.getInstance().removeFromGroups(reposToRemove);
				refreshRepositoriesView(null);
				return Status.OK_STATUS;
			}
		}
		return Status.CANCEL_STATUS;
	}

	private void refreshRepositoriesView(RepositoryGroupNode groupNode) {
		RepositoriesView view = (RepositoriesView) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		view.refresh();
		if (groupNode != null) {
			view.expandNodeForGroup(groupNode.getObject());
		}
	}

	private boolean onlyRepositoryNodesSelected(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			return ((List<?>) ((IStructuredSelection) selection).toList())
					.stream().allMatch(this::isRegularRepository);
		}
		return false;
	}

	private boolean isRegularRepository(Object node) {
		if (node instanceof RepositoryNode) {
			RepositoryNode repoNode = (RepositoryNode) node;
			return repoNode.getParent() == null || repoNode.getParent()
					.getType() == RepositoryTreeNodeType.REPOGROUP;
		}
		return false;
	}

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return FileTransfer.getInstance().isSupportedType(aTransferType);
	}
}

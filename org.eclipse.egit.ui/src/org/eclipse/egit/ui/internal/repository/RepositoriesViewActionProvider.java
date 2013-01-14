/*******************************************************************************
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

/**
 * Additional actions for the repositories view context menu.
 */
public class RepositoriesViewActionProvider extends CommonActionProvider {

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ISelection selection = getContext().getSelection();
		if (selection.isEmpty())
			return;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (shouldAddShowInMenu(structuredSelection)) {
				ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) getActionSite()
						.getViewSite();

				MenuManager showInSubMenu = UIUtils.createShowInMenu(
						site.getWorkbenchWindow());
				menu.appendToGroup(ICommonMenuConstants.GROUP_SHOW, showInSubMenu);
			}
		}
	}

	private static boolean shouldAddShowInMenu(IStructuredSelection selection) {
		for (Object element : selection.toList()) {
			if (element instanceof RepositoryNode
					|| element instanceof WorkingDirNode
					|| element instanceof FileNode
					|| element instanceof FolderNode
					|| element instanceof RemoteNode
					|| element instanceof FetchNode
					|| element instanceof PushNode
					|| element instanceof TagNode
					|| element instanceof RefNode)
				return true;
		}
		return false;
	}
}

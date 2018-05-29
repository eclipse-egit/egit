/*******************************************************************************
 * Copyright (C) 2012, 2014 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import org.eclipse.core.resources.IFile;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.FetchNode;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.WorkingDirNode;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

/**
 * Additional actions for the repositories view context menu.
 */
public class RepositoriesViewActionProvider extends CommonActionProvider {

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ISelection s = getContext().getSelection();
		if (s.isEmpty() || !(s instanceof IStructuredSelection))
			return;

		IStructuredSelection selection = (IStructuredSelection) s;
		ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) getActionSite()
				.getViewSite();
		if (shouldAddShowInMenu(selection)) {
			MenuManager showInSubMenu = UIUtils.createShowInMenu(site
					.getWorkbenchWindow());
			menu.appendToGroup(ICommonMenuConstants.GROUP_SHOW, showInSubMenu);
		}

		IFile file = getSelectedFile(selection);
		if (file != null) {
			MenuManager openWithSubMenu = new MenuManager(UIText.RepositoriesViewActionProvider_OpenWithMenu);
			openWithSubMenu.add(new OpenWithMenu(site.getPage(), file));
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openWithSubMenu);
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

	/**
	 * @param selection
	 * @return single selected file, or {@code null} if none
	 */
	@Nullable
	private static IFile getSelectedFile(IStructuredSelection selection) {
		if (selection.size() == 1
				&& selection.getFirstElement() instanceof FileNode) {
			FileNode fileNode = (FileNode) selection.getFirstElement();
			return ResourceUtil.getFileForLocation(fileNode.getPath(), false);
		}
		return null;
	}
}

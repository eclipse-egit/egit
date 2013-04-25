/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, IBM Corporation
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.factories;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.history.GitHistoryPageSource;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.synchronize.mapping.GitModelWorkbenchAdapter;
import org.eclipse.egit.ui.internal.synchronize.mapping.GitObjectMapping;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.IShowInSource;

/**
 * This class is an intelligent "cast" operation for getting
 * an instance of a suitable object from another for a specific
 * purpose.
 */
public class GitAdapterFactory implements IAdapterFactory {

	private Object historyPageSource = new GitHistoryPageSource();
	private GitModelWorkbenchAdapter gitModelWorkbenchAdapter;

	private static final IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType.isAssignableFrom(IHistoryPageSource.class)) {
			return historyPageSource;
		}

		if (IWorkbenchAdapter.class == adapterType) {
			if (adaptableObject instanceof RepositoryNode)
				return getRepsitoryNodeWorkbenchAdapter((RepositoryNode)adaptableObject);

			if (gitModelWorkbenchAdapter == null)
				gitModelWorkbenchAdapter = new GitModelWorkbenchAdapter();
			return gitModelWorkbenchAdapter;
		}

		if (adaptableObject instanceof IHistoryView
				&& IShowInSource.class == adapterType) {
			IHistoryView historyView = (IHistoryView) adaptableObject;
			IHistoryPage historyPage = historyView.getHistoryPage();
			if (historyPage instanceof GitHistoryPage)
				return historyPage;
		}

		if (adaptableObject instanceof GitModelObject
				&& adapterType == ResourceMapping.class)
			return GitObjectMapping.create((GitModelObject) adaptableObject);

		if (adaptableObject instanceof GitModelObject
				&& adapterType == IResource.class) {
			GitModelObject obj = (GitModelObject) adaptableObject;

			if (obj instanceof GitModelBlob) {
				IResource res = ResourceUtil.getFileForLocation(obj
						.getLocation());
				if (res == null)
					res = root.getFile(obj.getLocation());

				return res;
			}

			if (obj instanceof GitModelTree) {
				IResource res = root.getContainerForLocation(obj.getLocation());
				if (res == null)
					res = root.getFolder(obj.getLocation());

				return res;
			}
		}

		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IHistoryPageSource.class,
				ISynchronizationCompareAdapter.class, ResourceMapping.class,
				IResource.class, IWorkbenchAdapter.class, IShowInSource.class };
	}

	private static IWorkbenchAdapter getRepsitoryNodeWorkbenchAdapter(final RepositoryNode node) {
		return new WorkbenchAdapter() {
			@Override
			public String getLabel(Object object) {
				ILabelProvider labelProvider= new RepositoriesViewLabelProvider();
				return labelProvider.getText(node);
			}
		};
	}
}

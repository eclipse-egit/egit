/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
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
import org.eclipse.egit.ui.internal.history.GitHistoryPageSource;
import org.eclipse.egit.ui.internal.synchronize.mapping.GitModelWorkbenchAdapter;
import org.eclipse.egit.ui.internal.synchronize.mapping.GitObjectMapping;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

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

	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType.isAssignableFrom(IHistoryPageSource.class)) {
			return historyPageSource;
		}

		if (IWorkbenchAdapter.class == adapterType) {
			if (gitModelWorkbenchAdapter == null)
				gitModelWorkbenchAdapter = new GitModelWorkbenchAdapter();
			return gitModelWorkbenchAdapter;
		}

		if (adaptableObject instanceof GitModelObject
				&& adapterType == ResourceMapping.class)
			return GitObjectMapping.create((GitModelObject) adaptableObject);

		if (adaptableObject instanceof GitModelObject
				&& adapterType == IResource.class) {
			GitModelObject obj = (GitModelObject) adaptableObject;

			if (obj instanceof GitModelBlob)
				return root.getFileForLocation(obj.getLocation());

			if (obj instanceof GitModelTree) {
				return root.getContainerForLocation(obj.getLocation());
			}
		}

		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IHistoryPageSource.class,
				ISynchronizationCompareAdapter.class, ResourceMapping.class,
				IResource.class };
	}

}

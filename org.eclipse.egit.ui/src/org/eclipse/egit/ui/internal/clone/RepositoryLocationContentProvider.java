/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositoryServerProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.RepositoryServerInfo;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

class RepositoryLocationContentProvider implements ITreeContentProvider {

	private Map<RepositoryServerInfo, CloneSourceProvider> parents = new HashMap<>();

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing to do
	}

	@Override
	public void dispose() {
		// nothing to do
	}

	@Override
	public boolean hasChildren(Object element) {
		Object[] children = calculateChildren(element);
		return children != null && children.length > 0;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof RepositoryServerInfo)
			return parents.get(element);
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {
		List<CloneSourceProvider> repositoryImports = (List<CloneSourceProvider>) inputElement;
		return repositoryImports.toArray(new CloneSourceProvider[repositoryImports
				.size()]);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return calculateChildren(parentElement);
	}

	private Object[] calculateChildren(Object parentElement) {
		if (parentElement instanceof CloneSourceProvider) {
			CloneSourceProvider repositoryImport = (CloneSourceProvider) parentElement;
			if (repositoryImport.hasFixLocation())
				return null;
			Collection<RepositoryServerInfo> repositoryServerInfos = getRepositoryServerInfos(repositoryImport);
			if (repositoryServerInfos == null)
				return null;
			cacheParents(repositoryImport, repositoryServerInfos);
			return repositoryServerInfos
					.toArray(new RepositoryServerInfo[repositoryServerInfos
							.size()]);
		}
		return null;
	}

	private Collection<RepositoryServerInfo> getRepositoryServerInfos(
			CloneSourceProvider repositoryImport) {
		Collection<RepositoryServerInfo> repositoryServerInfos = null;
		IRepositoryServerProvider repositoryServerProvider;
		try {
			repositoryServerProvider = repositoryImport
					.getRepositoryServerProvider();
		} catch (CoreException e) {
			Activator.error(e.getLocalizedMessage(), e);
			return null;
		}
		if (repositoryServerProvider == null)
			return null;
		try {
			repositoryServerInfos = repositoryServerProvider
					.getRepositoryServerInfos();
		} catch (Exception e) {
			Activator.error(UIText.RepositoryLocationContentProvider_errorProvidingRepoServer, e);
		}
		return repositoryServerInfos;
	}

	private void cacheParents(CloneSourceProvider repositoryImport,
			Collection<RepositoryServerInfo> repositoryServerInfos) {
		for (RepositoryServerInfo info : repositoryServerInfos)
			parents.put(info, repositoryImport);
	}

}

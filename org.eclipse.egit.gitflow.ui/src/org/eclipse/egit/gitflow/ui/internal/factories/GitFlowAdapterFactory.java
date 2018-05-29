/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.factories;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;

/**
 * Get JGit repository for element selected in Git Flow UI.
 */
public class GitFlowAdapterFactory implements IAdapterFactory {
	@SuppressWarnings("unchecked")
	@Override
	public Repository getAdapter(Object adaptableObject, Class adapterType) {
		Repository repository = null;
		if (adaptableObject instanceof IResource) {
			IResource resource = (IResource) adaptableObject;
			repository = getRepository(resource);
		} else if (adaptableObject instanceof IHistoryView) {
			IHistoryView historyView = (IHistoryView) adaptableObject;
			IHistoryPage historyPage = historyView.getHistoryPage();
			Object input = historyPage.getInput();
			if (input instanceof RepositoryNode) {
				RepositoryNode node = (RepositoryNode) input;
				repository = node.getRepository();
			} else if (input instanceof IResource) {
				repository = getRepository((IResource) input);
			}
		} else if (adaptableObject instanceof ISelection) {
			IStructuredSelection structuredSelection = SelectionUtils
					.getStructuredSelection((ISelection) adaptableObject);
			repository = SelectionUtils.getRepository(structuredSelection);
		} else {
			throw new IllegalStateException();
		}

		return repository;
	}

	private Repository getRepository(IResource resource) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		return mapping != null ? mapping.getRepository() : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class[] getAdapterList() {
		return new Class[] { Repository.class };
	}
}

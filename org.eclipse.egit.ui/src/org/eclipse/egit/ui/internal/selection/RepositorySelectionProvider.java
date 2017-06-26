/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.util.function.Supplier;

import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;

/**
 * An {@link IPostSelectionProvider} that provides a selection containing the
 * current repository as determined by a {@link Supplier} if the base selection
 * provider does not have a selection.
 */
public class RepositorySelectionProvider extends AbstractSelectionProvider {

	private final ISelectionProvider baseProvider;

	private final Supplier<? extends Repository> repositoryProvider;

	private final ISelectionChangedListener selectionHook = event -> fireSelectionChanged(
			getSelectionListeners());

	private final ISelectionChangedListener postSelectionHook = event -> fireSelectionChanged(
			getPostSelectionListeners());

	/**
	 * Creates a new {@link RepositorySelectionProvider}. If the base provider
	 * yields an empty selection, it supplies the current repository as
	 * determined by the given {@link Supplier}.
	 *
	 * @param baseProvider
	 *            to use normally for selections
	 * @param repositoryProvider
	 *            to use to get the repository
	 */
	public RepositorySelectionProvider(ISelectionProvider baseProvider,
			Supplier<? extends Repository> repositoryProvider) {
		this.repositoryProvider = repositoryProvider;
		this.baseProvider = baseProvider;
		baseProvider.addSelectionChangedListener(selectionHook);
		if (baseProvider instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) baseProvider)
					.addPostSelectionChangedListener(postSelectionHook);
		}
	}

	@Override
	public ISelection getSelection() {
		ISelection selection = baseProvider.getSelection();
		if (selection.isEmpty() && selection instanceof IStructuredSelection) {
			Repository repository = repositoryProvider.get();
			if (repository != null) {
				return new StructuredSelection(
						new RepositoryNode(null, repository));
			}
		}
		return selection;
	}

	@Override
	public void setSelection(ISelection selection) {
		baseProvider.setSelection(selection);
	}
}

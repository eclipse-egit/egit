/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IServiceLocator;

/**
 * An {@link AbstractSourceProvider} that provides the current repository (based
 * on the current selection) as a variable in an Eclipse
 * {@link org.eclipse.core.expressions.IEvaluationContext}. To avoid interfering
 * with repository removal, which in EGit relies on {@link WeakReference}
 * semantics, the variable provides not the {@link Repository} directly but its
 * git directory as a string, which can then be used to obtain the
 * {@link Repository} instance via
 * {@link org.eclipse.egit.core.RepositoryCache#getRepository(java.io.File)}. If
 * no repository can be determined, the variable's value is the empty string.
 */
public class RepositorySourceProvider extends AbstractSourceProvider
		implements ISelectionListener, IWindowListener {

	/**
	 * Key for the new variable in the
	 * {@link org.eclipse.core.expressions.IEvaluationContext}; may be used in a
	 * &lt;with> element in plugin.xml to reference the variable.
	 */
	public static final String REPOSITORY_PROPERTY = "org.eclipse.egit.ui.currentRepository"; //$NON-NLS-1$

	/**
	 * Use a weak reference here to not preclude repository removal.
	 */
	private WeakReference<Repository> repositoryRef;

	@Override
	public void initialize(IServiceLocator locator) {
		super.initialize(locator);
		PlatformUI.getWorkbench().addWindowListener(this);
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().removeWindowListener(this);
		repositoryRef = null;
	}

	@Override
	public Map getCurrentState() {
		@SuppressWarnings("resource")
		Repository repository = repositoryRef == null ? null
				: repositoryRef.get();
		if (repository == null) {
			return Collections.singletonMap(REPOSITORY_PROPERTY, ""); //$NON-NLS-1$
		}
		return Collections.singletonMap(REPOSITORY_PROPERTY,
				repository.getDirectory().getAbsolutePath());
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { REPOSITORY_PROPERTY };
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		Repository newRepository;
		if (selection == null) {
			newRepository = null;
		} else {
			newRepository = SelectionUtils.getRepository(
					SelectionUtils.getStructuredSelection(selection));
		}
		@SuppressWarnings("resource")
		Repository currentRepository = repositoryRef == null ? null
				: repositoryRef.get();
		if (currentRepository == null && repositoryRef != null) {
			repositoryRef = null;
			if (newRepository == null) {
				// Last evaluation was non-null, but that repo has since gone.
				fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW,
						REPOSITORY_PROPERTY, ""); //$NON-NLS-1$
				return;
			}
		}
		if (currentRepository != newRepository) {
			if (newRepository != null) {
				repositoryRef = new WeakReference<>(newRepository);
				fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW,
						REPOSITORY_PROPERTY,
						newRepository.getDirectory().getAbsolutePath());
			} else {
				repositoryRef = null;
				fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW,
						REPOSITORY_PROPERTY, ""); //$NON-NLS-1$
			}
		}
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
		window.getSelectionService().addSelectionListener(this);
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
		window.getSelectionService().removeSelectionListener(this);
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		// Nothing to do
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		// Nothing to do
	}

}

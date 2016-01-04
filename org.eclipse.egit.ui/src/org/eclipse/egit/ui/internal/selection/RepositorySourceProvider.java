/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
 * {@link org.eclipse.core.expressions.IEvaluationContext}.
 */
public class RepositorySourceProvider extends AbstractSourceProvider
		implements ISelectionListener, IWindowListener {

	private final class UpdateSelectionJob extends Job {
		private ISelection sel;

		private UpdateSelectionJob() {
			super(UIText.RepositorySourceProvider_updateRepoSelection);
			setSystem(true);
			setUser(false);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ISelection s;
			synchronized (this) {
				// make sure we don't leak reference to the selection
				s = sel;
				sel = null;
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			Repository newRepository;
			if (s == null || s.isEmpty()) {
				newRepository = null;
			} else {
				IStructuredSelection ss = SelectionUtils
						.getStructuredSelection(s);
				if (ss.isEmpty()) {
					newRepository = null;
				} else {
					newRepository = SelectionUtils.getRepository(ss);
				}
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (repository != newRepository) {
				repository = newRepository;
				fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW,
						REPOSITORY_PROPERTY, repository);
			}
			return Status.OK_STATUS;
		}

		public void schedule(ISelection selection) {
			cancel();
			synchronized (this) {
				sel = selection;
			}
			schedule();
		}
	}

	/**
	 * Key for the new variable in the
	 * {@link org.eclipse.core.expressions.IEvaluationContext}; may be used in a
	 * &lt;with> element in plugin.xml to reference the variable.
	 */
	public static final String REPOSITORY_PROPERTY = "org.eclipse.egit.ui.currentRepository"; //$NON-NLS-1$

	private volatile Repository repository;

	private UpdateSelectionJob updateSelectionJob;

	@Override
	public void initialize(IServiceLocator locator) {
		super.initialize(locator);
		updateSelectionJob = new UpdateSelectionJob();
		PlatformUI.getWorkbench().addWindowListener(this);
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().removeWindowListener(this);
		repository = null;
		if (updateSelectionJob != null) {
			updateSelectionJob.cancel();
			updateSelectionJob = null;
		}

	}

	@Override
	public Map getCurrentState() {
		return Collections.singletonMap(REPOSITORY_PROPERTY, repository);
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { REPOSITORY_PROPERTY };
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		UpdateSelectionJob job = updateSelectionJob;
		if (job == null) {
			return;
		}
		job.schedule(selection);
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
		window.getSelectionService().addSelectionListener(this);
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
		window.getSelectionService().removeSelectionListener(this);
		updateSelectionJob.cancel();
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

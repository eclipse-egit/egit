/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * An {@link AbstractSourceProvider} that provides the current repository (based
 * on the current selection) as a variable in an Eclipse
 * {@link org.eclipse.core.expressions.IEvaluationContext}.
 */
public class RepositorySourceProvider extends AbstractSourceProvider
		implements ISelectionListener, IWindowListener {

	/**
	 * Key for the new variable in the
	 * {@link org.eclipse.core.expressions.IEvaluationContext}; may be used in a
	 * &lt;with> element in plugin.xml to reference the variable.
	 */
	public static final String REPOSITORY_PROPERTY = "org.eclipse.egit.ui.currentRepository"; //$NON-NLS-1$

	private volatile Repository repository;

	private volatile boolean updateInProgress;

	private UpdateSelectionJob updateSelectionJob;

	@Override
	public void initialize(final IServiceLocator locator) {
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
		if (updateInProgress) {
			// IEvaluationContext.UNDEFINED_VARIABLE seem to evaluate to false
			return Collections.singletonMap(REPOSITORY_PROPERTY, new Object());
		} else {
			return Collections.singletonMap(REPOSITORY_PROPERTY, repository);
		}
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
		if (!(selection instanceof IStructuredSelection)) {
			// we should not try to compute anything while user selects text:
			// this may show disturbing dialogs in case user just continuously
			// selects in the editor
			job.schedule(SelectionUtils.getStructuredSelection(selection));
		} else {
			job.schedule((IStructuredSelection) selection);
		}
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
		// Nothing to do
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
		// Nothing to do: this happens also if *any* dialog is opened
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		// Nothing to do
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		window.getSelectionService().addSelectionListener(this);
	}

	/**
	 * This method will block current thread and wait until the pending
	 * computation result is available. If no computation is running, it will
	 * immediately return last computed value or {@code null} if no value is
	 * known.
	 *
	 * @return latest repository state, can be {@code null}
	 */
	@Nullable
	public Repository waitFor() {
		UpdateSelectionJob job = updateSelectionJob;
		if (job == null || !updateInProgress) {
			return repository;
		}

		// NON UI caller goes this way.
		if (Display.getCurrent() == null) {
			while (updateInProgress) {
				try {
					Job.getJobManager().join(RepositorySourceProvider.class,
							new NullProgressMonitor());
				} catch (OperationCanceledException | InterruptedException e) {
					break;
				}
			}
			return repository;
		}

		// UI caller will get a nice progress dialog if it takes longer
		IProgressService service = PlatformUI.getWorkbench()
				.getService(IProgressService.class);
		final AtomicBoolean cancelled = new AtomicBoolean(false);
		while (updateInProgress && !cancelled.get()) {
			try {
				service.run(false, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						if (monitor.isCanceled()) {
							cancelled.set(true);
							return;
						}
						Job.getJobManager().join(RepositorySourceProvider.class,
								monitor);
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				break;
			}
		}
		return repository;
	}

	private final class UpdateSelectionJob extends Job {
		private IStructuredSelection sel;

		private UpdateSelectionJob() {
			super(UIText.RepositorySourceProvider_updateRepoSelection);
			setSystem(true);
			setUser(false);
		}

		@Override
		public boolean belongsTo(Object family) {
			return RepositorySourceProvider.class == family;
		}

		public void schedule(IStructuredSelection selection) {
			if (selection.isEmpty()) {
				// no need to do heavy weight job scheduling if nothing has to
				// be done
				boolean needUpdate;
				synchronized (this) {
					needUpdate = repository != null;
					repository = null;
				}
				if (needUpdate) {
					fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW,
							REPOSITORY_PROPERTY, null);
				}
				return;
			}

			// Whatever is being computed is obsolete now: cancel job...
			cancel();

			// Remember selection, mark us as busy and schedule
			synchronized (this) {
				updateInProgress = true;
				sel = selection;
			}
			schedule();
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IStructuredSelection s;
			synchronized (this) {
				// make sure we don't leak reference to the selection
				s = sel;
				sel = null;
			}
			if (s == null || monitor.isCanceled()) {
				return cancelled();
			}
			Repository newRepository = getRepository(s);
			boolean needUpdate;
			synchronized (this) {
				needUpdate = repository != newRepository;
				repository = newRepository;
				updateInProgress = false;
			}
			if (needUpdate) {
				fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW,
						REPOSITORY_PROPERTY, newRepository);
			}
			return Status.OK_STATUS;
		}

		IStatus cancelled() {
			synchronized (this) {
				updateInProgress = false;
			}
			return Status.CANCEL_STATUS;
		}
	}

	/**
	 * Figure out which repository to use. All selected resources must map to
	 * the same Git repository.
	 *
	 * @param selection
	 * @return repository for current project, or null
	 */
	@Nullable
	static Repository getRepository(@NonNull IStructuredSelection selection) {

		IPath[] locations = SelectionUtils.getSelectedLocations(selection);
		if (GitTraceLocation.SELECTION.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.SELECTION.getLocation(), "selection=" //$NON-NLS-1$
					+ selection + ", locations=" //$NON-NLS-1$
					+ Arrays.toString(locations));
		boolean hadNull = false;
		Repository result = null;
		for (IPath location : locations) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(location);
			Repository repo;
			if (mapping != null) {
				repo = mapping.getRepository();
			} else {
				// location is outside workspace
				repo = org.eclipse.egit.core.Activator.getDefault()
						.getRepositoryCache().getRepository(location);
			}
			if (repo == null) {
				hadNull = true;
			}
			if (result == null) {
				result = repo;
			}
			boolean mismatch = hadNull && result != null;
			if (mismatch || result != repo) {
				return null;
			}
		}

		if (result == null) {
			for (Object o : selection.toArray()) {
				Repository nextRepo = AdapterUtils.adapt(o, Repository.class);
				if (nextRepo != null && result != null && result != nextRepo) {
					return null;
				}
				result = nextRepo;
			}
		}
		return result;
	}
}

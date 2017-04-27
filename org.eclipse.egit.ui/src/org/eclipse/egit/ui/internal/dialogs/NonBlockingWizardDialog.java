/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A special-purpose {@link WizardDialog} whose
 * {@link #run(boolean, boolean, IRunnableWithProgress) run()} method really
 * runs the job in the background. Use this mechanism with care!
 */
public class NonBlockingWizardDialog extends WizardDialog {

	private Queue<BackgroundJob> jobs = new LinkedList<>();

	/**
	 * Creates a new {@link NonBlockingWizardDialog}.
	 *
	 * @param parentShell
	 *            for the dialog
	 * @param newWizard
	 *            to show in the dialog
	 */
	public NonBlockingWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);
	}

	@Override
	protected Control createContents(Composite parent) {
		parent.addDisposeListener(e -> cancelJobs());
		addPageChangedListener(e -> cancelJobs());
		return super.createContents(parent);
	}

	/**
	 * If {@code fork} is {@code true}, this implementation does <em>not</em>
	 * block but schedules a true background job. Such background jobs are
	 * queued and will execute one after another. They are canceled when the
	 * current wizard page changes, or when the dialog closes.
	 * </p>
	 */
	@Override
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable)
			throws InvocationTargetException, InterruptedException {
		if (!fork) {
			super.run(fork, cancelable, runnable);
		}
		run(runnable, null);
	}

	/**
	 * Runs the given {@code runnable} in a background job, reporting progress
	 * through the dialog's progress monitor, if any, and invoking
	 * {@code onCancel} if the job is canceled.
	 *
	 * @param runnable
	 *            to run
	 * @param onCancel
	 *            to run when the job is canceled; may be {@code null}
	 */
	public void run(IRunnableWithProgress runnable, Runnable onCancel) {
		Assert.isNotNull(runnable);
		synchronized (jobs) {
			BackgroundJob newJob = new BackgroundJob(runnable, onCancel,
					getCurrentPage());
			jobs.add(newJob);
			if (jobs.size() == 1) {
				newJob.schedule();
			}
		}
	}

	/**
	 * Cancels any currently scheduled background jobs.
	 */
	public void cancelJobs() {
		Job currentJob;
		synchronized (jobs) {
			currentJob = jobs.peek();
			jobs.clear();
		}
		if (currentJob != null) {
			currentJob.cancel();
		}
	}

	@Override
	public void showPage(IWizardPage page) {
		// Need to synchronize to avoid race condition with a background job
		// terminating.
		synchronized (jobs) {
			super.showPage(page);
		}
	}

	@Override
	public IWizardPage getCurrentPage() {
		synchronized (jobs) {
			return super.getCurrentPage();
		}
	}

	@Override
	protected final ProgressMonitorPart createProgressMonitorPart(
			Composite composite, GridLayout pmlayout) {
		return new BackgroundProgressMonitorPart(composite, pmlayout);
	}

	private void restoreFocus(Control focusControl) {
		if (focusControl != null && !focusControl.isDisposed()) {
			Shell shell = getShell();
			if (shell != null && !shell.isDisposed()
					&& focusControl.getShell() == shell) {
				focusControl.setFocus();
			}
		}
	}

	private class BackgroundProgressMonitorPart extends ProgressMonitorPart {

		private Job job;

		public BackgroundProgressMonitorPart(Composite parent, Layout layout) {
			super(parent, layout, true);
		}

		public void setJob(Job job) {
			this.job = job;
		}

		@Override
		public void beginTask(String name, int totalWork) {
			// Super implementation steals the focus and sets it to the
			// monitor part's stop button.
			Display display = this.getDisplay();
			Control focusControl = display.isDisposed() ? null
					: display.getFocusControl();
			super.beginTask(name, totalWork);
			restoreFocus(focusControl);
		}

		@Override
		public void setCanceled(boolean cancel) {
			super.setCanceled(cancel);
			if (cancel) {
				if (job != null) {
					job.cancel();
				}
			}
		}

		@Override
		public void done() {
			job = null;
			super.done();
		}
	}

	/**
	 * Copied from org.eclipse.jface.operation.AccumulatingProgressMonitor and
	 * made {@link #isCanceled()} also consider the progress monitor provided by
	 * the Job framework. Also handle disposal in {@link #done()}.
	 * <p>
	 * The resulting monitor can be used from any thread; progress reporting to
	 * the wrapped monitor will occur asynchronously in the UI thread.
	 */
	private static class ForwardingProgressMonitor
			extends ProgressMonitorWrapper {
		private Display display;

		private Collector collector;

		private IProgressMonitor jobMonitor;

		private String currentTask = ""; //$NON-NLS-1$

		private class Collector implements Runnable {
			private String taskName;

			private String subTask;

			private double worked;

			private IProgressMonitor monitor;

			public Collector(String taskName, String subTask, double work,
					IProgressMonitor monitor) {
				this.taskName = taskName;
				this.subTask = subTask;
				this.worked = work;
				this.monitor = monitor;
			}

			public void setTaskName(String name) {
				this.taskName = name;
			}

			public void worked(double workedIncrement) {
				this.worked = this.worked + workedIncrement;
			}

			public void subTask(String subTaskName) {
				this.subTask = subTaskName;
			}

			@Override
			public void run() {
				clearCollector(this);
				if (taskName != null) {
					monitor.setTaskName(taskName);
				}
				if (subTask != null) {
					monitor.subTask(subTask);
				}
				if (worked > 0) {
					monitor.internalWorked(worked);
				}
			}
		}

		/**
		 * Creates a progress monitor wrapping the given one that uses the given
		 * display.
		 *
		 * @param monitor
		 *            the actual progress monitor to be wrapped
		 * @param jobMonitor
		 *            auxiliary monitor to consider for isCanceled()
		 * @param display
		 *            the SWT display used to forward the calls to the wrapped
		 *            progress monitor
		 */
		public ForwardingProgressMonitor(IProgressMonitor monitor,
				IProgressMonitor jobMonitor, Display display) {
			super(monitor);
			Assert.isNotNull(display);
			this.display = display;
			this.jobMonitor = jobMonitor;
		}

		@Override
		public boolean isCanceled() {
			return jobMonitor.isCanceled() || super.isCanceled();
		}

		@Override
		public void beginTask(final String name, final int totalWork) {
			synchronized (this) {
				collector = null;
			}
			display.asyncExec(() -> {
				currentTask = name;
				getWrappedProgressMonitor().beginTask(name, totalWork);
			});
		}

		/**
		 * Clears the collector object used to accumulate work and subtask calls
		 * if it matches the given one.
		 *
		 * @param collectorToClear
		 */
		private synchronized void clearCollector(Collector collectorToClear) {
			// Check if the accumulator is still using the given collector.
			// If not, don't clear it.
			if (this.collector == collectorToClear) {
				this.collector = null;
			}
		}

		/**
		 * Creates a collector object to accumulate work and subtask calls.
		 *
		 * @param taskName
		 *            initial task name
		 * @param subTask
		 *            initial sub-task name
		 * @param work
		 *            initial work of the collector
		 */
		private void createCollector(String taskName, String subTask,
				double work) {
			collector = new Collector(taskName, subTask, work,
					getWrappedProgressMonitor());
			display.asyncExec(collector);
		}

		@Override
		public void done() {
			synchronized (this) {
				collector = null;
			}
			if (!display.isDisposed()) {
				display.asyncExec(() -> {
					try {
						getWrappedProgressMonitor().done();
					} catch (SWTException e) {
						// May occur if the wrapped monitor is some already
						// disposed control. ProgressMonitorPart is otherwise
						// careful not to do anything when it has been disposed,
						// but neglects to check in done(). Just ignore it.
					}
				});
			}
		}

		@Override
		public synchronized void internalWorked(final double work) {
			if (collector == null) {
				createCollector(null, null, work);
			} else {
				collector.worked(work);
			}
		}

		@Override
		public synchronized void setTaskName(final String name) {
			currentTask = name;
			if (collector == null) {
				createCollector(name, null, 0);
			} else {
				collector.setTaskName(name);
			}
		}

		@Override
		public synchronized void subTask(final String name) {
			if (collector == null) {
				createCollector(null, name, 0);
			} else {
				collector.subTask(name);
			}
		}

		@Override
		public void worked(int work) {
			internalWorked(work);
		}

		@Override
		public void clearBlocked() {
			// If this is a monitor that can report blocking do so.
			// Don't bother with a collector as this should only ever
			// happen once.
			IProgressMonitor wrapped = getWrappedProgressMonitor();
			if (!(wrapped instanceof IProgressMonitorWithBlocking)) {
				return;
			}

			display.asyncExec(() -> {
				((IProgressMonitorWithBlocking) wrapped).clearBlocked();
				Dialog.getBlockedHandler().clearBlocked();
			});
		}

		@Override
		public void setBlocked(final IStatus reason) {
			// If this is a monitor that can report blocking do so.
			// Don't bother with a collector as this should only ever
			// happen once and prevent any more progress.
			IProgressMonitor wrapped = getWrappedProgressMonitor();
			if (!(wrapped instanceof IProgressMonitorWithBlocking)) {
				return;
			}

			display.asyncExec(() -> {
				((IProgressMonitorWithBlocking) wrapped).setBlocked(reason);
				// Do not give a shell as we want it to block until it opens.
				Dialog.getBlockedHandler().showBlocked(wrapped, reason,
						currentTask);
			});
		}

	}

	private class BackgroundJob extends Job {

		private IRunnableWithProgress runnable;

		private Runnable onCancel;

		private IWizardPage page;

		public BackgroundJob(IRunnableWithProgress runnable, Runnable onCancel,
				IWizardPage page) {
			super(MessageFormat.format(
					UIText.NonBlockingWizardDialog_BackgroundJobName,
					page.getName()));
			this.runnable = runnable;
			this.onCancel = onCancel;
			this.page = page;
			this.addJobChangeListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					if (!PlatformUI.isWorkbenchRunning()) {
						return;
					}
					Display display = PlatformUI.getWorkbench().getDisplay();
					if (display == null || display.isDisposed()) {
						return;
					}
					display.syncExec(() -> {
						boolean hideProgress = false;
						synchronized (jobs) {
							Job currentJob = jobs.peek();
							if (currentJob == BackgroundJob.this) {
								jobs.poll();
								Job nextJob = jobs.peek();
								if (nextJob != null) {
									nextJob.schedule();
								} else {
									hideProgress = true;
								}
							} else if (currentJob == null) {
								hideProgress = true;
							}
							if (hideProgress) {
								IProgressMonitor uiMonitor = getProgressMonitor();
								if (uiMonitor instanceof ProgressMonitorPart) {
									ProgressMonitorPart part = ((ProgressMonitorPart) uiMonitor);
									if (!part.isDisposed()) {
										part.setVisible(false);
										part.removeFromCancelComponent(null);
									}
								}
							}
						}
					});
				}
			});
			this.setUser(false);
			this.setSystem(true);
		}

		@Override
		public boolean shouldRun() {
			synchronized (jobs) {
				return page == getCurrentPage() && jobs.peek() == this;
			}
		}

		@Override
		protected void canceling() {
			try {
				if (onCancel != null) {
					onCancel.run();
				}
			} finally {
				super.canceling();
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!shouldRun()) {
				// Should actually not occur. Just to be on the safe side.
				return Status.CANCEL_STATUS;
			}
			// Hook up monitors so that we can report progress in the UI.
			IProgressMonitor uiMonitor = getProgressMonitor();
			IProgressMonitor combinedMonitor;
			if (uiMonitor instanceof ProgressMonitorPart) {
				ProgressMonitorPart part = ((ProgressMonitorPart) uiMonitor);
				IProgressMonitor[] newMonitor = { null };
				Display display = PlatformUI.getWorkbench().getDisplay();
				if (display == null || display.isDisposed()) {
					return Status.CANCEL_STATUS;
				}
				display.syncExec(() -> {
					if (((ProgressMonitorPart) uiMonitor).isDisposed()) {
						return;
					}
					try {
						Control focusControl = display.getFocusControl();
						part.setVisible(true);
						part.attachToCancelComponent(null);
						// Attaching sets the focus to the stop button...
						restoreFocus(focusControl);
						if (part instanceof BackgroundProgressMonitorPart) {
							((BackgroundProgressMonitorPart) part)
									.setJob(BackgroundJob.this);
						}
						newMonitor[0] = new ForwardingProgressMonitor(uiMonitor,
								monitor, part.getDisplay());
					} catch (SWTException e) {
						return;
					}
				});
				combinedMonitor = newMonitor[0];
				if (combinedMonitor == null) {
					return Status.CANCEL_STATUS;
				}
			} else {
				combinedMonitor = monitor;
			}
			try {
				runnable.run(combinedMonitor);
			} catch (InvocationTargetException e) {
				return Activator.createErrorStatus(e.getLocalizedMessage(), e);
			} catch (InterruptedException e) {
				return Status.CANCEL_STATUS;
			} finally {
				monitor.done();
				if (combinedMonitor != monitor) {
					combinedMonitor.done();
				}
			}
			return Status.OK_STATUS;
		}

	}
}

/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;

/**
 * A {@code CancelableFuture} is a "Future" using Eclipse jobs to asynchronously
 * perform long tasks. The {@link #get() get()} method blocks until the result
 * is available or the future is canceled. The first call to {@link #get()
 * get()} starts a background job running the {@link #run(IProgressMonitor)
 * run()} operation, which is supposed to call {@link #set(Object) set()} to set
 * its result. Pre-fetching is possible by calling {@link #start()} directly.
 * <p>
 * The main differences to a {@link java.util.concurrent.FutureTask} are:
 * </p>
 * <ul>
 * <li>This class is not that much optimized.</li>
 * <li>Cancellation behaves differently. A {@code FutureTask} that is canceled
 * before completed ignores any result that might still arrive, and always says
 * it's been canceled. A {@code CancelableFuture} accepts a result even after
 * having been canceled, so a subsequent {@link #get()} may get that result. In
 * both, cancellation wakes up all waiting calls to {@link #get()}.</li>
 * <li>A {@code CancelableFuture} automatically runs the task in a background
 * job, whereas a {@code FutureTask} would need an extra
 * {@link java.util.concurrent.ExecutorService ExecutorService} to
 * asynchronously run its task.</li>
 * <li>This class uses Eclipse {@link Job}s instead of Threads directly.</li>
 * </ul>
 *
 * @param <V>
 *            the type of the result value
 */
public abstract class CancelableFuture<V> {

	/**
	 * Determines how to cancel a not yet completed future. Irrespective of the
	 * mechanism, the job may actually terminate normally, and subsequent calls
	 * to {@link CancelableFuture#get() get()} may return a result.
	 */
	public static enum CancelMode {
		/**
		 * Tries to cancel the job, which may decide to ignore the request.
		 * Callers to {@link CancelableFuture#get() get()} will remain blocked
		 * until the job terminates.
		 */
		CANCEL,
		/**
		 * Tries to cancel the job, which may decide to ignore the request.
		 * Outstanding {@link CancelableFuture#get() get()} calls will be woken
		 * up and may throw {@link InterruptedException} or return a result if
		 * the job terminated in the meantime.
		 */
		ABANDON,
		/**
		 * Tries to cancel the job, and if that doesn't succeed immediately,
		 * interrupts the job's thread. Outstanding calls to
		 * {@link CancelableFuture#get() get()} will be woken up and may throw
		 * {@link InterruptedException} or return a result if the job terminated
		 * in the meantime.
		 */
		INTERRUPT
	}

	private static enum State {
		PRISTINE, SCHEDULED, CANCELING, INTERRUPT, CANCELED, DONE
	}

	private State state = State.PRISTINE;

	private V result;

	private InterruptibleJob job;

	/**
	 * Tries to cancel the future. See {@link CancelMode} for semantics.
	 *
	 * @param cancellation
	 *            {@link CancelMode} defining how to cancel. If {@code null}
	 *            defaults to {@link CancelMode#CANCEL}.
	 *
	 * @return {@code true} if the future was canceled (its job is not running
	 *         anymore), {@code false} otherwise.
	 */
	public synchronized boolean cancel(CancelMode cancellation) {
		CancelMode mode = cancellation == null ? CancelMode.CANCEL
				: cancellation;
		switch (state) {
		case PRISTINE:
			finish(false);
			return true;
		case SCHEDULED:
			state = State.CANCELING;
			boolean canceled = job.cancel();
			if (canceled) {
				state = State.CANCELED;
			} else if (mode == CancelMode.INTERRUPT) {
				interrupt();
			} else if (mode == CancelMode.ABANDON) {
				notifyAll();
			}
			return canceled;
		case CANCELING:
			// cancel(CANCEL|ABANDON) was called before.
			if (mode == CancelMode.INTERRUPT) {
				interrupt();
			} else if (mode == CancelMode.ABANDON) {
				notifyAll();
			}
			return false;
		case INTERRUPT:
			if (mode != CancelMode.CANCEL) {
				notifyAll();
			}
			return false;
		case CANCELED:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Tells whether or not the future's background job is still running.
	 *
	 * @return {@code true} if the future's background job isn't running anymore
	 *         (was canceled or terminated normally)
	 */
	public synchronized boolean isFinished() {
		return state == State.CANCELED || state == State.DONE;
	}

	/**
	 * Tells whether the future completed normally.
	 *
	 * @return {@code true} if the future completed normally, {@code false}
	 *         otherwise
	 */
	public synchronized boolean isDone() {
		return state == State.DONE;
	}

	/**
	 * Retrieves the result. If the result is not yet available, the method
	 * blocks until it is or {@link #cancel(CancelMode)} is called with
	 * {@link CancelMode#ABANDON} or {@link CancelMode#INTERRUPT}.
	 *
	 * @return the result, which may be {@code null} if the future was canceled
	 * @throws InterruptedException
	 *             if waiting was interrupted
	 * @throws InvocationTargetException
	 *             if the future's job cannot be created
	 */
	public synchronized V get()
			throws InterruptedException, InvocationTargetException {
		switch (state) {
		case DONE:
		case CANCELED:
			return result;
		case PRISTINE:
			start();
			return get();
		default:
			wait();
			if (state == State.CANCELING || state == State.INTERRUPT) {
				// canceled with ABANDON or INTERRUPT
				throw new InterruptedException();
			}
			return get();
		}
	}

	private synchronized void finish(boolean done) {
		state = done ? State.DONE : State.CANCELED;
		job = null;
		try {
			done();
		} finally {
			// We're done, wake up all outstanding get() calls.
			notifyAll();
		}
	}

	private synchronized void interrupt() {
		state = State.INTERRUPT;
		job.interrupt();
		notifyAll(); // Abandon outstanding get() calls
	}

	/**
	 * Sets the future's result.
	 *
	 * @param value
	 *            to use as the result
	 */
	protected void set(V value) {
		result = value;
	}

	/**
	 * Called by {@link #start()} before the background job is scheduled.
	 * Subclasses may override to initialize data before the job starts.
	 *
	 * @throws InvocationTargetException
	 *             on errors
	 */
	protected void prepareRun() throws InvocationTargetException {
		// Default does nothing
	}

	/**
	 * Obtain a job title for the background job executing the future's task.
	 *
	 * @return the title
	 */
	protected String getJobTitle() {
		return ""; //$NON-NLS-1$
	}

	/**
	 * Performs the future's task.
	 *
	 * @param monitor
	 *            for progress reporting and cancellation
	 * @throws InterruptedException
	 *             if canceled
	 * @throws InvocationTargetException
	 *             on other errors
	 */
	protected abstract void run(IProgressMonitor monitor)
			throws InterruptedException, InvocationTargetException;

	/**
	 * Called when the future is done, i.e., either completed normally, was
	 * canceled, or failed to start. Subclasses may override to do clean-up.
	 */
	protected void done() {
		// Default does nothing
	}

	/**
	 * On the first call, starts a background job to fetch the result.
	 * Subsequent calls do nothing and return immediately. Before creating and
	 * scheduling the job, {@link #prepareRun()} is called.
	 *
	 * @throws InvocationTargetException
	 *             propagated from {@link #prepareRun()}
	 */
	public synchronized void start() throws InvocationTargetException {
		if (job != null || state != State.PRISTINE) {
			return;
		}
		try {
			prepareRun();
		} catch (InvocationTargetException e) {
			finish(false);
			throw e;
		}
		job = new InterruptibleJob(getJobTitle()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					CancelableFuture.this.run(monitor);
					return Status.OK_STATUS;
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				} catch (InvocationTargetException e) {
					synchronized (CancelableFuture.this) {
						if (state == State.CANCELING
								|| state == State.INTERRUPT) {
							return Status.CANCEL_STATUS;
						}
					}
					return Activator.createErrorStatus(e.getLocalizedMessage(),
							e);
				} catch (RuntimeException e) {
					return Activator.createErrorStatus(e.getLocalizedMessage(),
							e);
				}
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				IStatus status = event.getResult();
				finish(status != null && status.isOK());
			}

		});
		job.setUser(false);
		job.setSystem(true);
		state = State.SCHEDULED;
		job.schedule();
	}

	private static abstract class InterruptibleJob extends Job {

		public InterruptibleJob(String name) {
			super(name);
		}

		public void interrupt() {
			Thread thread = getThread();
			if (thread != null) {
				thread.interrupt();
			}
		}
	}
}

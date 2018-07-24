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
package org.eclipse.egit.ui.internal.decorators;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract base class for git decorators. It automatically listens to index
 * changes and fires {@link LabelProviderChangedEvent}s when the index diff
 * changes.
 */
public abstract class GitDecorator extends LabelProvider
		implements ILightweightLabelDecorator, IndexDiffChangedListener {

	private Object lock = new Object();

	/** Protected by lock's monitor. */
	private EventJob eventJob;

	/**
	 * Creates a new {@link GitDecorator}, registering to receive notifications
	 * about index changes.
	 */
	public GitDecorator() {
		org.eclipse.egit.core.Activator.getDefault().getIndexDiffCache()
				.addIndexDiffChangedListener(this);
	}

	@Override
	public void dispose() {
		org.eclipse.egit.core.Activator.getDefault().getIndexDiffCache()
				.removeIndexDiffChangedListener(this);
		Job job;
		synchronized (lock) {
			job = eventJob;
			eventJob = null;
		}
		if (job != null) {
			job.cancel();
		}
	}

	/**
	 * Posts an asynchronous {@link LabelProviderChangedEvent} invalidating all
	 * labels.
	 */
	protected void postLabelEvent() {
		getEventJob().post(this);
	}

	/**
	 * Posts a {@link LabelProviderChangedEvent} invalidating all labels.
	 */
	protected void fireLabelEvent() {
		LabelProviderChangedEvent event = new LabelProviderChangedEvent(this);
		// Re-trigger decoration process (in UI thread)
		PlatformUI.getWorkbench().getDisplay()
				.asyncExec(() -> fireLabelProviderChanged(event));
	}

	@Override
	public void indexDiffChanged(Repository repository,
			IndexDiffData indexDiffData) {
		postLabelEvent();
	}

	private EventJob getEventJob() {
		synchronized (lock) {
			if (eventJob == null) {
				eventJob = new EventJob(getName());
				eventJob.setSystem(true);
				eventJob.setUser(false);
			}
			return eventJob;
		}
	}

	/**
	 * @return a human-readable name for this decorator
	 */
	protected abstract String getName();

	/**
	 * Job reducing label events to prevent unnecessary (i.e. redundant) event
	 * processing. Each instance of a {@link GitDecorator} gets its own job.
	 */
	private static class EventJob extends Job {

		/**
		 * Constant defining the waiting time (in milliseconds) until an event
		 * is fired
		 */
		private static final long DELAY = 100L;

		private GitDecorator decorator;

		public EventJob(String name) {
			super(MessageFormat.format(UIText.GitDecorator_jobTitle, name));
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			if (decorator != null) {
				decorator.fireLabelEvent();
			}
			return Status.OK_STATUS;
		}

		public void post(GitDecorator source) {
			this.decorator = source;
			if (getState() == SLEEPING || getState() == WAITING) {
				cancel();
			}
			schedule(DELAY);
		}
	}
}

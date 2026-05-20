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
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jgit.events.ConfigChangedEvent;
import org.eclipse.jgit.events.ConfigChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract base class for git decorators. It automatically listens to index
 * changes and fires {@link LabelProviderChangedEvent}s when the index diff
 * changes.
 */
public abstract class GitDecorator extends LabelProvider
		implements ILightweightLabelDecorator, IndexDiffChangedListener,
		ConfigChangedListener {

	private static final Object UI_EVENT_LOCK = new Object();

	private static final Set<GitDecorator> uiEventQueue = new LinkedHashSet<>();

	private static boolean uiEventQueued;

	private Object lock = new Object();

	/** Protected by lock's monitor. */
	private EventJob eventJob;

	private ListenerHandle configListener;

	/**
	 * Creates a new {@link GitDecorator}, registering to receive notifications
	 * about index changes.
	 */
	public GitDecorator() {
		IndexDiffCache.INSTANCE.addIndexDiffChangedListener(this);
		configListener = RepositoryCache.INSTANCE.getGlobalListenerList()
				.addConfigChangedListener(this);
	}

	@Override
	public void dispose() {
		IndexDiffCache.INSTANCE.removeIndexDiffChangedListener(this);
		configListener.remove();
		configListener = null;
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
		// Re-trigger decoration process (in UI thread)
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display == null || display.isDisposed()) {
			return;
		}
		boolean scheduleUiRun = false;
		synchronized (UI_EVENT_LOCK) {
			uiEventQueue.add(this);
			if (!uiEventQueued) {
				uiEventQueued = true;
				scheduleUiRun = true;
			}
		}
		if (scheduleUiRun) {//
			display.asyncExec(GitDecorator::firePendingLabelEvents);
		}
	}

	@Override
	public void indexDiffChanged(Repository repository,
			IndexDiffData indexDiffData) {
		DecoratorRepositoryStateCache.INSTANCE.clear(repository);
		postLabelEvent();
	}

	@Override
	public void onConfigChanged(ConfigChangedEvent event) {
		DecoratorRepositoryStateCache.INSTANCE
				.resetBranchState(event.getRepository());
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

	private static void firePendingLabelEvents() {
		Set<GitDecorator> decorators;
		synchronized (UI_EVENT_LOCK) {
			uiEventQueued = false;
			if (uiEventQueue.isEmpty()) {
				return;
			}
			decorators = new LinkedHashSet<>(uiEventQueue);
			uiEventQueue.clear();
		}
		for (GitDecorator decorator : decorators) {
			// Check that the decorator hasn't been disposed already.
			if (decorator.configListener != null) {
				decorator.fireLabelProviderChanged(
						new LabelProviderChangedEvent(decorator));
			}
		}
	}

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

/*******************************************************************************
 * Copyright (C) 2025 EGit Committers and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.core.op.FetchOperation;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * A component that periodically fetches from remotes of all tracked
 * repositories in the background. Depending on the
 * {@link UIPreferences#AUTO_FETCH} setting, it fetches either only the default
 * remote or all remotes. The interval is controlled by
 * {@link UIPreferences#AUTO_FETCH_INTERVAL}.
 */
@Component(property = EventConstants.EVENT_TOPIC + '='
		+ ApplicationActiveListener.TOPIC_APPLICATION_ACTIVE)
public class AutoFetchComponent implements EventHandler {

	private AtomicBoolean isActive = new AtomicBoolean();

	private AutoFetchJob fetchJob;

	@Override
	public void handleEvent(Event event) {
		if (ApplicationActiveListener.TOPIC_APPLICATION_ACTIVE
				.equals(event.getTopic())) {
			Object value = event.getProperty(IEventBroker.DATA);
			if (value instanceof Boolean) {
				boolean newValue = ((Boolean) value).booleanValue();
				if (isActive.compareAndSet(!newValue, newValue) && newValue) {
					fetchJob.schedule();
				}
			}
		}
	}

	@Activate
	void startUp() {
		fetchJob = new AutoFetchJob(isActive);
		Activator.getDefault().getPreferenceStore()
				.addPropertyChangeListener(fetchJob);
	}

	@Deactivate
	void shutDown() {
		Activator.getDefault().getPreferenceStore()
				.removePropertyChangeListener(fetchJob);
		fetchJob.setReschedule(false);
		fetchJob.cancel();
		try {
			fetchJob.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// package-private for testability
	static class AutoFetchJob extends Job implements IPropertyChangeListener {

		// volatile to ensure thread synchronization
		private volatile boolean doReschedule;

		private volatile int interval;

		private final AtomicBoolean workbenchActive;

		AutoFetchJob(AtomicBoolean workbenchActive) {
			super(UIText.AutoFetchJob_Name);
			this.workbenchActive = workbenchActive;
			setSystem(true);
			setUser(false);
			updateFetchInterval();
		}

		@Override
		public boolean shouldSchedule() {
			return doReschedule;
		}

		@Override
		public boolean shouldRun() {
			return doReschedule;
		}

		void setReschedule(boolean reschedule) {
			doReschedule = reschedule;
		}

		@Override
		public boolean belongsTo(Object family) {
			return JobFamilies.AUTO_FETCH.equals(family)
					|| super.belongsTo(family);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IPreferenceStore store = Activator.getDefault()
					.getPreferenceStore();
			if (store.getBoolean(UIPreferences.REFRESH_ONLY_WHEN_ACTIVE)
					&& !workbenchActive.get()) {
				schedule(interval);
				return Status.OK_STATUS;
			}

			String autoFetch = store.getString(UIPreferences.AUTO_FETCH);
			boolean fetchAll = UIPreferences.AUTO_FETCH_ALL_REMOTES
					.equals(autoFetch);
			Repository[] repos = RepositoryCache.INSTANCE.getAllRepositories();
			SubMonitor progress = SubMonitor.convert(monitor, repos.length);
			for (Repository repo : repos) {
				if (monitor.isCanceled()) {
					break;
				}
				if (repo.isBare()) {
					progress.worked(1);
					continue;
				}
				try {
					List<RemoteConfig> remotes;
					if (fetchAll) {
						remotes = RemoteConfig
								.getAllRemoteConfigs(repo.getConfig());
					} else {
						String remoteName = getDefaultRemote(repo);
						remotes = List.of(new RemoteConfig(
								repo.getConfig(), remoteName));
					}
					for (RemoteConfig remote : remotes) {
						if (remote.getURIs().isEmpty()) {
							continue;
						}
						FetchOperation op = new FetchOperation(repo, remote,
								GitSettings.getRemoteConnectionTimeout(),
								false);
						op.setCredentialsProvider(
								new EGitCredentialsProvider());
						op.run(progress.newChild(0));
					}
				} catch (Exception e) {
					Activator.logError(
							"Auto fetch failed for " + repo, e); //$NON-NLS-1$
				}
				progress.worked(1);
			}
			monitor.done();
			schedule(interval);
			return Status.OK_STATUS;
		}

		private static String getDefaultRemote(Repository repo) {
			try {
				String branch = repo.getBranch();
				if (branch != null) {
					BranchConfig branchConfig = new BranchConfig(
							repo.getConfig(), branch);
					String remote = branchConfig.getRemote();
					if (remote != null && !remote.isEmpty()) {
						return remote;
					}
				}
			} catch (IOException e) {
				// fall through to default
			}
			return Constants.DEFAULT_REMOTE_NAME;
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			String prop = event.getProperty();
			if (UIPreferences.AUTO_FETCH.equals(prop)
					|| UIPreferences.AUTO_FETCH_INTERVAL.equals(prop)) {
				updateFetchInterval();
			}
		}

		private void updateFetchInterval() {
			IPreferenceStore store = Activator.getDefault()
					.getPreferenceStore();
			String autoFetch = store.getString(UIPreferences.AUTO_FETCH);
			boolean enabled = !UIPreferences.AUTO_FETCH_DISABLED
					.equals(autoFetch);
			interval = enabled
					? 1000 * store.getInt(UIPreferences.AUTO_FETCH_INTERVAL)
					: 0;
			setReschedule(interval > 0);
			cancel();
			if (interval > 0) {
				schedule(interval);
			}
		}
	}
}

/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Utility class that launches model synchronization action
 */
public class GitModelSynchronize {

	private static final String providerId = "org.eclipse.egit.core.GitProvider"; //$NON-NLS-1$

	/**
	 * Launches Git Model synchronization action
	 *
	 * @param data
	 * @param resources
	 */
	public static final void launch(GitSynchronizeData data,
			IResource[] resources) {
		launch(new GitSynchronizeDataSet(data), resources);
	}

	/**
	 * Launches Git Model synchronization action
	 *
	 * @param gsdSet
	 * @param resources
	 */
	public static final void launch(final GitSynchronizeDataSet gsdSet,
			IResource[] resources) {
		final ResourceMapping[] mappings = getSelectedResourceMappings(resources);

		final IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();

		boolean launchFetch = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH);
		if (launchFetch) {
			Job fetchJob = new SynchronizeFetchJob(gsdSet);
			fetchJob.setUser(true);
			fetchJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					fireSynchronizeAction(window, gsdSet, mappings);
				}
			});

			fetchJob.schedule();
		} else {
			fireSynchronizeAction(window, gsdSet, mappings);
		}
	}

	/**
	 * Based on org.eclipse.team.internal.ui.actions.TeamAction#getSelectedResourceMappings
	 *
	 * @param elements
	 * @return the resource mappings that contain resources associated with the
	 *         given provider
	 */
	private static ResourceMapping[] getSelectedResourceMappings(
			IResource[] elements) {
		List<ResourceMapping> providerMappings = new ArrayList<ResourceMapping>();

		for (IResource element : elements) {
			Object adapted = getResourceMapping(element);
			if (adapted != null && adapted instanceof ResourceMapping) {
				ResourceMapping mapping = (ResourceMapping) adapted;

				if (isMappedToProvider(mapping))
					providerMappings.add(mapping);
			}
		}

		return providerMappings.toArray(new ResourceMapping[providerMappings
				.size()]);
	}

	/**
	 * Copied from TeamAction#getResourceMapping(Object)
	 *
	 * @param object
	 * @return resource mapping
	 */
	private static Object getResourceMapping(Object object) {
		if (object instanceof ResourceMapping)
			return object;

		if (object instanceof IAdaptable)
			return ((IAdaptable) object).getAdapter(ResourceMapping.class);

		return null;
	}

	/**
	 * Copied from TeamAction#isMappedToProvider(ResourceMapping, String)
	 *
	 * @param element
	 * @return <code>true</code> if resource is mapped to Git provider,
	 *         <code>false</code> otherwise
	 */
	private static boolean isMappedToProvider(ResourceMapping element) {
		IProject[] projects = element.getProjects();
		for (IProject project: projects) {
			RepositoryProvider provider = RepositoryProvider
					.getProvider(project);

			if (provider != null && provider.getID().equals(providerId))
				return true;
		}
		return false;
	}

	private static void fireSynchronizeAction(final IWorkbenchWindow window,
			final GitSynchronizeDataSet gsdSet, final ResourceMapping[] mappings) {
		final GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsdSet);

		Job syncJob = new Job(UIText.GitModelSynchonize_fetchGitDataJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				subscriber.init(monitor);

				return Status.OK_STATUS;
			}
		};

		syncJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				RemoteResourceMappingContext remoteContext = new GitSubscriberResourceMappingContext(subscriber,
						gsdSet);
				SubscriberScopeManager manager = new SubscriberScopeManager(
						subscriber.getName(), mappings, subscriber,
						remoteContext, true);
				GitSubscriberMergeContext context = new GitSubscriberMergeContext(
						subscriber, manager, gsdSet);
				final GitModelSynchronizeParticipant participant = new GitModelSynchronizeParticipant(
						context);

				TeamUI.getSynchronizeManager().addSynchronizeParticipants(
						new ISynchronizeParticipant[] { participant });

				IWorkbenchPart activePart = null;
				if (window != null)
					activePart = window.getActivePage().getActivePart();

				participant.run(activePart);
			}
		});

		syncJob.setUser(true);
		syncJob.schedule();
	}

	private GitModelSynchronize() {
		// non instantiable class
	}
}

/*******************************************************************************
 * Copyright (C) 2010, 2012 Dariusz Luksza <dariusz@luksza.org>.
 * Copyright (C) 2012, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.AdapterUtils;
import org.eclipse.egit.core.internal.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.internal.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.internal.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.internal.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.internal.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
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

	private static final String GIT_PROVIDER_ID = "org.eclipse.egit.core.GitProvider"; //$NON-NLS-1$

	/**
	 * Launches Git Model synchronize operation for synchronizing a set of
	 * resources in the workspace with a remote reference.
	 *
	 * @param file
	 *            the file to synchronize (will also be used to lookup resouce
	 *            mappings)
	 * @param repository
	 *            the repository
	 * @param refName
	 *            the reference to synchronize with
	 * @throws IOException
	 */
	public static final void synchronizeModelWithWorkspace(IFile file,
			Repository repository, String refName) throws IOException {
		synchronizeModel(file, new GitSynchronizeData(repository,
				Constants.HEAD, refName, true));
	}

	/**
	 * Launches Git Model synchronize operation for synchronizing a set of
	 * resources in the workspace with a remote reference.
	 *
	 * @param file
	 *            the file to synchronize (will also be used to lookup resouce
	 *            mappings)
	 * @param repository
	 *            the repository
	 * @param sourceRef
	 *            the source reference
	 * @param destinationRef
	 *            the destination reference
	 * @throws IOException
	 */
	public static final void synchronizeModelBetweenRefs(IFile file,
			Repository repository, String sourceRef, String destinationRef) throws IOException {
		synchronizeModel(file, new GitSynchronizeData(repository,
				sourceRef, destinationRef, false));
	}

	private static void synchronizeModel(IFile file, final GitSynchronizeData data) {
		final GitSynchronizeDataSet dataSet = new GitSynchronizeDataSet(data);

		// use all available local mappings for proper model support
		final ResourceMapping[] mappings = ResourceUtil.getResourceMappings(
				file, ResourceMappingContext.LOCAL_CONTEXT);

		GitModelSynchronize.launch(dataSet, mappings);
	}

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
		ResourceMapping[] mappings = getGitResourceMappings(resources);

		launch(gsdSet, mappings);
	}

	/**
	 * Launches Git Model synchronization action using the specified resource
	 * mapping
	 *
	 * @param gsdSet
	 * @param mappings
	 */
	public static final void launch(final GitSynchronizeDataSet gsdSet,
			ResourceMapping[] mappings) {

		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();

		fireSynchronizeAction(window, gsdSet, mappings);
	}

	/**
	 * Based on org.eclipse.team.internal.ui.actions.TeamAction#getSelectedResourceMappings
	 *
	 * @param elements
	 * @return the resource mappings that contain resources associated with the
	 *         given provider
	 */
	private static ResourceMapping[] getGitResourceMappings(
			IResource[] elements) {
		List<ResourceMapping> gitMappings = new ArrayList<ResourceMapping>();

		for (IResource element : elements) {
			ResourceMapping mapping = AdapterUtils.adapt(element,
					ResourceMapping.class);
			if (mapping != null && isMappedToGitProvider(mapping))
				gitMappings.add(mapping);
		}

		return gitMappings.toArray(new ResourceMapping[gitMappings.size()]);
	}

	/**
	 * Copied from TeamAction#isMappedToProvider(ResourceMapping, String)
	 *
	 * @param element
	 * @return <code>true</code> if resource is mapped to Git provider,
	 *         <code>false</code> otherwise
	 */
	private static boolean isMappedToGitProvider(ResourceMapping element) {
		IProject[] projects = element.getProjects();
		for (IProject project: projects) {
			RepositoryProvider provider = RepositoryProvider
					.getProvider(project);

			if (provider != null && provider.getID().equals(GIT_PROVIDER_ID))
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
			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.SYNCHRONIZE_READ_DATA.equals(family))
					return true;

				return super.belongsTo(family);
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

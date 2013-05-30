/*******************************************************************************
 * Copyright (C) 2010, 2012 Dariusz Luksza <dariusz@luksza.org>.
 * Copyright (C) 2012, 2013 Laurent Goubet <laurent.goubet@obeo.fr>
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Utility class that launches model synchronization action
 */
public class GitModelSynchronize {

	private static final String GIT_PROVIDER_ID = "org.eclipse.egit.core.GitProvider"; //$NON-NLS-1$

	/**
	 * This can be used to open the synchronize view for the given set of
	 * resources, comparing the given revisions together.
	 * <p>
	 * Note that this falls back to the git tree compare view if the destination
	 * revision is the index.
	 * </p>
	 *
	 * @param resources
	 *            The set of resources to synchronize. Can be empty (in which
	 *            case we'll synchronize the whole repository).
	 * @param repository
	 *            The repository to load file revisions from.
	 * @param srcRev
	 *            Source revision of the synchronization (or "left" side).
	 * @param dstRev
	 *            Destination revision of the synchronization ("right" side).
	 * @param includeLocal
	 *            If <code>true</code>, this will use local data for the "left"
	 *            side of the synchronization.
	 * @throws IOException
	 */
	public static final void synchronize(IResource[] resources,
			Repository repository, String srcRev, String dstRev,
			boolean includeLocal) throws IOException {
		final Set<IResource> includedResources = new HashSet<IResource>(
				Arrays.asList(resources));
		final Set<ResourceMapping> allMappings = new HashSet<ResourceMapping>();

		Set<IResource> newResources = new HashSet<IResource>(
				includedResources);
		do {
			final Set<IResource> copy = newResources;
			newResources = new HashSet<IResource>();
			for (IResource resource : copy) {
				ResourceMapping[] mappings = ResourceUtil.getResourceMappings(
						resource, ResourceMappingContext.LOCAL_CONTEXT);
				allMappings.addAll(Arrays.asList(mappings));
				newResources.addAll(collectResources(mappings));
			}
		} while (includedResources.addAll(newResources));

		if (dstRev == GitFileRevision.INDEX) {
			final IResource[] resourcesArray = includedResources
					.toArray(new IResource[includedResources.size()]);
			openGitTreeCompare(resourcesArray, srcRev,
					CompareTreeView.INDEX_VERSION);
		} else if (srcRev == GitFileRevision.INDEX) {
			// Even git tree compare cannot handle index as source...
			// Synchronize using the local data for now.
			final ResourceMapping[] mappings = allMappings
					.toArray(new ResourceMapping[allMappings.size()]);
			final GitSynchronizeData data = new GitSynchronizeData(repository,
					srcRev, dstRev, true);
			launch(new GitSynchronizeDataSet(data), mappings);
		} else {
			final ResourceMapping[] mappings = allMappings
					.toArray(new ResourceMapping[allMappings.size()]);
			final GitSynchronizeData data = new GitSynchronizeData(repository,
					srcRev, dstRev, includeLocal);
			launch(new GitSynchronizeDataSet(data), mappings);
		}
	}

	private static Set<IResource> collectResources(ResourceMapping[] mappings) {
		final Set<IResource> resources = new HashSet<IResource>();
		ResourceMappingContext context = ResourceMappingContext.LOCAL_CONTEXT;
		for (ResourceMapping mapping : mappings) {
			try {
				ResourceTraversal[] traversals = mapping.getTraversals(context,
						new NullProgressMonitor());
				for (ResourceTraversal traversal : traversals)
					resources.addAll(Arrays.asList(traversal.getResources()));
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return resources;
	}

	private static void openGitTreeCompare(IResource[] resources,
			String srcRev, String dstRev) {
		CompareTreeView view;
		try {
			view = (CompareTreeView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.showView(CompareTreeView.ID);
			view.setInput(resources, srcRev, dstRev);
		} catch (PartInitException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
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

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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

/**
 *
 */
public class GitModelSynchronize {

	/**
	 * @param data
	 * @param resources
	 */
	public GitModelSynchronize(GitSynchronizeData data, IResource[] resources) {
		this(new GitSynchronizeDataSet(data), resources);
	}

	/**
	 * @param gsdSet
	 * @param resources
	 */
	public GitModelSynchronize(GitSynchronizeDataSet gsdSet,
			IResource[] resources) {
		ResourceMapping[] mappings = getSelectedResourceMappings(resources);

		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsdSet);
		RemoteResourceMappingContext remoteContext = new GitSubscriberResourceMappingContext(
				gsdSet);
		SubscriberScopeManager manager = new SubscriberScopeManager(
				subscriber.getName(), mappings, subscriber, remoteContext, true);
		SynchronizationContext context = new GitSubscriberMergeContext(
				subscriber, manager, gsdSet);
		GitModelSynchronizeParticipant participant = new GitModelSynchronizeParticipant(
				context);

		TeamUI.getSynchronizeManager().addSynchronizeParticipants(
				new ISynchronizeParticipant[] { participant });
		participant.run(getTargetPart());
	}

	private IWorkbenchPart getTargetPart() {
		IWorkbenchPart targetPart = null;
		IWorkbenchPage page = TeamUIPlugin.getActivePage();
		if (page != null) {
			targetPart = page.getActivePart();
		}
		return targetPart;
	}

	/**
	 * Based on {@link TeamAction#getSelectedResourceMappings}
	 *
	 * @param elements
	 * @return the resource mappings that contain resources associated with the
	 *         given provider
	 */
	private ResourceMapping[] getSelectedResourceMappings(IResource[] elements) {
		ArrayList providerMappings = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			Object object = elements[i];
			Object adapted = getResourceMapping(object);
			if (adapted instanceof ResourceMapping) {
				ResourceMapping mapping = (ResourceMapping) adapted;
				if (isMappedToProvider(mapping,
						"org.eclipse.egit.core.GitProvider")) { //$NON-NLS-1$
					providerMappings.add(mapping);
				}
			}
		}
		return (ResourceMapping[]) providerMappings
				.toArray(new ResourceMapping[providerMappings.size()]);
	}

	/**
	 * Copied from TeamAction#getResourceMapping(Object)
	 *
	 * @param object
	 * @return resource mapping
	 */
	private Object getResourceMapping(Object object) {
		if (object instanceof ResourceMapping)
			return object;

		if (object instanceof IAdaptable)
			return ((IAdaptable) object).getAdapter(ResourceMapping.class);

		return Utils.getResourceMapping(object);
	}

	/**
	 * Copied from TeamAction#isMappedToProvider(ResourceMapping, String)
	 *
	 * @param element
	 * @param providerId
	 * @return TODO
	 */
	private boolean isMappedToProvider(ResourceMapping element,
			String providerId) {
		IProject[] projects = element.getProjects();
		for (int k = 0; k < projects.length; k++) {
			IProject project = projects[k];
			RepositoryProvider provider = RepositoryProvider
					.getProvider(project);
			if (provider != null && provider.getID().equals(providerId)) {
				return true;
			}
		}
		return false;
	}
}

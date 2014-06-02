/*******************************************************************************
 * Copyright (C) 2011, Tasktop Technologies Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla (benjamin.muskalla@tasktop.com) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.progress.IProgressService;

/**
 * Utilities to handle {@link ResourceMapping}s and {@link GitScopeOperation}
 */
public class GitScopeUtil {

	/**
	 * Returns the set of {@link IResource}s that need to be operated on to have
	 * a consistent model. Model providers will be asked which resources are
	 * relevant. The user will be informed which resources are not in the scope
	 * of the current operation and will be added to the current.
	 *
	 * @param part
	 *            the active workbench part
	 * @param resources
	 *            the resources to operate on
	 * @return returns the set of resources to operate on
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static IResource[] getRelatedChanges(IWorkbenchPart part,
			IResource[] resources) throws ExecutionException,
			InterruptedException {
		if (part == null)
			throw new IllegalArgumentException();
		if (resources == null)
			return new IResource[0];
		IResource[] resourcesInScope;
		// Only builds the logical model if the preference holds true
		if (Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.USE_LOGICAL_MODEL)) {

			try {
				resourcesInScope = findRelatedChanges(part, resources);
			} catch (InvocationTargetException e) {
				Activator.handleError(
						UIText.CommitActionHandler_errorBuildingScope,
						e.getCause(), true);
				// fallback to initial resource set
				resourcesInScope = resources;
			}
		} else {
			resourcesInScope = resources;
		}
		return resourcesInScope;
	}

	/**
	 * Creates a new {@link SubscriberScopeManager} for the given set of
	 * {@link IResource}s
	 *
	 * @param resources
	 * @return {@link SubscriberScopeManager}
	 */
	private static SubscriberScopeManager createScopeManager(
			final IResource[] resources) {
		ResourceMapping[] mappings = GitScopeUtil
				.getResourceMappings(resources);
		GitSynchronizeDataSet set = new GitSynchronizeDataSet();
		Subscriber subscriber = new GitResourceVariantTreeSubscriber(set);
		SubscriberScopeManager manager = new SubscriberScopeManager(
				UIText.GitScopeOperation_GitScopeManager, mappings, subscriber,
				true);
		return manager;
	}

	/**
	 * Returns all resource mappings for the given resources
	 *
	 * @param resources
	 * @return ResourceMappings
	 */
	private static ResourceMapping[] getResourceMappings(IResource[] resources) {
		Set<ResourceMapping> result = new LinkedHashSet<ResourceMapping>();
		for (IResource resource : resources) {
			ResourceMapping[] additional = ResourceUtil.getResourceMappings(
					resource, ResourceMappingContext.LOCAL_CONTEXT);
			result.addAll(Arrays.asList(additional));
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	private static IResource[] findRelatedChanges(final IWorkbenchPart part,
			final IResource[] selectedResources)
			throws InvocationTargetException, InterruptedException {

		final List<IResource> relatedChanges = new ArrayList<IResource>();
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				try {
					monitor.beginTask(
							UIText.CommitActionHandler_lookingForChanges, 100);
					List<IResource> collectedResources = collectRelatedChanges(
							selectedResources, part, monitor);
					relatedChanges.addAll(collectedResources);
				} finally {
					monitor.done();
				}
			}

		};

		IProgressService progressService = (IProgressService) part.getSite()
				.getService(IProgressService.class);
		progressService.run(true, true, runnable);

		return relatedChanges.toArray(new IResource[relatedChanges.size()]);
	}

	private static List<IResource> collectRelatedChanges(
			IResource[] selectedResources, IWorkbenchPart part,
			IProgressMonitor monitor) throws InterruptedException,
			InvocationTargetException {

		SubscriberScopeManager manager = GitScopeUtil
				.createScopeManager(selectedResources);
		GitScopeOperation buildScopeOperation = GitScopeOperationFactory
				.getFactory().createGitScopeOperation(part, manager);

		buildScopeOperation.run(new SubProgressMonitor(monitor, 50));

		return buildScopeOperation.getRelevantResources();
	}

}

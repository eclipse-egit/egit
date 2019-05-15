/*******************************************************************************
 * Copyright (C) 2011, 2015 Tasktop Technologies Inc. and others.
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla (benjamin.muskalla@tasktop.com) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ide.IContributorResourceAdapter2;
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
	 * @param monitor
	 * @return {@link SubscriberScopeManager}
	 */
	private static SubscriberScopeManager createScopeManager(
			final IResource[] resources, IProgressMonitor monitor) {
		ResourceMapping[] mappings = GitScopeUtil
				.getResourceMappings(resources);
		GitSynchronizeDataSet set = new GitSynchronizeDataSet();
		final GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				set);
		monitor.setTaskName(UIText.GitModelSynchronize_fetchGitDataJobName);
		subscriber.init(monitor);
		SubscriberScopeManager manager = new SubscriberScopeManager(
				UIText.GitScopeOperation_GitScopeManager, mappings, subscriber,
				true);
		return manager;
	}

	@Nullable
	private static ResourceMapping getResourceMapping(Object o) {
		ResourceMapping mapping = Adapters.adapt(o, ResourceMapping.class);
		if (mapping != null) {
			return mapping;
		}
		if (o instanceof IAdaptable) {
			IContributorResourceAdapter adapted = Adapters.adapt(o,
					IContributorResourceAdapter.class);
			if (adapted instanceof IContributorResourceAdapter2) {
				IContributorResourceAdapter2 cra = (IContributorResourceAdapter2) adapted;
				return cra.getAdaptedResourceMapping((IAdaptable) o);
			}
		}
		return null;
	}

	/**
	 * Returns all resource mappings for the given resources
	 *
	 * @param resources
	 * @return ResourceMappings
	 */
	private static ResourceMapping[] getResourceMappings(IResource[] resources) {
		List<ResourceMapping> result = new ArrayList<>();
		for (IResource resource : resources)
			result.add(getResourceMapping(resource));
		return result.toArray(new ResourceMapping[0]);
	}

	private static IResource[] findRelatedChanges(final IWorkbenchPart part,
			final IResource[] selectedResources)
			throws InvocationTargetException, InterruptedException {

		final List<IResource> relatedChanges = new ArrayList<>();
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
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

		IProgressService progressService = CommonUtils.getService(part.getSite(), IProgressService.class);
		progressService.run(true, true, runnable);

		return relatedChanges.toArray(new IResource[0]);
	}

	private static List<IResource> collectRelatedChanges(
			IResource[] selectedResources, IWorkbenchPart part,
			IProgressMonitor monitor) throws InterruptedException,
			InvocationTargetException {

		SubMonitor progress = SubMonitor.convert(monitor, 2);
		SubscriberScopeManager manager = GitScopeUtil.createScopeManager(
				selectedResources, progress.newChild(1));
		GitScopeOperation buildScopeOperation = GitScopeOperationFactory
				.getFactory().createGitScopeOperation(part, manager);

		buildScopeOperation.run(progress.newChild(1));

		return buildScopeOperation.getRelevantResources();
	}

}

/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObjectContainer;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRoot;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.ui.mapping.SynchronizationContentProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Git's variant of {@link SynchronizationContentProvider}. This class creates
 * model root of change set and base {@link ResourceTraversal}'s.
 */
public class GitChangeSetContentProvider extends SynchronizationContentProvider {

	private ITreeContentProvider provider;

	private GitModelRoot modelRoot;

	private Map<Object, ResourceTraversal[]> traversalCache = new HashMap<>();

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof GitModelBlob)
			return false;

		if (element instanceof GitModelObjectContainer)
			return ((GitModelObjectContainer) element).getChildren().length > 0;

		return super.hasChildren(element);
	}

	@Override
	protected ITreeContentProvider getDelegateContentProvider() {
		if (provider == null)
			provider = new WorkbenchContentProvider();

		return provider;
	}

	@Override
	protected String getModelProviderId() {
		return GitChangeSetModelProvider.ID;
	}

	@Override
	protected Object getModelRoot() {
		if (modelRoot == null) {
			GitSubscriberMergeContext context = (GitSubscriberMergeContext) getContext();
			modelRoot = new GitModelRoot(context.getSyncData());
		}

		return modelRoot;
	}

	@Override
	protected ResourceTraversal[] getTraversals(
			ISynchronizationContext context, Object object) {
		if (object instanceof IAdaptable) {
			if (traversalCache.containsKey(object)) {
				return traversalCache.get(object);
			}

			ResourceMapping rm = getResourceMapping(object);
			if(rm == null){
				return null;
			}
			GitSubscriberMergeContext ctx = (GitSubscriberMergeContext) getContext();
			ResourceMappingContext rmCtx = new GitSubscriberResourceMappingContext(
					(GitResourceVariantTreeSubscriber) ctx.getSubscriber(),
					ctx.getSyncData());
			try {
				ResourceTraversal[] traversals = rm.getTraversals(rmCtx, new NullProgressMonitor());
				traversalCache.put(object, traversals);
				return traversals;
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return null;
	}

	@Nullable
	private ResourceMapping getResourceMapping(Object object) {
		return AdapterUtils.adapt(object, ResourceMapping.class);
	}

	@Override
	public void dispose() {
		if (provider != null)
			provider.dispose();
		if (modelRoot != null)
			modelRoot.dispose();
		traversalCache.clear();

		super.dispose();
	}

	@Override
	protected void refresh() {
		traversalCache.clear();
		super.refresh();
	}

	@Override
	protected boolean isVisible(ISynchronizationContext context, Object object) {
		if (object instanceof GitModelRepository
				|| object instanceof GitModelCommit
				|| object instanceof GitModelCache) {
			int kind = ((GitModelObjectContainer) object).getKind();
			switch (getConfiguration().getMode()) {
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				return (kind & Differencer.RIGHT) != 0;
			case ISynchronizePageConfiguration.INCOMING_MODE:
				return (kind & Differencer.LEFT) != 0;
			default:
				break;
			}
		}
		return true;
	}
}

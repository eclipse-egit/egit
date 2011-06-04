/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.ui.internal.synchronize.GitChangeSetModelProvider;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObjectContainer;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
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

	private Map<Object, ResourceTraversal[]> traversalCache = new HashMap<Object, ResourceTraversal[]>();

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
			if (traversalCache.containsKey(object))
				return traversalCache.get(object);

			ResourceMapping rm = getResourceMapping(object);
			GitSubscriberMergeContext ctx = (GitSubscriberMergeContext) getContext();
			ResourceMappingContext rmCtx = new GitSubscriberResourceMappingContext(
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

	private ResourceMapping getResourceMapping(Object object) {
		return (ResourceMapping) ((IAdaptable) object)
				.getAdapter(ResourceMapping.class);
	}

	@Override
	public void dispose() {
		if (provider != null)
			provider.dispose();

		super.dispose();
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		List<IPath> toRefresh = new LinkedList<IPath>();
		for (IDiff addition : event.getAdditions())
			toRefresh.add(addition.getPath());
		for (IDiff changed : event.getChanges())
			toRefresh.add(changed.getPath());


		toRefresh.addAll(Arrays.asList(event.getRemovals()));
		for (IPath refresh : toRefresh)
			for (GitModelObject child : modelRoot.getChildren())
				if (child instanceof GitModelCache)
					for (IProject project : child.getProjects())
						if (project.getName().equals(refresh.segment(0))) {
							child.refresh();
							break;
						}

		super.diffsChanged(event, monitor);
	}

	@Override
	protected void refresh() {
		traversalCache.clear();
		super.refresh();
	}

	protected boolean isVisible(ISynchronizationContext context, Object object) {
		if (object instanceof GitModelCommit) {
			int kind = ((GitModelCommit) object).getKind();
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

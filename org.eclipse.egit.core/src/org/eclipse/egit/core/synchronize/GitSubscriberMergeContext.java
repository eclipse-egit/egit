/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.GitResourceDeltaVisitor;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;

/**
 *
 */
public class GitSubscriberMergeContext extends SubscriberMergeContext {

	private final GitSynchronizeDataSet gsds;

	private final IndexDiffChangedListener indexChangeListener;

	private final IResourceChangeListener resourceChangeListener;

	private final GitResourceVariantTreeSubscriber subscriber;

	/**
	 * @param subscriber
	 * @param manager
	 * @param gsds
	 */
	public GitSubscriberMergeContext(final GitResourceVariantTreeSubscriber subscriber,
			ISynchronizationScopeManager manager, GitSynchronizeDataSet gsds) {
		super(subscriber, manager);
		this.subscriber = subscriber;
		this.gsds = gsds;


		indexChangeListener = (repository,
				indexDiffData) -> handleRepositoryChange(repository);
		resourceChangeListener = event -> {
			IResourceDelta delta = event.getDelta();
			if (delta != null) {
				handleResourceChange(delta);
			}
		};
		IndexDiffCache indexDiffCache = Activator.getDefault().getIndexDiffCache();
		if (indexDiffCache != null)
			indexDiffCache.addIndexDiffChangedListener(indexChangeListener);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);

		initialize();
	}

	@Override
	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		IResource resource = getDiffTree().getResource(node);
		AddToIndexOperation operation = new AddToIndexOperation(
				new IResource[] { resource });
		operation.execute(monitor);
	}

	@Override
	public void reject(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	/**
	 * @return git synchronization data
	 */
	public GitSynchronizeDataSet getSyncData() {
		return gsds;
	}

	@Override
	protected void makeInSync(IDiff diff, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		Activator activator = Activator.getDefault();
		if (activator == null)
			return;

		IndexDiffCache indexDiffCache = activator.getIndexDiffCache();
		if (indexDiffCache != null)
			indexDiffCache.removeIndexDiffChangedListener(indexChangeListener);

		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		subscriber.dispose();
		super.dispose();
	}

	private void handleRepositoryChange(Repository which) {
		boolean shouldRefresh = false;
		for (GitSynchronizeData gsd : gsds) {
			if (which.equals(gsd.getRepository())) {
				updateRevs(gsd);
				shouldRefresh = true;
			}
		}

		if (!shouldRefresh)
			return;

		subscriber.reset(this.gsds);
		ResourceTraversal[] traversals = getScopeManager().getScope()
				.getTraversals();
		try {
			subscriber.refresh(traversals, new NullProgressMonitor());
		} catch (TeamException e) {
			Activator.logError(
					CoreText.GitSubscriberMergeContext_FailedRefreshSyncView, e);
		}
	}

	private void handleResourceChange(IResourceDelta delta) {
		IResourceDelta[] children = delta.getAffectedChildren();
		for (IResourceDelta resourceDelta : children) {
			IResource resource = resourceDelta.getResource();
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			if (mapping == null)
				continue;

			scanDeltaAndRefresh(mapping, resourceDelta);
		}
	}

	private void scanDeltaAndRefresh(RepositoryMapping mapping,
			IResourceDelta delta) {
		Repository repo = mapping.getRepository();
		GitResourceDeltaVisitor visitor = new GitResourceDeltaVisitor(repo);
		try {
			delta.accept(visitor);
			Collection<IFile> files = visitor.getFileResourcesToUpdate();
			if (files != null && files.isEmpty())
				return;

			for (GitSynchronizeData gsd : gsds) {
				if (repo.equals(gsd.getRepository()))
					refreshResources(files);
			}
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
		}
	}

	private void refreshResources(Collection<IFile> resources) {
		IResource[] files = resources.toArray(new IResource[0]);
		try {
			subscriber.refresh(files, IResource.DEPTH_ONE,
					new NullProgressMonitor());
		} catch (final CoreException e) {
			Activator.logError(
					CoreText.GitSubscriberMergeContext_FailedRefreshSyncView, e);
		}
	}

	private void updateRevs(GitSynchronizeData gsd) {
		try {
			gsd.updateRevs();
		} catch (IOException e) {
			Activator.logError(
					CoreText.GitSubscriberMergeContext_FailedUpdateRevs, e);
			return;
		}
	}

}

/*******************************************************************************
 * Copyright (C) 2010,2011 Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryChangeListener;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;

/**
 *
 */
public class GitSubscriberMergeContext extends SubscriberMergeContext {

	private final GitSynchronizeDataSet gsds;

	private final RepositoryChangeListener repoChangeListener;

	private final IndexDiffChangedListener indexDiffChangeListener;

	/**
	 * @param subscriber
	 * @param manager
	 * @param gsds
	 */
	public GitSubscriberMergeContext(final GitResourceVariantTreeSubscriber subscriber,
			ISynchronizationScopeManager manager, GitSynchronizeDataSet gsds) {
		super(subscriber, manager);
		this.gsds = gsds;


		repoChangeListener = new RepositoryChangeListener() {
			public void repositoryChanged(RepositoryMapping which) {
				handleRepositoryChange(subscriber, which);
			}
		};
		indexDiffChangeListener = new IndexDiffChangedListener() {

			public void indexDiffChanged(Repository repository,
					IndexDiffData indexDiffData) {
				Collection<IFile> resources = indexDiffData
						.getChangedFileResources();
				handleResourceChange(subscriber, repository, resources);
			}
		};
		GitProjectData.addRepositoryChangeListener(repoChangeListener);
		Activator.getDefault().getIndexDiffCache().addIndexDiffChangedListener(indexDiffChangeListener);

		initialize();
	}

	public void markAsMerged(IDiff node, boolean inSyncHint,
			IProgressMonitor monitor) throws CoreException {
		IResource resource = getDiffTree().getResource(node);
		AddToIndexOperation operation = new AddToIndexOperation(
				new IResource[] { resource });
		operation.execute(monitor);
	}

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
		GitProjectData.removeRepositoryChangeListener(repoChangeListener);
		IndexDiffCache indexDiffCache = Activator.getDefault()
				.getIndexDiffCache();
		if (indexDiffCache != null)
			indexDiffCache
					.removeIndexDiffChangedListener(indexDiffChangeListener);

		super.dispose();
	}

	private void handleRepositoryChange(
			GitResourceVariantTreeSubscriber subscriber, RepositoryMapping which) {
		for (GitSynchronizeData gsd : gsds) {
			if (which.getRepository().equals(gsd.getRepository())) {
				try {
					gsd.updateRevs();
				} catch (IOException e) {
					Activator
							.error(CoreText.GitSubscriberMergeContext_FailedUpdateRevs,
									e);

					return;
				}

				subscriber.reset(this.gsds);
			}
		}
	}

	private void handleResourceChange(GitResourceVariantTreeSubscriber subscriber,
			Repository which, Collection<IFile> resources) {
		for (GitSynchronizeData gsd : gsds) {
			if (which.equals(gsd.getRepository())) {
				if (!resources.isEmpty())
					refreshResources(subscriber, resources);
				else
					refreshRepository(subscriber);
			}
		}
	}

	private void refreshResources(GitResourceVariantTreeSubscriber subscriber,
			Collection<IFile> resources) {
		final IResource[] iResources = resources
				.toArray(new IResource[resources.size()]);
		try {
			subscriber.refresh(iResources, IResource.DEPTH_ONE,
					new NullProgressMonitor());
		} catch (final CoreException e) {
			Activator
					.error(CoreText.GitSubscriberMergeContext_FailedRefreshSyncView,
							e);
		}
	}

	private void refreshRepository(GitResourceVariantTreeSubscriber subscriber) {
		ResourceTraversal[] traversals = getScope().getTraversals();
		try {
			subscriber.refresh(traversals, new NullProgressMonitor());
		} catch (final CoreException e) {
			Activator
					.error(CoreText.GitSubscriberMergeContext_FailedRefreshSyncView,
							e);
		}
	}

}

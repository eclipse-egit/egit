/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryChangeListener;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;

/**
 *
 */
public class GitSubscriberMergeContext extends SubscriberMergeContext {

	private final GitSynchronizeDataSet gsds;

	private final RepositoryChangeListener repoChangeListener;

	private final IResourceChangeListener resourceChangeListener;

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
				update(subscriber, which);
			}
		};
		resourceChangeListener = new IResourceChangeListener() {

			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getDelta() == null)
					return;

				for (IResourceDelta delta : event.getDelta().getAffectedChildren()) {
					RepositoryMapping repo = RepositoryMapping.getMapping(delta.getResource());
					if (repo != null)
						update(subscriber, repo);
				}
			}
		};
		GitProjectData.addRepositoryChangeListener(repoChangeListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener);

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
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		super.dispose();
	}


	private void update(GitResourceVariantTreeSubscriber subscriber,
			RepositoryMapping which) {
		for (GitSynchronizeData gsd : gsds) {
			if (which.getRepository().equals(gsd.getRepository())) {
				try {
					gsd.updateRevs();
				} catch (IOException e) {
					Activator.error(
							CoreText.GitSubscriberMergeContext_FailedUpdateRevs,
							e);

					return;
				}

				subscriber.reset(this.gsds);

				ResourceTraversal[] traversals = getScopeManager().getScope()
						.getTraversals();
				try {
					subscriber.refresh(traversals, new NullProgressMonitor());
				} catch (CoreException e) {
					Activator
							.error(CoreText.GitSubscriberMergeContext_FailedRefreshSyncView,
									e);
				}

				return;
			}
		}
	}

}

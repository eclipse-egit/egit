/*******************************************************************************
 * Copyright (c) 2011, 2013 Chris Aniszczyk and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Robin Stocker <robin@nibor.org> - ProjectSetCapability
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitProjectSetCapability;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.subscribers.Subscriber;

/**
 * The repository type for Git
 */
public class GitRepositoryProviderType extends RepositoryProviderType {

	private Subscriber subscriber;

	/**
	 * Creates {@link GitRepositoryProviderType}
	 */
	public GitRepositoryProviderType() {
		GitSynchronizeDataSet set = new GitSynchronizeDataSet();
		try {
			Repository[] repositories = Activator.getDefault()
					.getRepositoryCache().getAllRepositories();
			for (Repository repository : repositories) {
				if (!repository.isBare()) {
					GitSynchronizeData data = new GitSynchronizeData(
							repository, Constants.HEAD, Constants.HEAD, true);
					set.add(data);
				}
			}
		} catch (IOException e) {
			// do nothing
		}

		Job initJob = new Job("Initialize Git subscriber") { //$NON-NLS-1$
			{
				setSystem(true);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				GitResourceVariantTreeSubscriber gitSubscriber = new GitResourceVariantTreeSubscriber(
						set);
				gitSubscriber.init(new NullProgressMonitor());

				subscriber = gitSubscriber;
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return GitRepositoryProviderType.class.equals(family);
			}

		};
		initJob.schedule();
	}

	@Override
	public Subscriber getSubscriber() {
		if (subscriber == null) {
			try {
				Job.getJobManager().join(GitRepositoryProviderType.class,
						new NullProgressMonitor());
			} catch (InterruptedException e) {
				throw new IllegalStateException(
						"Subscriber initialization aborted"); //$NON-NLS-1$
			}
		}
		return subscriber;
	}

	@Override
	public ProjectSetCapability getProjectSetCapability() {
		return new GitProjectSetCapability();
	}
}

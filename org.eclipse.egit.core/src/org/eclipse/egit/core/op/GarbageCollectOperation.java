/*******************************************************************************
 * Copyright (C) 2012, 2013 Matthias Sohn <matthias.sohn@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.jgit.api.GarbageCollectCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Operation to garbage collect a git repository
 */
public class GarbageCollectOperation implements IEGitOperation {

	private Repository repository;

	/**
	 * @param repository the repository to garbage collect
	 */
	public GarbageCollectOperation(Repository repository) {
		this.repository = repository;
	}

	/**
	 * Execute garbage collection
	 */
	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			GarbageCollectCommand gc = RepositoryUtil
					.getGarbageCollectCommand(repository);
			gc.setProgressMonitor(
					new EclipseGitProgressTransformer(monitor)).call();
		} catch (IllegalStateException | GitAPIException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(), e.getMessage(), e));
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}

}

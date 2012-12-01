/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import static org.eclipse.egit.core.project.RepositoryMapping.findRepositoryMapping;
import static org.eclipse.jgit.lib.Constants.HEAD;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Remove from Git Index operation (unstage).
 */
public class RemoveFromIndexOperation implements IEGitOperation {

	private final IResource[] resources;

	/**
	 * @param resources
	 *            list of resources that should be removed from index
	 */
	public RemoveFromIndexOperation(IResource[] resources) {
		this.resources = resources;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor = (m != null) ? m : new NullProgressMonitor();

		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitResourcesByRepository(resources);

		monitor.beginTask(
				CoreText.RemoveFromIndexOperation_removingFilesFromIndex,
				pathsByRepository.size());

		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepository.entrySet()) {
			Repository repository = entry.getKey();
			Collection<String> paths = entry.getValue();

			ResetCommand resetCommand = new Git(repository).reset();
			resetCommand.setRef(HEAD);
			for (String path : paths)
				if (path == "") // Working directory //$NON-NLS-1$
					resetCommand.addPath("."); //$NON-NLS-1$
				else
					resetCommand.addPath(path);

			try {
				resetCommand.call();
				monitor.worked(1);
			} catch (GitAPIException e) {
				Activator.logError(e.getMessage(), e);
			} finally {
				findRepositoryMapping(repository).fireRepositoryChanged();
			}
		}

		monitor.done();
	}

	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRuleForRepositories(resources);
	}

}

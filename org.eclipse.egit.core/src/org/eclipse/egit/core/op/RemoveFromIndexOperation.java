/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Remove from Git Index operation (unstage).
 */
public class RemoveFromIndexOperation implements IEGitOperation {

	private final Map<Repository, Collection<String>> pathsByRepository;

	/**
	 * @param paths
	 *            list of paths that should be removed from index
	 * @since 2.2
	 */
	public RemoveFromIndexOperation(Collection<IPath> paths) {
		this.pathsByRepository = ResourceUtil.splitPathsByRepository(paths);
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor = (m != null) ? m : new NullProgressMonitor();

		monitor.beginTask(
				CoreText.RemoveFromIndexOperation_removingFilesFromIndex,
				pathsByRepository.size());

		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepository.entrySet()) {
			Repository repository = entry.getKey();
			Collection<String> paths = entry.getValue();

			GitCommand<?> command = prepareCommand(repository, paths);

			try {
				command.call();
				monitor.worked(1);
			} catch (GitAPIException e) {
				Activator.logError(e.getMessage(), e);
			}
		}

		monitor.done();
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRuleForRepositories(pathsByRepository.keySet());
	}

	private static GitCommand<?> prepareCommand(Repository repository,
			Collection<String> paths) {
		try (Git git = new Git(repository)) {
			if (hasHead(repository)) {
				ResetCommand resetCommand = git.reset();
				resetCommand.setRef(HEAD);
				for (String path : paths) {
					resetCommand.addPath(getCommandPath(path));
				}
				return resetCommand;
			} else {
				RmCommand rmCommand = git.rm();
				rmCommand.setCached(true);
				for (String path : paths) {
					rmCommand.addFilepattern(getCommandPath(path));
				}
				return rmCommand;
			}
		}
	}

	private static boolean hasHead(Repository repository) {
		try {
			Ref head = repository.exactRef(HEAD);
			return head != null && head.getObjectId() != null;
		} catch (IOException e) {
			return false;
		}
	}

	private static String getCommandPath(String path) {
		if ("".equals(path)) // Working directory //$NON-NLS-1$
			return "."; //$NON-NLS-1$
		else
			return path;
	}
}

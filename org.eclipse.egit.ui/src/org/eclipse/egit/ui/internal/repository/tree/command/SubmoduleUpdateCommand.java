/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.op.SubmoduleUpdateOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.swt.widgets.Shell;

/**
 * Command to update selected submodules
 */
public class SubmoduleUpdateCommand extends
		SubmoduleCommand<RepositoryTreeNode<?>> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Map<Repository, List<String>> repoPaths = getSubmodules(
				getSelectedNodes(event));
		if (!repoPaths.isEmpty()) {
			updateSubmodules(repoPaths, getActiveShell(event));
		}
		return null;
	}

	/**
	 * Updates submodules after asking the user to clean up uncommitted changes
	 * in them.
	 *
	 * @param repoPaths
	 *            parent repositories mapped to submodule paths, a {@code null}
	 *            value updates all submodules of that repository
	 * @param shell
	 *            parent for the cleanup dialogs
	 */
	public static void updateSubmodules(
			Map<Repository, List<String>> repoPaths, Shell shell) {
		if (repoPaths.isEmpty()) {
			return;
		}
		List<Repository> subRepos = new ArrayList<>();
		// Check for uncommitted changes in submodules.
		try {
			for (Entry<Repository, List<String>> entry : repoPaths
					.entrySet()) {
				if (entry.getValue() == null) {
					try (SubmoduleWalk walk = SubmoduleWalk
							.forIndex(entry.getKey())) {
						while (walk.next()) {
							addCached(subRepos, walk.getRepository());
						}
					}
				} else {
					for (String path : entry.getValue()) {
						addCached(subRepos, SubmoduleWalk
								.getSubmoduleRepository(entry.getKey(), path));
					}
				}
			}
			for (Repository subRepo : subRepos) {
				String repoName = RepositoryUtil.INSTANCE
						.getRepositoryName(subRepo);
				if (!UIRepositoryUtils.handleUncommittedFiles(subRepo, shell,
						MessageFormat.format(
								UIText.SubmoduleUpdateCommand_UncommittedChanges,
								repoName))) {
					return;
				}
			}
		} catch (Exception e) {
			Activator.handleError(UIText.SubmoduleUpdateCommand_UpdateError, e,
					true);
			return;
		}

		Job job = new WorkspaceJob(UIText.SubmoduleUpdateCommand_Title) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				SubMonitor progress = SubMonitor.convert(monitor,
						repoPaths.size());
				try {
					for (Entry<Repository, List<String>> entry : repoPaths
							.entrySet()) {
						if (progress.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						SubmoduleUpdateOperation op = new SubmoduleUpdateOperation(
								entry.getKey());
						if (entry.getValue() != null) {
							for (String path : entry.getValue()) {
								op.addPath(path);
							}
						}
						op.execute(progress.newChild(1));
					}
				} catch (CoreException e) {
					return Activator.createErrorStatus(
							UIText.SubmoduleUpdateCommand_UpdateError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.SUBMODULE_UPDATE.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		// Include the submodules so that this job cannot run concurrently
		// with a discard or stash job that the cleanup dialog above may
		// have scheduled on a submodule not present as workspace project.
		job.setRule(MultiRule.combine(
				ResourcesPlugin.getWorkspace().getRoot(),
				RuleUtil.getRuleForRepositories(subRepos)));
		job.schedule();
	}

	private static void addCached(List<Repository> repositories,
			Repository repository) throws IOException {
		if (repository == null) {
			return;
		}
		try {
			Repository cached = RepositoryCache.INSTANCE
					.lookupRepository(repository.getDirectory());
			if (cached != null) {
				repositories.add(cached);
			}
		} finally {
			repository.close();
		}
	}
}

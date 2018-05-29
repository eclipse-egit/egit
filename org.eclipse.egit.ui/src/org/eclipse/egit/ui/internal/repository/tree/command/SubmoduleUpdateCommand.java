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
import org.eclipse.egit.core.op.SubmoduleUpdateOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
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
		final Map<Repository, List<String>> repoPaths = getSubmodules(getSelectedNodes(event));
		if (!repoPaths.isEmpty()) {
			// Check for uncommitted changes in submodules.
			try {
				boolean submodulesNodeSelected = false;
				List<Repository> subRepos = new ArrayList<>();
				// If Submodules node is selected, check all submodules.
				for (RepositoryTreeNode<?> node : getSelectedNodes(event)) {
					if (node.getType() == RepositoryTreeNodeType.SUBMODULES) {
						submodulesNodeSelected = true;
						SubmoduleWalk walk = SubmoduleWalk
								.forIndex(node.getRepository());
						while (walk.next()) {
							Repository subRepo = walk.getRepository();
							if (subRepo != null) {
								subRepos.add(subRepo);
							}
						}
						break;
					}
				}
				// If Submodule node is not selected, check the selected
				// submodules.
				if (!submodulesNodeSelected) {
					for (Entry<Repository, List<String>> entry : repoPaths
							.entrySet()) {
						if (entry.getValue() != null) {
							for (String path : entry.getValue()) {
								Repository subRepo;
								subRepo = SubmoduleWalk.getSubmoduleRepository(
										entry.getKey(), path);
								if (subRepo != null) {
									subRepos.add(subRepo);
								}
							}
						}
					}
				}
				Shell parent = getActiveShell(event);
				for (Repository subRepo : subRepos) {
					String repoName = Activator.getDefault().getRepositoryUtil()
							.getRepositoryName(subRepo);
					if (!UIRepositoryUtils.handleUncommittedFiles(subRepo,
							parent,
							MessageFormat.format(
									UIText.SubmoduleUpdateCommand_UncommittedChanges,
									repoName))) {
						return null;
					}
				}
			} catch (Exception e) {
				Activator.handleError(UIText.SubmoduleUpdateCommand_UpdateError,
						e, true);
				return null;
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
						Activator.logError(
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
			job.setRule(ResourcesPlugin.getWorkspace().getRoot());
			job.schedule();
		}
		return null;
	}
}

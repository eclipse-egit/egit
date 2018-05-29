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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.SubmoduleSyncOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Repository;

/**
 * Command to sync submodule configuration
 */
public class SubmoduleSyncCommand extends
		SubmoduleCommand<RepositoryTreeNode<?>> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Map<Repository, List<String>> repoPaths = getSubmodules(getSelectedNodes(event));

		if (!repoPaths.isEmpty()) {
			Job job = new WorkspaceJob(UIText.SubmoduleSyncCommand_Title) {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					SubMonitor progress = SubMonitor.convert(monitor,
							repoPaths.size());
					try {
						for (Entry<Repository, List<String>> entry : repoPaths
								.entrySet()) {
							SubmoduleSyncOperation op = new SubmoduleSyncOperation(
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
								UIText.SubmoduleSyncCommand_SyncError, e);
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					if (JobFamilies.SUBMODULE_SYNC.equals(family))
						return true;
					return super.belongsTo(family);
				}
			};
			job.setUser(true);
			job.schedule();
		}
		return null;
	}
}

/******************************************************************************
 *  Copyright (c) 2026 Lars Vogel and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Pushes the configured remote of every submodule of the parent repository
 * (or of every selected submodule).
 */
public class PushSubmodulesCommand
		extends SubmoduleCommand<RepositoryTreeNode<?>> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Map<Repository, List<String>> repoPaths = getSubmodules(
				getSelectedNodes(event));
		if (repoPaths.isEmpty()) {
			return null;
		}
		Job job = new Job(UIText.PushSubmodulesCommand_Title) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					Set<Repository> subRepos = FetchSubmodulesCommand
							.collectSubmoduleRepositories(repoPaths);
					SubMonitor progress = SubMonitor.convert(monitor,
							subRepos.size());
					for (Repository subRepo : subRepos) {
						if (progress.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						RemoteConfig config = SimpleConfigurePushDialog
								.getConfiguredRemote(subRepo);
						if (config != null) {
							new PushOperationUI(subRepo, config.getName(),
									false).start();
						}
						progress.worked(1);
					}
				} catch (IOException e) {
					return Activator.createErrorStatus(
							UIText.PushSubmodulesCommand_Error, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.PUSH.equals(family)) {
					return true;
				}
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}
}

/******************************************************************************
 *  Copyright (c) 2012, Matthias Sohn <matthias.sohn@sap.com>
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.GarbageCollectOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Command to run jgit garbage collector
 */
public class GarbageCollectCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.team.GarbageCollect"; //$NON-NLS-1$

	/**
	 * Execute garbage collection
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// get selected nodes
		final List<RepositoryNode> selectedNodes;
		try {
			selectedNodes = getSelectedNodes(event);
			if (selectedNodes.isEmpty())
				return null;
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}

		Job job = new Job("Collecting Garbage...") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor progress = SubMonitor.convert(monitor,
						selectedNodes.size());

				for (RepositoryNode node : selectedNodes) {
					if (progress.isCanceled()) {
						break;
					}
					Repository repo = node.getRepository();
					String name = MessageFormat.format(
							UIText.GarbageCollectCommand_jobTitle,
							getRepositoryName(repo));
					this.setName(name);
					final GarbageCollectOperation op = new GarbageCollectOperation(
							repo);
					try {
						op.execute(progress.newChild(1));
					} catch (CoreException e) {
						Activator.logError(MessageFormat.format(
								UIText.GarbageCollectCommand_failed, repo), e);
					}
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		IServiceLocator serviceLocator = HandlerUtil.getActiveSite(event);
		if (serviceLocator != null) {
			IWorkbenchSiteProgressService service = serviceLocator
					.getService(IWorkbenchSiteProgressService.class);
			service.schedule(job);
		} else {
			job.schedule();
		}

		return null;
	}

	private String getRepositoryName(Repository repository) {
		File directory;
		if (!repository.isBare())
			directory = repository.getDirectory().getParentFile();
		else
			directory = repository.getDirectory();
		StringBuilder sb = new StringBuilder();
		sb.append(directory.getName());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(directory.getAbsolutePath());
		return sb.toString();
	}

}

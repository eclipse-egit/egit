/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.SubmoduleUpdateOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jgit.lib.Repository;

/**
 * Command to update selected submodules
 */
public class SubmoduleUpdateCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode<?>> {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Map<Repository, List<String>> repoPaths = new HashMap<Repository, List<String>>();
		List<RepositoryTreeNode<?>> nodes = getSelectedNodes(event);
		for (RepositoryTreeNode<?> node : nodes) {
			if (node.getType() == RepositoryTreeNodeType.REPO) {
				Repository parent = node.getParent().getRepository();
				String path = Repository.stripWorkDir(parent.getWorkTree(),
						node.getRepository().getWorkTree());
				List<String> paths = repoPaths.get(parent);
				if (paths == null) {
					paths = new ArrayList<String>();
					repoPaths.put(parent, paths);
				}
				paths.add(path);
			}
		}
		for (RepositoryTreeNode<?> node : nodes)
			if (node.getType() == RepositoryTreeNodeType.SUBMODULES)
				// Clear paths so all submodules are updated
				repoPaths.put(node.getParent().getRepository(), null);

		if (!repoPaths.isEmpty()) {
			Job job = new Job(UIText.SubmoduleUpdateCommand_Title) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("", repoPaths.size()); //$NON-NLS-1$
					try {
						for (Entry<Repository, List<String>> entry : repoPaths
								.entrySet()) {
							SubmoduleUpdateOperation op = new SubmoduleUpdateOperation(
									entry.getKey());
							if (entry.getValue() != null)
								for (String path : entry.getValue())
									op.addPath(path);
							op.execute(new SubProgressMonitor(monitor, 1));
						}
					} catch (CoreException e) {
						Activator.logError(
								UIText.SubmoduleUpdateCommand_UpdateError, e);
					}
					return Status.OK_STATUS;
				}
			};
			job.setUser(true);
			job.setRule(ResourcesPlugin.getWorkspace().getRoot());
			job.schedule();
		}
		return null;
	}

}

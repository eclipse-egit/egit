/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * This class implements renaming of a remote
 */
public class RenameRemoteOperation implements IEGitOperation {
	private final Repository repository;

	private final String oldName;

	private final String newName;

	/**
	 * @param repository
	 * @param oldName
	 *            the remote to rename
	 * @param newName
	 *            the new name
	 */
	public RenameRemoteOperation(Repository repository, String oldName,
			String newName) {
		this.repository = repository;
		this.oldName = oldName;
		this.newName = newName;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				final String taskName = NLS.bind(
						CoreText.RenameRemoteOperation_TaskName, oldName,
						newName);
				actMonitor.beginTask(taskName, 1);
				try {
					new Git(repository).remoteRename().setOldName(oldName)
							.setNewName(newName).call();

					repository.getConfig().save();
				} catch (Exception e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				}
				actMonitor.worked(1);
				actMonitor.done();
			}
		};

		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}
}
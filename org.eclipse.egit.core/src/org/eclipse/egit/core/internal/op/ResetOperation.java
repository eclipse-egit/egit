/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.op;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * A class for changing a ref and possibly index and workdir too.
 */
public class ResetOperation implements IEGitOperation {

	private final Repository repository;

	private final String refName;

	private final ResetType type;

	/**
	 * Construct a {@link ResetOperation}
	 *
	 * @param repository
	 * @param refName
	 * @param type
	 */
	public ResetOperation(Repository repository, String refName, ResetType type) {
		this.repository = repository;
		this.refName = refName;
		this.type = type;
	}

	public ISchedulingRule getSchedulingRule() {
		if (type == ResetType.HARD)
			return ResourcesPlugin.getWorkspace().getRoot();
		else
			return null;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		if (type == ResetType.HARD) {
			IWorkspaceRunnable action = new IWorkspaceRunnable() {
				public void run(IProgressMonitor actMonitor) throws CoreException {
					reset(actMonitor);
				}
			};
			// lock workspace to protect working tree changes
			ResourcesPlugin.getWorkspace().run(action, monitor);
		} else {
			reset(monitor);
		}
	}

	private void reset(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(NLS.bind(CoreText.ResetOperation_performingReset,
				type.toString().toLowerCase(), refName), 2);

		IProject[] validProjects = null;
		if (type == ResetType.HARD)
			validProjects = ProjectUtil.getValidOpenProjects(repository);

		ResetCommand reset = Git.wrap(repository).reset();
		reset.setMode(type);
		reset.setRef(refName);
		try {
			reset.call();
		} catch (GitAPIException e) {
			throw new TeamException(e.getLocalizedMessage(), e.getCause());
		}
		monitor.worked(1);

		// only refresh if working tree changes
		if (type == ResetType.HARD)
			ProjectUtil.refreshValidProjects(validProjects,
					new SubProgressMonitor(monitor, 1));

		monitor.done();
	}
}

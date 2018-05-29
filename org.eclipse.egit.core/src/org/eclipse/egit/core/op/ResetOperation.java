/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.util.Locale;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
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

	@Override
	public ISchedulingRule getSchedulingRule() {
		if (type == ResetType.HARD)
			return RuleUtil.getRule(repository);
		else
			return null;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		if (type == ResetType.HARD) {
			IWorkspaceRunnable action = new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor actMonitor) throws CoreException {
					reset(actMonitor);
				}
			};
			// lock workspace to protect working tree changes
			ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
					IWorkspace.AVOID_UPDATE, m);
		} else {
			reset(m);
		}
	}

	private void reset(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor,
				NLS.bind(CoreText.ResetOperation_performingReset,
						type.toString().toLowerCase(Locale.ROOT), refName),
				type == ResetType.HARD ? 2 : 1);

		IProject[] validProjects = null;
		if (type == ResetType.HARD) {
			validProjects = ProjectUtil.getValidOpenProjects(repository);
			ResourceUtil.saveLocalHistory(repository);
		}

		ResetCommand reset = Git.wrap(repository).reset();
		reset.setMode(type);
		reset.setRef(refName);
		try {
			reset.call();
		} catch (GitAPIException e) {
			throw new TeamException(e.getLocalizedMessage(), e.getCause());
		}
		progress.worked(1);

		// only refresh if working tree changes
		if (type == ResetType.HARD) {
			ProjectUtil.refreshValidProjects(validProjects,
					progress.newChild(1));
		}
	}
}

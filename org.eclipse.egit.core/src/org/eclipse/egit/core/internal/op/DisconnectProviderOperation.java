/*******************************************************************************
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.op;

import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Disconnects the Git team provider from a project.
 * <p>
 * Once disconnected, Git operations will no longer be available on the project.
 * </p>
 */
public class DisconnectProviderOperation implements IEGitOperation {
	private final Collection<IProject> projectList;

	/**
	 * Create a new disconnect operation.
	 *
	 * @param projs
	 *            the collection of {@link IProject}s which should be
	 *            disconnected from the Git team provider, and returned to
	 *            untracked/unmanaged status.
	 */
	public DisconnectProviderOperation(final Collection<IProject> projs) {
		projectList = projs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		monitor.beginTask(CoreText.DisconnectProviderOperation_disconnecting,
				projectList.size() * 200);
		try {
			for (IProject p : projectList) {
				// TODO is this the right location?
				if (GitTraceLocation.CORE.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.CORE.getLocation(),
							"disconnect " + p.getName()); //$NON-NLS-1$
				unmarkTeamPrivate(p);
				RepositoryProvider.unmap(p);
				monitor.worked(100);

				p.refreshLocal(IResource.DEPTH_INFINITE,
						new SubProgressMonitor(monitor, 100));
			}
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return new MultiRule(projectList.toArray(new IProject[projectList.size()]));
	}

	private void unmarkTeamPrivate(final IContainer p) throws CoreException {
		final IResource[] c;
		c = p.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		if (c != null) {
			for (int k = 0; k < c.length; k++) {
				if (c[k] instanceof IContainer) {
					unmarkTeamPrivate((IContainer) c[k]);
				}
				if (c[k].isTeamPrivateMember()) {
					// TODO is this the right location?
					if (GitTraceLocation.CORE.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.CORE.getLocation(),
								"notTeamPrivate " + c[k]); //$NON-NLS-1$
					c[k].setTeamPrivateMember(false);
				}
			}
		}
	}
}

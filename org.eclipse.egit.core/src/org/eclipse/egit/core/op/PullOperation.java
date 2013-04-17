/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler <mathias.kinzler@sap.com> - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Wraps the JGit API {@link PullCommand} into an operation
 */
public class PullOperation implements IEGitOperation {
	private final Repository[] repositories;

	private final Map<Repository, Object> results = new LinkedHashMap<Repository, Object>();

	private final int timeout;

	/**
	 * @param repositories
	 *            the repository
	 * @param timeout
	 *            in seconds
	 */
	public PullOperation(Set<Repository> repositories, int timeout) {
		this.timeout = timeout;
		this.repositories = repositories.toArray(new Repository[repositories
				.size()]);
	}

	public void execute(IProgressMonitor m) throws CoreException {
		if (!results.isEmpty())
			throw new CoreException(new Status(IStatus.ERROR, Activator
					.getPluginId(), CoreText.OperationAlreadyExecuted));
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		monitor.beginTask(NLS.bind(CoreText.PullOperation_TaskName, Integer
				.valueOf(repositories.length)), repositories.length * 2);
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor mymonitor) throws CoreException {
				for (int i = 0; i < repositories.length; i++) {
					Repository repository = repositories[i];
					if (mymonitor.isCanceled())
						throw new CoreException(Status.CANCEL_STATUS);
					IProject[] validProjects = ProjectUtil.getValidOpenProjects(repository);
					PullCommand pull = new Git(repository).pull();
					PullResult pullResult = null;
					try {
						pull.setProgressMonitor(new EclipseGitProgressTransformer(
								new SubProgressMonitor(mymonitor, 1)));
						pull.setTimeout(timeout);
						pullResult = pull.call();
						results.put(repository, pullResult);
					} catch (DetachedHeadException e) {
						results.put(repository, Activator.error(
								CoreText.PullOperation_DetachedHeadMessage, e));
					} catch (InvalidConfigurationException e) {
						IStatus error = Activator
								.error(CoreText.PullOperation_PullNotConfiguredMessage,
										e);
						results.put(repository, error);
					} catch (GitAPIException e) {
						results.put(repository,
								Activator.error(e.getMessage(), e));
					} catch (JGitInternalException e) {
						Throwable cause = e.getCause();
						if (cause == null || !(cause instanceof TransportException))
							cause = e;
						results.put(repository,
								Activator.error(cause.getMessage(), cause));
					} finally {
						mymonitor.worked(1);
						if (refreshNeeded(pullResult)) {
							ProjectUtil.refreshValidProjects(validProjects,
									new SubProgressMonitor(mymonitor, 1));
							mymonitor.worked(1);
						}
					}
				}
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	private boolean refreshNeeded(PullResult pullResult) {
		if (pullResult == null)
			return true;
		MergeResult mergeResult = pullResult.getMergeResult();
		if (mergeResult == null)
			return true;
		if (mergeResult.getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE)
			return false;
		return true;
	}

	/**
	 * @return the results, or an empty Map if this has not been executed
	 */
	public Map<Repository, Object> getResults() {
		return this.results;
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}

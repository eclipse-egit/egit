/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;

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
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.merge.StorageContentMerger;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * This class implements the merge of a ref with the current head
 *
 */
public class MergeOperation implements IEGitOperation {

	private final Repository repository;

	private final String refName;

	private MergeStrategy mergeStrategy;

	private MergeResult mergeResult;

	/**
	 * @param repository
	 * @param refName name of a commit which should be merged
	 */
	public MergeOperation(Repository repository, String refName) {
		this.repository = repository;
		this.refName = refName;
	}

	/**
	* Create a MergeOperation object
	* @param repository
	* @param refName name of a commit which should be merged
	* @param mergeStrategy the strategy to use for merge
	*/
	public MergeOperation(Repository repository, String refName,
		String mergeStrategy) {
		this.repository = repository;
		this.refName = refName;
		if (mergeStrategy != null)
			this.mergeStrategy = MergeStrategy.get(mergeStrategy);
	}

	public void execute(IProgressMonitor m) throws CoreException {
		if (mergeResult != null)
			throw new CoreException(new Status(IStatus.ERROR, Activator
					.getPluginId(), CoreText.OperationAlreadyExecuted));
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor mymonitor) throws CoreException {
				IProject[] validProjects = ProjectUtil.getValidProjects(repository);
				mymonitor.beginTask(NLS.bind(CoreText.MergeOperation_ProgressMerge, refName), 3);
				Git git = new Git(repository);
				mymonitor.worked(1);
				MergeCommand merge;
				try {
					Ref ref = repository.getRef(refName);
					if (ref != null)
						merge = git.merge().include(ref);
					else
						merge = git.merge().include(ObjectId.fromString(refName));
				} catch (IOException e) {
					throw new TeamException(CoreText.MergeOperation_InternalError, e);
				}
				if (mergeStrategy != null) {
					merge.setStrategy(mergeStrategy);
				}
				merge.mergeWith(new StorageContentMerger(repository));
				try {
					mergeResult = merge.call();
					mymonitor.worked(1);
					if (MergeResult.MergeStatus.FAILED.equals(mergeResult.getMergeStatus()))
						throw new TeamException(mergeResult.toString());
					else if (MergeResult.MergeStatus.NOT_SUPPORTED.equals(mergeResult.getMergeStatus()))
						throw new TeamException(new Status(IStatus.INFO, Activator.getPluginId(), mergeResult.toString()));
				} catch (NoHeadException e) {
					throw new TeamException(CoreText.MergeOperation_MergeFailedNoHead, e);
				} catch (ConcurrentRefUpdateException e) {
					throw new TeamException(CoreText.MergeOperation_MergeFailedRefUpdate, e);
				} catch (GitAPIException e) {
					throw new TeamException(e.getLocalizedMessage(), e.getCause());
				} finally {
					ProjectUtil.refreshValidProjects(validProjects, new SubProgressMonitor(
							mymonitor, 1));
					mymonitor.done();
				}
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	/**
	 * @return the merge result, or <code>null</code> if this has not been
	 *         executed or if an exception occurred
	 */
	public MergeResult getResult() {
		return this.mergeResult;
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}


}

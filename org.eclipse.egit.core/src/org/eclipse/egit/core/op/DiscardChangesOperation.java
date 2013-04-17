/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code extracted from org.eclipse.egit.ui.internal.actions.DiscardChangesAction
 * and reworked.
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * The operation discards changes on a set of resources. In case of a folder
 * resource all file resources in the sub tree are processed. Untracked files
 * are ignored.
 */
public class DiscardChangesOperation implements IEGitOperation {

	String revision;

	IResource[] files;

	ISchedulingRule schedulingRule;

	/**
	 * Construct a {@link DiscardChangesOperation} object.
	 *
	 * @param files
	 */
	public DiscardChangesOperation(IResource[] files) {
		this(files, null);
	}

	/**
	 * Construct a {@link DiscardChangesOperation} object.
	 *
	 * @param files
	 * @param revision
	 */
	public DiscardChangesOperation(IResource[] files, String revision) {
		this.files = new IResource[files.length];
		System.arraycopy(files, 0, this.files, 0, files.length);
		this.revision = revision;
		schedulingRule = MultiRule.combine(calcRefreshRule(files),
				RuleUtil.getRuleForRepositories(files));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	private static ISchedulingRule calcRefreshRule(IResource[] resources) {
		List<ISchedulingRule> rules = new ArrayList<ISchedulingRule>();
		IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace()
				.getRuleFactory();
		for (IResource resource : resources) {
			ISchedulingRule rule = ruleFactory.refreshRule(resource);
			if (rule != null)
				rules.add(rule);
		}
		if (rules.size() == 0)
			return null;
		else
			return new MultiRule(rules.toArray(new IResource[rules.size()]));
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				discardChanges(actMonitor);
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	private void discardChanges(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(CoreText.DiscardChangesOperation_discardingChanges, 2);
		boolean errorOccurred = false;
		try {
			discardChanges();
		} catch (GitAPIException e) {
			errorOccurred = true;
			Activator.logError(CoreText.DiscardChangesOperation_discardFailed, e);
		}
		monitor.worked(1);
		try {
			ProjectUtil.refreshResources(files, new SubProgressMonitor(monitor,
					1));
		} catch (CoreException e) {
			errorOccurred = true;
			Activator.logError(CoreText.DiscardChangesOperation_refreshFailed,
					e);
		}
		monitor.worked(1);
		monitor.done();
		if (errorOccurred) {
			IStatus status = Activator.error(
					CoreText.DiscardChangesOperation_discardFailedSeeLog, null);
			throw new CoreException(status);
		}
	}

	private void discardChanges() throws GitAPIException {
		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitResourcesByRepository(files);
		for (Entry<Repository, Collection<String>> entry : pathsByRepository.entrySet()) {
			Repository repository = entry.getKey();
			Collection<String> paths = entry.getValue();
			CheckoutCommand checkoutCommand = new Git(repository).checkout();
			checkoutCommand.setStartPoint(this.revision);
			if (paths.isEmpty() || paths.contains("")) //$NON-NLS-1$
				checkoutCommand.setAllPaths(true);
			else
				for (String path : paths)
					checkoutCommand.addPath(path);
			checkoutCommand.call();
		}
	}

}

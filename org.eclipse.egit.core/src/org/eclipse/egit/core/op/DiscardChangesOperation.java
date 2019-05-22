/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 * Copyright (C) 2012, 2014 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Code extracted from org.eclipse.egit.ui.internal.actions.DiscardChangesAction
 * and reworked.
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * The operation discards changes on a set of resources (checkout with paths).
 * In case of a folder resource all file resources in the sub tree are
 * processed. Untracked files are ignored.
 */
public class DiscardChangesOperation implements IEGitOperation {

	private final Map<Repository, Collection<String>> pathsByRepository;
	private final ISchedulingRule schedulingRule;

	private String revision;

	private Stage stage;

	/**
	 * The index stage to check out for conflicting files.
	 */
	public enum Stage {
		/**
		 * "base" stage
		 */
		BASE(CheckoutCommand.Stage.BASE),
		/**
		 * "ours" stage
		 */
		OURS(CheckoutCommand.Stage.OURS),
		/**
		 * "theirs" stage
		 */
		THEIRS(CheckoutCommand.Stage.THEIRS);

		private CheckoutCommand.Stage checkoutStage;

		private Stage(CheckoutCommand.Stage checkoutStage) {
			this.checkoutStage = checkoutStage;
		}
	}

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
		this(ResourceUtil.splitResourcesByRepository(files));
		this.revision = revision;
	}

	/**
	 * {@link DiscardChangesOperation} for absolute paths.
	 *
	 * @param paths
	 */
	public DiscardChangesOperation(Collection<IPath> paths) {
		this(ResourceUtil.splitPathsByRepository(paths));
	}

	/**
	 * A {@link DiscardChangesOperation} that resets the given repo-relative
	 * paths in the given repository.
	 *
	 * @param repository
	 *            to work on
	 * @param paths
	 *            collection of repository-relative paths to reset
	 * @param revision
	 *            to reset to, if {@code null}, reset to the index
	 */
	public DiscardChangesOperation(Repository repository,
			Collection<String> paths, String revision) {
		this(Collections.singletonMap(repository, paths));
		this.revision = revision;
	}

	private DiscardChangesOperation(
			Map<Repository, Collection<String>> pathsByRepository) {
		this.pathsByRepository = pathsByRepository;
		this.schedulingRule = RuleUtil.getRuleForRepositories(pathsByRepository
				.keySet());
	}

	/**
	 * Retrieves the paths that will be reset.
	 *
	 * @return an unmodifiable map containing the paths per repository.
	 */
	public Map<Repository, Collection<String>> getPathsPerRepository() {
		Map<Repository, Collection<String>> result = new HashMap<>();
		for (Map.Entry<Repository, Collection<String>> entry : pathsByRepository
				.entrySet()) {
			result.put(entry.getKey(),
					Collections.unmodifiableCollection(entry.getValue()));
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Set the index stage to check out for conflicting files. Not compatible
	 * with revision.
	 *
	 * @param stage
	 */
	public void setStage(Stage stage) {
		if (revision != null)
			throw new IllegalStateException(
					"Either stage or revision can be set, but not both"); //$NON-NLS-1$
		this.stage = stage;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	@Override
	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor actMonitor) throws CoreException {
				discardChanges(actMonitor);
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	private void discardChanges(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor,
				CoreText.DiscardChangesOperation_discardingChanges,
				pathsByRepository.size() * 2);
		boolean errorOccurred = false;

		for (Entry<Repository, Collection<String>> entry : pathsByRepository
				.entrySet()) {
			Repository repository = entry.getKey();
			Collection<String> paths = entry.getValue();

			try {
				discardChanges(repository, paths, progress.newChild(1));
			} catch (GitAPIException e) {
				errorOccurred = true;
				Activator.logError(
						CoreText.DiscardChangesOperation_discardFailed, e);
			}

			try {
				ProjectUtil.refreshRepositoryResources(repository, paths,
						progress.newChild(1));
			} catch (CoreException e) {
				errorOccurred = true;
				Activator.logError(
						CoreText.DiscardChangesOperation_refreshFailed, e);
			}
		}

		if (errorOccurred) {
			IStatus status = Activator.error(
					CoreText.DiscardChangesOperation_discardFailedSeeLog, null);
			throw new CoreException(status);
		}
	}

	private void discardChanges(Repository repository, Collection<String> paths,
			IProgressMonitor progress)
			throws GitAPIException {
		ResourceUtil.saveLocalHistory(repository);
		try (Git git = new Git(repository)) {
			CheckoutCommand checkoutCommand = git.checkout().setProgressMonitor(
					new EclipseGitProgressTransformer(progress));

			if (revision != null) {
				checkoutCommand.setStartPoint(revision);
			}
			if (stage != null) {
				checkoutCommand.setStage(stage.checkoutStage);
			}
			if (paths.isEmpty() || paths.contains("")) { //$NON-NLS-1$
				checkoutCommand.setAllPaths(true);
			} else {
				for (String path : paths) {
					checkoutCommand.addPath(path);
				}
			}
			checkoutCommand.call();
		}
	}

}

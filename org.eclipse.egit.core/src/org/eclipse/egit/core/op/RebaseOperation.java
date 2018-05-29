/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler <mathias.kinzler@sap.com> - initial implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *    Stephan Hackstedt <stephan.hackstedt@googlemail.com> - Bug 477695
 *******************************************************************************/
package org.eclipse.egit.core.op;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;

/**
 * This class implements rebase.
 */
public class RebaseOperation implements IEGitOperation {
	private final Repository repository;

	private final Ref ref;

	private final Operation operation;

	private RebaseResult result;

	private final InteractiveHandler handler;

	private boolean preserveMerges = false;

	/**
	 * Construct a {@link RebaseOperation} object for a {@link Ref}.
	 * <p>
	 * Upon {@link #execute(IProgressMonitor)}, the current HEAD will be rebased
	 * onto the provided {@link Ref}
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the branch or tag
	 */
	public RebaseOperation(Repository repository, Ref ref) {
		this(repository, ref, Operation.BEGIN, null);
	}

	/**
	 * Construct a {@link RebaseOperation} object for a {@link Ref}.
	 * <p>
	 * Upon {@link #execute(IProgressMonitor)}, the current HEAD will be rebased
	 * interactively onto the provided {@link Ref}
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the branch or tag
	 * @param handler
	 */
	public RebaseOperation(Repository repository, Ref ref,
			InteractiveHandler handler) {
		this(repository, ref, Operation.BEGIN, handler);
	}

	/**
	 * Used to abort, skip, or continue a stopped rebase operation that has been
	 * started before.
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param operation
	 *            one of {@link Operation#ABORT}, {@link Operation#CONTINUE},
	 *            {@link Operation#SKIP}
	 */
	public RebaseOperation(Repository repository, Operation operation) {
		this(repository, null, operation, null);
	}

	/**
	 * Used to abort, skip, or continue a stopped rebase interactive operation
	 * that has been started before.
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param operation
	 *            one of {@link Operation#ABORT}, {@link Operation#CONTINUE},
	 *            {@link Operation#SKIP}
	 * @param handler
	 */
	public RebaseOperation(Repository repository, Operation operation,
			InteractiveHandler handler) {
		this(repository, null, operation, handler);
	}

	private RebaseOperation(Repository repository, Ref ref,
			Operation operation, InteractiveHandler handler) {
		this.repository = repository;
		this.ref = ref;
		this.operation = operation;
		this.handler = handler;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		if (result != null)
			throw new CoreException(new Status(IStatus.ERROR, Activator
					.getPluginId(), CoreText.OperationAlreadyExecuted));
		final IProject[] validProjects = ProjectUtil.getValidOpenProjects(repository);
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor actMonitor) throws CoreException {
				SubMonitor progress = SubMonitor.convert(actMonitor, 2);
				try (Git git = new Git(repository)) {
					RebaseCommand cmd = git.rebase().setProgressMonitor(
							new EclipseGitProgressTransformer(
									progress.newChild(1)));
					MergeStrategy strategy = Activator.getDefault()
							.getPreferredMergeStrategy();
					if (strategy != null) {
						cmd.setStrategy(strategy);
					}
					if (handler != null) {
						cmd.runInteractively(handler, true);
					}
					if (operation == Operation.BEGIN) {
						cmd.setPreserveMerges(preserveMerges);
						result = cmd.setUpstream(ref.getName()).call();
					} else {
						result = cmd.setOperation(operation).call();
					}
				} catch (JGitInternalException | GitAPIException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				} finally {
					if (refreshNeeded()) {
						ProjectUtil.refreshValidProjects(validProjects,
								progress.newChild(1));
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	private boolean refreshNeeded() {
		return result == null
				|| result.getStatus() != RebaseResult.Status.UP_TO_DATE;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}

	/**
	 * @return the result of calling {@link #execute(IProgressMonitor)}, or
	 *         <code>null</code> if this has not been executed yet
	 */
	public RebaseResult getResult() {
		return result;
	}

	/**
	 * @return the {@link Repository}
	 */
	public final Repository getRepository() {
		return repository;
	}

	/**
	 * @return the {@link Operation} if it has been set, otherwise null
	 */
	public final Operation getOperation() {
		return operation;
	}

	/**
	 * @param preserveMerges
	 *            true to preserve merges during the rebase
	 */
	public void setPreserveMerges(boolean preserveMerges) {
		this.preserveMerges = preserveMerges;
	}
}

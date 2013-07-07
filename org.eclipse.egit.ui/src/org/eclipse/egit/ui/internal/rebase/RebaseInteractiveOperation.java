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
package org.eclipse.egit.ui.internal.rebase;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
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
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.Action;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseCommand.Step;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * This class implements rebase.
 */
public class RebaseInteractiveOperation implements IEGitOperation {
	private final Repository repository;

	private final RevCommit upstream;

	private final Operation operation;

	private final InteractiveHandler handler;

	private InteractiveResult result;

	/**
	 * Construct a {@link RebaseInteractiveOperation} object for a {@link Ref}.
	 * <p>
	 * Upon {@link #execute(IProgressMonitor)}, the current HEAD will be rebased
	 * onto the provided {@link Ref}
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param upstreamCommit
	 *            the branch or tag
	 * @param handler
	 */
	public RebaseInteractiveOperation(Repository repository,
			RevCommit upstreamCommit, InteractiveHandler handler) {
		this.repository = repository;
		this.operation = Operation.BEGIN;
		this.upstream = upstreamCommit;
		this.handler = handler;
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
	 * @param handler
	 */
	public RebaseInteractiveOperation(Repository repository,
			Operation operation, InteractiveHandler handler) {
		this.repository = repository;
		this.operation = operation;
		this.handler = handler;
		this.upstream = null;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		if (result != null)
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(), CoreText.OperationAlreadyExecuted));
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		final IProject[] validProjects = ProjectUtil
				.getValidOpenProjects(repository);
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				RebaseCommand cmd = new Git(repository).rebase()
						.setProgressMonitor(
								new EclipseGitProgressTransformer(actMonitor));
				try {
					if (operation == Operation.BEGIN)
						result = new InteractiveResult(cmd
								.setUpstream(upstream)
								.runInteractively(handler).call());
					else
						result = new InteractiveResult(cmd.setOperation(
								operation).call());

				} catch (NoHeadException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				} catch (RefNotFoundException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				} catch (JGitInternalException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				} catch (GitAPIException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				} finally {
					if (refreshNeeded())
						ProjectUtil.refreshValidProjects(validProjects,
								new SubProgressMonitor(actMonitor, 1));
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	private boolean refreshNeeded() {
		if (result == null)
			return true;
		if (result.rebaseResult.getStatus() == RebaseResult.Status.UP_TO_DATE)
			return false;
		return true;
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * @return the result of calling {@link #execute(IProgressMonitor)}, or
	 *         <code>null</code> if this has not been executed yet
	 */
	public InteractiveResult getResult() {
		return result;
	}

	/**
	 *
	 */
	public static class InteractiveResult {

		enum ResultType {
			REBASE_RESULT, EDIT_RESULT, REWORD_RESULT, PREPARE_RESULT, EXCEPTION;
		}

		final RebaseResult rebaseResult;

		final ResultType type;

		final List<Step> steps;

		final Throwable exception;

		String commitMessage;

		/**
		 * @param rebaseResult
		 */
		public InteractiveResult(RebaseResult rebaseResult) {
			Assert.isNotNull(rebaseResult);
			this.rebaseResult = rebaseResult;
			this.type = rebaseResult.getAction() == Action.EDIT ? ResultType.EDIT_RESULT
					: ResultType.REBASE_RESULT;
			this.steps = null;
			this.exception = null;
		}

		/**
		 * @param commit
		 */
		public InteractiveResult(String commit) {
			Assert.isNotNull(commit);
			this.rebaseResult = null;
			this.type = ResultType.REWORD_RESULT;
			this.commitMessage = commit;
			this.steps = null;
			this.exception = null;
		}

		/**
		 * @param steps
		 */
		public InteractiveResult(List<Step> steps) {
			Assert.isNotNull(steps);
			this.rebaseResult = null;
			this.type = ResultType.PREPARE_RESULT;
			this.steps = steps;
			this.exception = null;
		}

		/**
		 * @param exception
		 */
		public InteractiveResult(Throwable exception) {
			Assert.isNotNull(exception);
			this.rebaseResult = null;
			this.type = ResultType.EXCEPTION;
			this.steps = null;
			this.exception = exception;

		}

		/**
		 * @return the original commit message to be changed
		 */
		public String getCommitMessage() {
			if (type == ResultType.REWORD_RESULT)
				return commitMessage;
			throw new RuntimeException(); // TODO: Exception handling: throw
											// wrong result type exception
		}

		/**
		 * @param message
		 */
		public void changeCommitMessage(String message) {
			if (type == ResultType.REWORD_RESULT) {
				this.commitMessage = message;
			}
			throw new RuntimeException(); // TODO: Exception handling: throw
											// wrong result type exception
		}

		/**
		 * @return the Step list to be changed
		 */
		public List<Step> getSteps() {
			if (type == ResultType.REWORD_RESULT)
				return steps;
			throw new RuntimeException(); // TODO: Exception handling: throw
			// wrong result type exception
		}

		/**
		 * @return the exception
		 */
		public Throwable getException() {
			return exception;
		}

		/**
		 * @param onlyCheckedGitRebaseException
		 *            if true and occurred exceptions type is on of the declared
		 *            checked exceptions this exceptions is thrown, if false any
		 *            throwable the occurred will be thrown
		 * @throws NoHeadException
		 * @throws RefNotFoundException
		 * @throws JGitInternalException
		 * @throws GitAPIException
		 *
		 */
		public void throwExceptions(boolean onlyCheckedGitRebaseException)
				throws NoHeadException,
				RefNotFoundException, JGitInternalException, GitAPIException {
			if (exception == null)
				return;
			if (exception instanceof NoHeadException)
				throw (NoHeadException) exception;
			if (exception instanceof RefNotFoundException)
				throw (RefNotFoundException) exception;
			if (exception instanceof JGitInternalException)
				throw (JGitInternalException) exception;
			if (exception instanceof GitAPIException)
				throw (GitAPIException) exception;
			if (onlyCheckedGitRebaseException)
				return;
			throw new RuntimeException(exception);
		}
	}

	/**
	 * } /**
	 *
	 */
	public static class StepListChangedException extends CoreException {

		private final List<Step> lastStepList;

		private final Repository repo;

		/**
		 * @param lastStepList
		 * @param repository
		 */
		public StepListChangedException(final List<Step> lastStepList,
				Repository repository) {
			super(new Status(IStatus.WARNING, Activator.getPluginId(),
					CoreText.OperationAlreadyExecuted)); // TODO: new
															// message
			this.lastStepList = lastStepList;
			this.repo = repository;
		}

		/**
		 * @return the last known step list provided by JGit
		 */
		public List<Step> getLastStepList() {
			return lastStepList;
		}

		/**
		 * @return the repository
		 */
		public Repository getRepo() {
			return repo;
		}
	}
}

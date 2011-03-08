/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.team.core.TeamException;

/**
 * This class implements the commit of a list of files.
 */
public class CommitOperation implements IEGitOperation {

	private IFile[] filesToCommit;

	private boolean commitWorkingDirChanges = false;

	private String author;

	private String committer;

	private String message;

	private boolean amending = false;

	private boolean commitAll = false;

	// needed for amending
	private Repository[] repos;

	private Collection<IFile> notIndexed;

	private Collection<IFile> notTracked;

	private boolean createChangeId;

	/**
	 *
	 * @param filesToCommit
	 *            a list of files which will be included in the commit
	 * @param notIndexed
	 *            a list of all files with changes not in the index
	 * @param notTracked
	 *            a list of all untracked files
	 * @param author
	 *            the author of the commit
	 * @param committer
	 *            the committer of the commit
	 * @param message
	 *            the commit message
	 */
	public CommitOperation(IFile[] filesToCommit, Collection<IFile> notIndexed,
			Collection<IFile> notTracked, String author, String committer,
			String message) {
		this.filesToCommit = filesToCommit;
		this.notIndexed = notIndexed;
		this.notTracked = notTracked;
		this.author = author;
		this.committer = committer;
		this.message = message;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			public void run(IProgressMonitor actMonitor) throws CoreException {
				final Date commitDate = new Date();
				final TimeZone timeZone = TimeZone.getDefault();
				final PersonIdent authorIdent = RawParseUtils.parsePersonIdent(author);
				final PersonIdent committerIdent = RawParseUtils.parsePersonIdent(committer);
				if (commitAll) {
					for (Repository repo : repos) {
						Git git = new Git(repo);
						try {
							git.commit()
							.setAll(true)
							.setAuthor(
									new PersonIdent(authorIdent,
											commitDate, timeZone))
											.setCommitter(
													new PersonIdent(committerIdent,
															commitDate, timeZone))
															.setMessage(message).call();
						} catch (NoHeadException e) {
							throw new TeamException(e.getLocalizedMessage(), e);
						} catch (NoMessageException e) {
							throw new TeamException(e.getLocalizedMessage(), e);
						} catch (UnmergedPathException e) {
							throw new TeamException(e.getLocalizedMessage(), e);
						} catch (ConcurrentRefUpdateException e) {
							throw new TeamException(
									CoreText.MergeOperation_InternalError, e);
						} catch (JGitInternalException e) {
							throw new TeamException(
									CoreText.MergeOperation_InternalError, e);
						} catch (WrongRepositoryStateException e) {
							throw new TeamException(e.getLocalizedMessage(), e);
						}
					}
				}

				else if (amending || filesToCommit != null
						&& filesToCommit.length > 0) {
					actMonitor.beginTask(
							CoreText.CommitOperation_PerformingCommit,
							filesToCommit.length * 2);
					actMonitor.setTaskName(CoreText.CommitOperation_PerformingCommit);

					List repositories = new ArrayList <Repository>();
					try {
						prepareTrees(filesToCommit, repositories, actMonitor);
					} catch (Exception e) {
						throw new TeamException(
								CoreText.CommitOperation_errorPreparingTrees, e);
					}

					try {
						System.out.println(notIndexed);
						System.out.println(notTracked);
						doCommits(message, repositories);
						actMonitor.worked(filesToCommit.length);
					} catch (Exception e) {
						throw new TeamException(
								CoreText.CommitOperation_errorCommittingChanges,
								e);
					}
				} else if (commitWorkingDirChanges) {
					// TODO commit -a
				} else {
					// TODO commit
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	private boolean prepareTrees(IFile[] selectedItems, List repositories, IProgressMonitor monitor)
	throws NoFilepatternException {

		for (IFile file : selectedItems) {
			if (monitor.isCanceled())
				return false;
			monitor.worked(1);

			IProject project = file.getProject();
			RepositoryMapping repositoryMapping = RepositoryMapping
			.getMapping(project);
			String repoRelativePath = repositoryMapping
			.getRepoRelativePath(file);
			Repository repository = repositoryMapping.getRepository();
			repositories.add(repository);
			Git git = new Git(repository);
			if(notIndexed.contains(file))
				continue;

			git.add().addFilepattern(repoRelativePath).call();
		}

		return true;
	}

	private void doCommits(String actMessage, List <Repository> repositories) throws IOException,
	NoHeadException, NoMessageException, ConcurrentRefUpdateException, JGitInternalException, WrongRepositoryStateException {

		String commitMessage = actMessage;
		final PersonIdent authorIdent = RawParseUtils.parsePersonIdent(author);
		final PersonIdent committerIdent = RawParseUtils.parsePersonIdent(committer);

		for (Repository repo : repositories) {
			Git git = new Git(repo);
			CommitCommand command = git.commit();
			command.setAmend(amending);
			command.setAuthor(authorIdent);
			command.setCommitter(committerIdent);

			if (createChangeId) {
				// TODO
//				ObjectId parentId;
//				if (parentIds.length > 0)
//					parentId = parentIds[0];
//				else
//					parentId = null;
//				ObjectId changeId = ChangeIdUtil.computeChangeId(tree.getId(), parentId, authorIdent, committerIdent, commitMessage);
//				commitMessage = ChangeIdUtil.insertId(commitMessage, changeId);
//				if (changeId != null)
//					commitMessage = commitMessage.replaceAll("\nChange-Id: I0000000000000000000000000000000000000000\n", "\nChange-Id: I" + changeId.getName() + "\n");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			}

			command.setMessage(commitMessage);
			command.call();
		}
	}

	/**
	 *
	 * @param amending
	 */
	public void setAmending(boolean amending) {
		this.amending = amending;
	}

	/**
	 *
	 * @param commitAll
	 */
	public void setCommitAll(boolean commitAll) {
		this.commitAll = commitAll;
	}

	/**
	 *
	 * @param repos
	 */
	public void setRepos(Repository[] repos) {
		this.repos = repos;
	}

	/**
	 * @param createChangeId
	 *            <code>true</code> if a Change-Id should be inserted
	 */
	public void setComputeChangeId(boolean createChangeId) {
		this.createChangeId = createChangeId;
	}

}

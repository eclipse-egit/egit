/*******************************************************************************
 * Copyright (c) 2010-2012, SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Jens Baumgart (SAP AG)
 *    Robin Stocker (independent)
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * This class implements the commit of a list of files.
 */
public class CommitOperation implements IEGitOperation {

	Collection<String> commitFileList;

	private boolean commitWorkingDirChanges = false;

	private String author;

	private String committer;

	private String message;

	private boolean amending = false;

	private boolean commitAll = false;

	private Repository repo;

	Collection<String> notTracked;

	private boolean createChangeId;

	private boolean commitIndex;

	RevCommit commit = null;

	/**
	 * @param filesToCommit
	 *            a list of files which will be included in the commit
	 * @param notTracked
	 *            a list of all untracked files
	 * @param author
	 *            the author of the commit
	 * @param committer
	 *            the committer of the commit
	 * @param message
	 *            the commit message
	 * @throws CoreException
	 */
	public CommitOperation(IFile[] filesToCommit, Collection<IFile> notTracked,
			String author, String committer, String message) throws CoreException {
		this.author = author;
		this.committer = committer;
		this.message = message;
		if (filesToCommit != null && filesToCommit.length > 0)
			setRepository(filesToCommit[0]);
		if (filesToCommit != null)
			commitFileList = buildFileList(Arrays.asList(filesToCommit));
		if (notTracked != null)
			this.notTracked = buildFileList(notTracked);
	}

	/**
	 * @param repository
	 * @param filesToCommit
	 *            a list of files which will be included in the commit
	 * @param notTracked
	 *            a list of all untracked files
	 * @param author
	 *            the author of the commit
	 * @param committer
	 *            the committer of the commit
	 * @param message
	 *            the commit message
	 * @throws CoreException
	 */
	public CommitOperation(Repository repository, Collection<String> filesToCommit, Collection<String> notTracked,
			String author, String committer, String message) throws CoreException {
		this.repo = repository;
		this.author = author;
		this.committer = committer;
		this.message = message;
		if (filesToCommit != null)
			commitFileList = new HashSet<String>(filesToCommit);
		if (notTracked != null)
			this.notTracked = new HashSet<String>(notTracked);
	}

	/**
	 * Constructs a CommitOperation that commits the index
	 * @param repository
	 * @param author
	 * @param committer
	 * @param message
	 * @throws CoreException
	 */
	public CommitOperation(Repository repository, String author, String committer,
			String message) throws CoreException {
		this.repo = repository;
		this.author = author;
		this.committer = committer;
		this.message = message;
		this.commitIndex = true;
	}


	private void setRepository(IFile file) throws CoreException {
		RepositoryMapping mapping = RepositoryMapping.getMapping(file);
		if (mapping == null)
			throw new CoreException(Activator.error(NLS.bind(
					CoreText.CommitOperation_couldNotFindRepositoryMapping,
					file), null));
		repo = mapping.getRepository();
	}

	/**
	 * @param repository
	 */
	public void setRepository(Repository repository) {
		repo = repository;
	}

	private Collection<String> buildFileList(Collection<IFile> files) throws CoreException {
		Collection<String> result = new HashSet<String>();
		for (IFile file : files) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(file);
			if (mapping == null)
				throw new CoreException(Activator.error(NLS.bind(CoreText.CommitOperation_couldNotFindRepositoryMapping, file), null));
			String repoRelativePath = mapping.getRepoRelativePath(file);
			result.add(repoRelativePath);
		}
		return result;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor actMonitor) throws CoreException {
				if (commitAll)
					commitAll();
				else if (amending || commitFileList != null
						&& commitFileList.size() > 0 || commitIndex) {
					SubMonitor progress = SubMonitor.convert(actMonitor);
					progress.setTaskName(
							CoreText.CommitOperation_PerformingCommit);
					addUntracked();
					commit();
				} else if (commitWorkingDirChanges) {
					// TODO commit -a
				} else {
					// TODO commit
				}
			}

		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	private void addUntracked() throws CoreException {
		if (notTracked == null || notTracked.size() == 0) {
			return;
		}
		try (Git git = new Git(repo)) {
			AddCommand addCommand = git.add();
			boolean fileAdded = false;
			for (String path : notTracked)
				if (commitFileList.contains(path)) {
					addCommand.addFilepattern(path);
					fileAdded = true;
				}
			if (fileAdded) {
				addCommand.call();
			}
		} catch (GitAPIException e) {
			throw new CoreException(Activator.error(e.getMessage(), e));
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repo);
	}

	private void commit() throws TeamException {
		try (Git git = new Git(repo)) {
			CommitCommand commitCommand = git.commit();
			setAuthorAndCommitter(commitCommand);
			commitCommand.setAmend(amending)
					.setMessage(message)
					.setInsertChangeId(createChangeId);
			if (!commitIndex)
				for(String path:commitFileList)
					commitCommand.setOnly(path);
			commit = commitCommand.call();
		} catch (Exception e) {
			throw new TeamException(
					CoreText.MergeOperation_InternalError, e);
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
	 * @param createChangeId
	 *            <code>true</code> if a Change-Id should be inserted
	 */
	public void setComputeChangeId(boolean createChangeId) {
		this.createChangeId = createChangeId;
	}

	/**
	 * @return the newly created commit if committing was successful, null otherwise.
	 */
	public RevCommit getCommit() {
		return commit;
	}

	// TODO: can the commit message be change by the user in case of a merge commit?
	private void commitAll() throws TeamException {
		try (Git git = new Git(repo)) {
			CommitCommand commitCommand = git.commit();
			setAuthorAndCommitter(commitCommand);
			commit = commitCommand.setAll(true).setMessage(message)
					.setInsertChangeId(createChangeId).call();
		} catch (JGitInternalException e) {
			throw new TeamException(CoreText.MergeOperation_InternalError, e);
		} catch (GitAPIException e) {
			throw new TeamException(e.getLocalizedMessage(), e);
		}
	}

	private void setAuthorAndCommitter(CommitCommand commitCommand) throws TeamException {
		final Date commitDate = new Date();
		final TimeZone timeZone = TimeZone.getDefault();

		final PersonIdent enteredAuthor = RawParseUtils.parsePersonIdent(author);
		final PersonIdent enteredCommitter = RawParseUtils.parsePersonIdent(committer);
		if (enteredAuthor == null)
			throw new TeamException(NLS.bind(
					CoreText.CommitOperation_errorParsingPersonIdent, author));
		if (enteredCommitter == null)
			throw new TeamException(
					NLS.bind(CoreText.CommitOperation_errorParsingPersonIdent,
							committer));

		PersonIdent authorIdent;
		if (repo.getRepositoryState().equals(
				RepositoryState.CHERRY_PICKING_RESOLVED)) {
			try (RevWalk rw = new RevWalk(repo)) {
				ObjectId cherryPickHead = repo.readCherryPickHead();
				authorIdent = rw.parseCommit(cherryPickHead)
						.getAuthorIdent();
			} catch (IOException e) {
				Activator.logError(
						CoreText.CommitOperation_ParseCherryPickCommitFailed,
						e);
				throw new IllegalStateException(e);
			}
		} else {
			authorIdent = new PersonIdent(enteredAuthor, commitDate, timeZone);
		}

		final PersonIdent committerIdent = new PersonIdent(enteredCommitter, commitDate, timeZone);

		if (amending) {
			RepositoryUtil repoUtil = Activator.getDefault().getRepositoryUtil();
			RevCommit headCommit = repoUtil.parseHeadCommit(repo);
			if (headCommit != null) {
				final PersonIdent headAuthor = headCommit.getAuthorIdent();
				authorIdent = new PersonIdent(enteredAuthor,
						headAuthor.getWhen(), headAuthor.getTimeZone());
			}
		}

		commitCommand.setAuthor(authorIdent);
		commitCommand.setCommitter(committerIdent);
	}
}

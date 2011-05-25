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
 *    Jens Baumgart (SAP AG)
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.AddCommand;
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

	Collection<String> notIndexed;

	Collection<String> notTracked;

	private boolean createChangeId;

	private boolean commitIndex;

	/**
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
	 * @throws CoreException
	 */
	public CommitOperation(IFile[] filesToCommit, Collection<IFile> notIndexed,
			Collection<IFile> notTracked, String author, String committer,
			String message) throws CoreException {
		this.author = author;
		this.committer = committer;
		this.message = message;
		if (filesToCommit != null && filesToCommit.length > 0)
			setRepository(filesToCommit[0]);
		if (filesToCommit != null)
			commitFileList = buildFileList(Arrays.asList(filesToCommit));
		if (notIndexed != null)
			this.notIndexed = buildFileList(notIndexed);
		if (notTracked != null)
			this.notTracked = buildFileList(notTracked);
	}

	/**
	 * @param repository
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
	 * @throws CoreException
	 */
	public CommitOperation(Repository repository, Collection<String> filesToCommit, Collection<String> notIndexed,
			Collection<String> notTracked, String author, String committer,
			String message) throws CoreException {
		this.repo = repository;
		this.author = author;
		this.committer = committer;
		this.message = message;
		if (filesToCommit != null)
			commitFileList = new HashSet<String>(filesToCommit);
		if (notIndexed != null)
			this.notIndexed = new HashSet<String>(notIndexed);
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
				if (commitAll)
					commitAll(commitDate, timeZone, authorIdent, committerIdent);
				else if (amending || commitFileList != null
						&& commitFileList.size() > 0 || commitIndex) {
					actMonitor.beginTask(
							CoreText.CommitOperation_PerformingCommit,
							20);
					actMonitor.setTaskName(CoreText.CommitOperation_PerformingCommit);
					addUntracked();
					commit();
					actMonitor.worked(10);
				} else if (commitWorkingDirChanges) {
					// TODO commit -a
				} else {
					// TODO commit
				}
			}

		};
		ResourcesPlugin.getWorkspace().run(action, monitor);
	}

	private void addUntracked() throws CoreException {
		if (notTracked == null || notTracked.size() == 0)
			return;
		AddCommand addCommand = new Git(repo).add();
		boolean fileAdded = false;
		for (String path : notTracked)
			if (commitFileList.contains(path)) {
				addCommand.addFilepattern(path);
				fileAdded = true;
			}
		if (fileAdded) {
			try {
				addCommand.call();
			} catch (NoFilepatternException e) {
				throw new CoreException(Activator.error(e.getMessage(), e));
			}
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	private void commit() throws TeamException {
		final Date commitDate = new Date();
		final TimeZone timeZone = TimeZone.getDefault();
		final PersonIdent authorIdent = RawParseUtils.parsePersonIdent(author);
		final PersonIdent committerIdent = RawParseUtils.parsePersonIdent(committer);

		Git git = new Git(repo);
		try {
			CommitCommand commitCommand = git.commit();
			commitCommand
					.setAuthor(
							new PersonIdent(authorIdent,
									commitDate, timeZone))
					.setCommitter(
							new PersonIdent(committerIdent,
									commitDate, timeZone))
					.setAmend(amending)
					.setMessage(message)
					.setInsertChangeId(createChangeId);
			if (!commitIndex)
				for(String path:commitFileList)
					commitCommand.setOnly(path);
			commitCommand.call();
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

	// TODO: can the commit message be change by the user in case of a merge commit?
	private void commitAll(final Date commitDate, final TimeZone timeZone,
			final PersonIdent authorIdent, final PersonIdent committerIdent)
			throws TeamException {

		Git git = new Git(repo);
		try {
			git.commit()
					.setAll(true)
					.setAuthor(
							new PersonIdent(authorIdent, commitDate, timeZone))
					.setCommitter(
							new PersonIdent(committerIdent, commitDate,
									timeZone)).setMessage(message)
					.setInsertChangeId(createChangeId).call();
		} catch (NoHeadException e) {
			throw new TeamException(e.getLocalizedMessage(), e);
		} catch (NoMessageException e) {
			throw new TeamException(e.getLocalizedMessage(), e);
		} catch (UnmergedPathException e) {
			throw new TeamException(e.getLocalizedMessage(), e);
		} catch (ConcurrentRefUpdateException e) {
			throw new TeamException(CoreText.MergeOperation_InternalError, e);
		} catch (JGitInternalException e) {
			throw new TeamException(CoreText.MergeOperation_InternalError, e);
		} catch (WrongRepositoryStateException e) {
			throw new TeamException(e.getLocalizedMessage(), e);
		}
	}

}

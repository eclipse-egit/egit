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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
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

	private IFile[] filesToCommit;

	private boolean commitWorkingDirChanges = false;

	private String author;

	private String committer;

	private String message;

	private boolean amending = false;

	private boolean commitAll = false;

	private Repository[] repos;

	Collection<IFile> notIndexed;

	Collection<IFile> notTracked;

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
				if (commitAll)
					commitAll(commitDate, timeZone, authorIdent, committerIdent);

				else if (amending || filesToCommit != null
						&& filesToCommit.length > 0) {
					actMonitor.beginTask(
							CoreText.CommitOperation_PerformingCommit,
							filesToCommit.length * 2);
					actMonitor.setTaskName(CoreText.CommitOperation_PerformingCommit);
					Map<Repository, List<String>> filesByRepo = prepareCommit(actMonitor);
					doCommits(filesByRepo);
					actMonitor.worked(filesToCommit.length);
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

	private Map<Repository, List<String>> prepareCommit(IProgressMonitor monitor)
			throws CoreException {
		Map<Repository, List<String>> filesByRepo = new HashMap<Repository, List<String>>();
		ArrayList<IFile> filesToAddToIndex = new ArrayList<IFile>();

		for (IFile file : filesToCommit) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(file);
			if (mapping == null)
				throw new CoreException(Activator.error(NLS.bind(CoreText.CommitOperation_couldNotFindRepositoryMapping, file), null));
			String repoRelativePath = mapping.getRepoRelativePath(file);
			Repository repository = mapping.getRepository();
			monitor.worked(1);
			List<String> commitFileList = getCommitFileListForRepository(filesByRepo, repository);
			commitFileList.add(repoRelativePath);
			if (file.exists() && (notIndexed.contains(file) || notTracked.contains(file)))
				filesToAddToIndex.add(file);
		}
		if (filesToAddToIndex.size()>0)
			new AddToIndexOperation(filesToAddToIndex)
					.execute(new SubProgressMonitor(monitor, 1));
		return filesByRepo;
	}

	private List<String> getCommitFileListForRepository(
			Map<Repository, List<String>> filesByRepo, Repository repository) {
		List<String> result = filesByRepo.get(repository);
		if (result == null) {
			result = new ArrayList<String>();
			filesByRepo.put(repository, result);
		}
		return result;
	}

	private void doCommits(Map<Repository, List<String>> filesByRepo)
			throws TeamException {

		final Date commitDate = new Date();
		final TimeZone timeZone = TimeZone.getDefault();

		final PersonIdent authorIdent = RawParseUtils.parsePersonIdent(author);
		final PersonIdent committerIdent = RawParseUtils.parsePersonIdent(committer);

		if (amending && filesToCommit.length == 0) {
			commit(repos[0], new ArrayList<String>(), commitDate, timeZone, authorIdent, committerIdent);
			return;
		}
		for (java.util.Map.Entry<Repository, List<String>> entry : filesByRepo.entrySet()) {
			List<String> commitFileList = entry.getValue();
			Repository repo = entry.getKey();
			commit(repo, commitFileList, commitDate, timeZone, authorIdent, committerIdent);
		}
	}

	private void commit(Repository repo, List<String> commitFileList, final Date commitDate,
			final TimeZone timeZone, final PersonIdent authorIdent,
			final PersonIdent committerIdent) throws TeamException {
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

	private void commitAll(final Date commitDate, final TimeZone timeZone,
			final PersonIdent authorIdent, final PersonIdent committerIdent)
			throws TeamException {
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
						.setMessage(message)
						.setInsertChangeId(createChangeId)
						.call();
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

}

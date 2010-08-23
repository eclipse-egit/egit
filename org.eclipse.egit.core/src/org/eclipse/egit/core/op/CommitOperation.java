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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.api.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.JGitInternalException;
import org.eclipse.jgit.api.NoHeadException;
import org.eclipse.jgit.api.NoMessageException;
import org.eclipse.jgit.api.WrongRepositoryStateException;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.ChangeIdUtil;
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

	// needed for amending
	private RevCommit previousCommit;

	// needed for amending
	private Repository[] repos;

	private ArrayList<IFile> notIndexed;

	private ArrayList<IFile> notTracked;

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
	public CommitOperation(IFile[] filesToCommit, ArrayList<IFile> notIndexed,
			ArrayList<IFile> notTracked, String author, String committer,
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

			public void run(IProgressMonitor monitor) throws CoreException {
				final Date commitDate = new Date();
				final TimeZone timeZone = TimeZone.getDefault();
				final PersonIdent authorIdent = new PersonIdent(author);
				final PersonIdent committerIdent = new PersonIdent(committer);
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
					monitor.beginTask(
							CoreText.CommitOperation_PerformingCommit,
							filesToCommit.length * 2);
					monitor.setTaskName(CoreText.CommitOperation_PerformingCommit);
					HashMap<Repository, Tree> treeMap = new HashMap<Repository, Tree>();
					try {
						if (!prepareTrees(filesToCommit, treeMap, monitor)) {
							// reread the indexes, they were changed in memory
							for (Repository repo : treeMap.keySet())
								repo.getIndex().read();
							return;
						}
					} catch (IOException e) {
						throw new TeamException(
								CoreText.CommitOperation_errorPreparingTrees, e);
					}

					try {
						doCommits(message, treeMap);
						monitor.worked(filesToCommit.length);
					} catch (IOException e) {
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

	private boolean prepareTrees(IFile[] selectedItems,
			HashMap<Repository, Tree> treeMap, IProgressMonitor monitor)
			throws IOException, UnsupportedEncodingException {
		if (selectedItems.length == 0) {
			// amending commit - need to put something into the map
			for (Repository repo : repos) {
				treeMap.put(repo, repo.mapTree(Constants.HEAD));
			}
		}

		for (IFile file : selectedItems) {

			if (monitor.isCanceled())
				return false;
			monitor.worked(1);

			IProject project = file.getProject();
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			Repository repository = repositoryMapping.getRepository();
			Tree projTree = treeMap.get(repository);
			if (projTree == null) {
				projTree = repository.mapTree(Constants.HEAD);
				if (projTree == null)
					projTree = new Tree(repository);
				treeMap.put(repository, projTree);
				// TODO is this the right Location?
				if (GitTraceLocation.CORE.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.CORE.getLocation(),
							"Orig tree id: " + projTree.getId()); //$NON-NLS-1$
			}
			GitIndex index = repository.getIndex();
			String repoRelativePath = repositoryMapping
					.getRepoRelativePath(file);
			String string = repoRelativePath;

			TreeEntry treeMember = projTree.findBlobMember(repoRelativePath);
			// we always want to delete it from the current tree, since if it's
			// updated, we'll add it again
			if (treeMember != null)
				treeMember.delete();

			Entry idxEntry = index.getEntry(string);
			if (notIndexed.contains(file)) {
				File thisfile = new File(repositoryMapping.getWorkTree(),
						idxEntry.getName());
				if (!thisfile.isFile()) {
					index.remove(repositoryMapping.getWorkTree(), thisfile);
					// TODO is this the right Location?
					if (GitTraceLocation.CORE.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.CORE.getLocation(),
								"Phantom file, so removing from index"); //$NON-NLS-1$
					continue;
				} else {
					idxEntry.update(thisfile);
				}
			}
			if (notTracked.contains(file)) {
				idxEntry = index.add(repositoryMapping.getWorkTree(), new File(
						repositoryMapping.getWorkTree(), repoRelativePath));

			}

			if (idxEntry != null) {
				projTree.addFile(repoRelativePath);
				TreeEntry newMember = projTree.findBlobMember(repoRelativePath);

				newMember.setId(idxEntry.getObjectId());
				// TODO is this the right Location?
				if (GitTraceLocation.CORE.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.CORE.getLocation(),
							"New member id for " + repoRelativePath //$NON-NLS-1$
									+ ": " + newMember.getId() + " idx id: " //$NON-NLS-1$ //$NON-NLS-2$
									+ idxEntry.getObjectId());
			}
		}
		return true;
	}

	private void doCommits(String actMessage,
			HashMap<Repository, Tree> treeMap) throws IOException,
			TeamException {

		String commitMessage = actMessage;
		final Date commitDate = new Date();
		final TimeZone timeZone = TimeZone.getDefault();

		final PersonIdent authorIdent = new PersonIdent(author);
		final PersonIdent committerIdent = new PersonIdent(committer);

		for (java.util.Map.Entry<Repository, Tree> entry : treeMap.entrySet()) {
			Tree tree = entry.getValue();
			Repository repo = tree.getRepository();
			repo.getIndex().write();
			writeTreeWithSubTrees(tree);

			ObjectId currentHeadId = repo.resolve(Constants.HEAD);
			ObjectId[] parentIds;
			if (amending) {
				RevCommit[] parents = previousCommit.getParents();
				parentIds = new ObjectId[parents.length];
				for (int i = 0; i < parents.length; i++)
					parentIds[i] = parents[i].getId();
			} else {
				if (currentHeadId != null)
					parentIds = new ObjectId[] { currentHeadId };
				else
					parentIds = new ObjectId[0];
			}
			if (createChangeId) {
				ObjectId parentId;
				if (parentIds.length > 0)
					parentId = parentIds[0];
				else
					parentId = null;
				ObjectId changeId = ChangeIdUtil.computeChangeId(tree.getId(), parentId, authorIdent, committerIdent, commitMessage);
				commitMessage = ChangeIdUtil.insertId(commitMessage, changeId);
				if (changeId != null)
					commitMessage = commitMessage.replaceAll("\nChange-Id: I0000000000000000000000000000000000000000\n", "\nChange-Id: I" + changeId.getName() + "\n");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			}
			CommitBuilder commit = new CommitBuilder();
			commit.setTreeId(tree.getTreeId());
			commit.setParentIds(parentIds);
			commit.setMessage(commitMessage);
			commit
					.setAuthor(new PersonIdent(authorIdent, commitDate,
							timeZone));
			commit.setCommitter(new PersonIdent(committerIdent, commitDate,
					timeZone));

			ObjectInserter inserter = repo.newObjectInserter();
			try {
				inserter.insert(commit);
				inserter.flush();
			} finally {
				inserter.release();
			}

			final RefUpdate ru = repo.updateRef(Constants.HEAD);
			ru.setNewObjectId(commit.getCommitId());
			ru.setRefLogMessage(buildReflogMessage(commitMessage), false);
			if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE) {
				throw new TeamException(NLS.bind(
						CoreText.CommitOperation_failedToUpdate, ru.getName(),
						commit.getCommitId()));
			}
		}
	}

	private void writeTreeWithSubTrees(Tree tree) throws TeamException {
		if (tree.getId() == null) {
			// TODO is this the right Location?
			if (GitTraceLocation.CORE.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.CORE.getLocation(),
						"writing tree for: " + tree.getFullName()); //$NON-NLS-1$
			try {
				for (TreeEntry entry : tree.members()) {
					if (entry.isModified()) {
						if (entry instanceof Tree) {
							writeTreeWithSubTrees((Tree) entry);
						} else {
							// this shouldn't happen.... not quite sure what to
							// do here :)
							// TODO is this the right Location?
							if (GitTraceLocation.CORE.isActive())
								GitTraceLocation.getTrace().trace(
										GitTraceLocation.CORE.getLocation(),
										"BAD JUJU: " //$NON-NLS-1$
												+ entry.getFullName());
						}
					}
				}

				ObjectInserter inserter = tree.getRepository().newObjectInserter();
				try {
					tree.setId(inserter.insert(Constants.OBJ_TREE, tree.format()));
					inserter.flush();
				} finally {
					inserter.release();
				}
			} catch (IOException e) {
				throw new TeamException(
						CoreText.CommitOperation_errorWritingTrees, e);
			}
		}
	}

	private String buildReflogMessage(String commitMessage) {
		String firstLine = commitMessage;
		int newlineIndex = commitMessage.indexOf("\n"); //$NON-NLS-1$
		if (newlineIndex > 0) {
			firstLine = commitMessage.substring(0, newlineIndex);
		}
		String commitStr = amending ? "commit (amend):" : "commit: "; //$NON-NLS-1$ //$NON-NLS-2$
		String result = commitStr + firstLine;
		return result;
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
	 * @param previousCommit
	 */
	public void setPreviousCommit(RevCommit previousCommit) {
		this.previousCommit = previousCommit;
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

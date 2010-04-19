/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * An operation for committing files to the repository.
 */
public class CommitOperation implements IWorkspaceRunnable {

	/**
	 * Whether this commit should be amending the previous commit or not.
	 */
	private boolean amending;

	/**
	 * The author of the commit.
	 */
	private String author;

	/**
	 * The committer of the commit.
	 */
	private String committer;

	/**
	 * The message of this commit.
	 */
	private String commitMessage;

	/**
	 * The files to be committed.
	 */
	private IFile[] selectedItems;

	private Repository repository;

	private Tree tree;

	private RepositoryMapping mapping;

	private Commit previousCommit;

	private Collection<IFile> notIndexed;

	/**
	 * A collection of files that are not under version control.
	 */
	private Collection<IFile> notTracked;

	/**
	 *
	 * @param amending
	 * @param author
	 * @param committer
	 * @param commitMessage
	 * @param selectedItems
	 * @param repository
	 * @param mapping
	 * @param previousCommit
	 * @param notIndexed
	 * @param notTracked
	 */
	public CommitOperation(boolean amending, String author, String committer,
			String commitMessage, IFile[] selectedItems, Repository repository,
			RepositoryMapping mapping, Commit previousCommit,
			Collection<IFile> notIndexed, Collection<IFile> notTracked) {
		this.amending = amending;
		this.author = author;
		this.committer = committer;
		this.commitMessage = commitMessage;
		this.selectedItems = selectedItems;
		this.repository = repository;
		this.mapping = mapping;
		this.previousCommit = previousCommit;
		this.notIndexed = notIndexed;
		this.notTracked = notTracked;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		SubMonitor progressMonitor = SubMonitor.convert(monitor);
		progressMonitor.beginTask("Committing...", 5);

		try {
			prepareTrees(progressMonitor.newChild(3, SubMonitor.SUPPRESS_NONE));
		} catch (IOException e) {
			monitor.done();
			throw new TeamException(UIText.CommitAction_errorPreparingTrees, e);
		}

		try {
			doCommits(progressMonitor.newChild(2, SubMonitor.SUPPRESS_NONE));
		} catch (IOException e) {
			monitor.done();
			throw new TeamException(UIText.CommitAction_errorCommittingChanges,
					e);
		}

		mapping.fireRepositoryChanged();
		monitor.done();
	}

	private ObjectId[] getParentIds(ObjectId currentHeadId) {
		if (amending) {
			return previousCommit.getParentIds();
		}

		return currentHeadId != null ? new ObjectId[] { currentHeadId }
				: new ObjectId[0];
	}

	private void doCommits(IProgressMonitor monitor) throws IOException,
			TeamException {
		monitor.beginTask("Committing...", 3);

		monitor.subTask("Writing to tree");

		writeTreeWithSubTrees(tree);

		monitor.worked(1);

		Date commitDate = new Date();
		TimeZone timeZone = TimeZone.getDefault();
		PersonIdent authorIdent = new PersonIdent(author);
		PersonIdent committerIdent = new PersonIdent(committer);

		ObjectId currentHeadId = repository.resolve(Constants.HEAD);
		Commit commit = new Commit(repository, getParentIds(currentHeadId));
		commit.setTree(tree);
		commit.setMessage(commitMessage);
		commit.setAuthor(new PersonIdent(authorIdent, commitDate, timeZone));
		commit.setCommitter(new PersonIdent(committerIdent, commitDate,
				timeZone));

		monitor.subTask("Writing commit");

		ObjectWriter writer = new ObjectWriter(repository);
		commit.setCommitId(writer.writeCommit(commit));

		monitor.worked(1);

		monitor.subTask("Updating ref");

		RefUpdate ru = repository.updateRef(Constants.HEAD);
		ru.setNewObjectId(commit.getCommitId());
		ru.setRefLogMessage(buildReflogMessage(), false);

		monitor.worked(1);
		monitor.done();

		if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE) {
			throw new TeamException(NLS.bind(
					UIText.CommitAction_failedToUpdate, ru.getName(), commit
							.getCommitId()));
		}
	}

	private String buildReflogMessage() {
		String firstLine = commitMessage;
		int newlineIndex = commitMessage.indexOf("\n"); //$NON-NLS-1$
		if (newlineIndex > 0) {
			firstLine = commitMessage.substring(0, newlineIndex);
		}
		String commitStr = amending ? "commit (amend):" : "commit: "; //$NON-NLS-1$ //$NON-NLS-2$
		String message = commitStr + firstLine;
		return message;
	}

	private void writeTreeWithSubTrees(Tree treeTarget) throws TeamException {
		if (treeTarget.getId() == null) {
			// TODO is this the right Location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.UI.getLocation(),
						"writing tree for: " + treeTarget.getFullName()); //$NON-NLS-1$
			try {
				for (TreeEntry entry : treeTarget.members()) {
					if (entry.isModified()) {
						if (entry instanceof Tree) {
							writeTreeWithSubTrees((Tree) entry);
						} else {
							// this shouldn't happen.... not quite sure what to
							// do here :)
							// TODO is this the right Location?
							if (GitTraceLocation.UI.isActive())
								GitTraceLocation.getTrace().trace(
										GitTraceLocation.UI.getLocation(),
										"BAD JUJU: " //$NON-NLS-1$
												+ entry.getFullName());
						}
					}
				}
				ObjectWriter writer = new ObjectWriter(treeTarget
						.getRepository());
				treeTarget.setId(writer.writeTree(treeTarget));
			} catch (IOException e) {
				throw new TeamException(UIText.CommitAction_errorWritingTrees,
						e);
			}
		}
	}

	private void prepareTrees(IProgressMonitor monitor) throws IOException,
			UnsupportedEncodingException {
		tree = repository.mapTree(Constants.HEAD);
		if (tree == null) {
			tree = new Tree(repository);
		}

		// TODO is this the right Location?
		if (GitTraceLocation.UI.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.UI.getLocation(),
					"Orig tree id: " + tree.getId()); //$NON-NLS-1$
		}

		if (selectedItems.length == 0) {
			monitor.done();
			return;
		}

		monitor.beginTask("Writing entries into index...", selectedItems.length);
		GitIndex index = repository.getIndex();
		for (IFile file : selectedItems) {
			String repoRelativePath = mapping.getRepoRelativePath(file);
			String string = repoRelativePath;

			monitor.subTask(repoRelativePath);

			TreeEntry treeMember = tree.findBlobMember(repoRelativePath);
			// we always want to delete it from the current tree, since if it's
			// updated, we'll add it again
			if (treeMember != null) {
				treeMember.delete();
			}

			Entry idxEntry = index.getEntry(string);
			if (notIndexed.contains(file)) {
				File thisfile = new File(mapping.getWorkDir(), idxEntry
						.getName());
				if (!thisfile.isFile()) {
					index.remove(mapping.getWorkDir(), thisfile);
					index.write();
					// TODO is this the right Location?
					if (GitTraceLocation.UI.isActive()) {
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.UI.getLocation(),
								"Phantom file, so removing from index"); //$NON-NLS-1$
					}
					continue;
				} else if (idxEntry.update(thisfile)) {
					index.write();
				}
			}

			if (notTracked.contains(file)) {
				idxEntry = index.add(mapping.getWorkDir(), new File(mapping
						.getWorkDir(), repoRelativePath));
				index.write();
			}

			if (idxEntry != null) {
				tree.addFile(repoRelativePath);
				TreeEntry newMember = tree.findBlobMember(repoRelativePath);

				newMember.setId(idxEntry.getObjectId());
				// TODO is this the right Location?
				if (GitTraceLocation.UI.isActive()) {
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.UI.getLocation(),
							"New member id for " + repoRelativePath //$NON-NLS-1$
									+ ": " + newMember.getId() + " idx id: " //$NON-NLS-1$ //$NON-NLS-2$
									+ idxEntry.getObjectId());
				}
			}

			monitor.worked(1);
		}
		monitor.done();
	}

}

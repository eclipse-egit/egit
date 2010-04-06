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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.util.ChangeIdUtil;

/**
 * Scan for modified resources in the same project as the selected resources.
 */
public class CommitAction extends RepositoryAction {

	private ArrayList<IFile> notIndexed;
	private ArrayList<IFile> indexChanges;
	private ArrayList<IFile> files;

	private Commit previousCommit;

	private boolean amendAllowed;
	private boolean amending;

	@Override
	public void run(IAction act) {
		resetState();
		try {
			buildIndexHeadDiffList();
		} catch (IOException e) {
			Utils.handleError(getTargetPart().getSite().getShell(), e, "Error during commit", "Error occurred computing diffs");
			return;
		}

		Repository[] repos = getRepositoriesFor(getProjectsForSelectedResources());
		Repository repository = null;
		amendAllowed = repos.length == 1;
		for (Repository repo : repos) {
			repository = repo;
			if (!repo.getRepositoryState().canCommit()) {
				MessageDialog.openError(getTargetPart().getSite().getShell(),
					"Cannot commit now", "Repository state:"
							+ repo.getRepositoryState().getDescription());
				return;
			}
		}

		loadPreviousCommit();
		if (files.isEmpty()) {
			if (amendAllowed && previousCommit != null) {
				boolean result = MessageDialog
				.openQuestion(getTargetPart().getSite().getShell(),
						"No files to commit",
				"No changed items were selected. Do you wish to amend the last commit?");
				if (!result)
					return;
				amending = true;
			} else {
				MessageDialog.openWarning(getTargetPart().getSite().getShell(), "No files to commit", "Commit/amend not possible. Possible causes:\n\n- No changed items were selected\n- Multiple repositories selected\n- No repositories selected\n- No previous commits");
				return;
			}
		}

		String author = null;
		String committer = null;
		if (repository != null) {
			final RepositoryConfig config = repository.getConfig();
			author = config.getAuthorName();
			final String authorEmail = config.getAuthorEmail();
			author = author + " <" + authorEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$

			committer = config.getCommitterName();
			final String committerEmail = config.getCommitterEmail();
			committer = committer + " <" + committerEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		CommitDialog commitDialog = new CommitDialog(getTargetPart().getSite().getShell());
		commitDialog.setAmending(amending);
		commitDialog.setAmendAllowed(amendAllowed);
		commitDialog.setFileList(files);
		commitDialog.setAuthor(author);
		commitDialog.setCommitter(committer);

		if (previousCommit != null) {
			commitDialog.setPreviousCommitMessage(previousCommit.getMessage());
			PersonIdent previousAuthor = previousCommit.getAuthor();
			commitDialog.setPreviousAuthor(previousAuthor.getName() + " <" + previousAuthor.getEmailAddress() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (commitDialog.open() != IDialogConstants.OK_ID)
			return;

		String commitMessage = commitDialog.getCommitMessage();
		amending = commitDialog.isAmending();
		try {
			performCommit(commitDialog, commitMessage);
		} catch (TeamException e) {
			Utils.handleError(getTargetPart().getSite().getShell(), e, "Error during commit", "Error occurred while committing");
		}
	}

	private void resetState() {
		files = new ArrayList<IFile>();
		notIndexed = new ArrayList<IFile>();
		indexChanges = new ArrayList<IFile>();
		amending = false;
		previousCommit = null;
	}

	private void loadPreviousCommit() {
		IProject project = getProjectsForSelectedResources()[0];

		Repository repo = RepositoryMapping.getMapping(project).getRepository();
		try {
			ObjectId parentId = repo.resolve(Constants.HEAD);
			if (parentId != null)
				previousCommit = repo.mapCommit(parentId);
		} catch (IOException e) {
			Utils.handleError(getTargetPart().getSite().getShell(), e, "Error during commit", "Error occurred retrieving last commit");
		}
	}

	private void performCommit(CommitDialog commitDialog, String commitMessage)
			throws TeamException {

		IFile[] selectedItems = commitDialog.getSelectedFiles();

		HashMap<Repository, Tree> treeMap = new HashMap<Repository, Tree>();
		try {
			prepareTrees(selectedItems, treeMap);
		} catch (IOException e) {
			throw new TeamException("Preparing trees", e);
		}

		try {
			doCommits(commitDialog, commitMessage, treeMap);
		} catch (IOException e) {
			throw new TeamException("Committing changes", e);
		}
		for (IProject proj : getProjectsForSelectedResources()) {
			RepositoryMapping.getMapping(proj).fireRepositoryChanged();
		}
	}

	private void doCommits(CommitDialog commitDialog, String commitMessage,
			HashMap<Repository, Tree> treeMap) throws IOException, TeamException {

		final String author = commitDialog.getAuthor();
		final String committer = commitDialog.getCommitter();
		final Date commitDate = new Date();
		final TimeZone timeZone = TimeZone.getDefault();

		final PersonIdent authorIdent = new PersonIdent(author);
		final PersonIdent committerIdent = new PersonIdent(committer);

		for (java.util.Map.Entry<Repository, Tree> entry : treeMap.entrySet()) {
			Tree tree = entry.getValue();
			Repository repo = tree.getRepository();
			writeTreeWithSubTrees(tree);

			ObjectId currentHeadId = repo.resolve(Constants.HEAD);
			ObjectId[] parentIds;
			if (amending) {
				parentIds = previousCommit.getParentIds();
			} else {
				if (currentHeadId != null)
					parentIds = new ObjectId[] { currentHeadId };
				else
					parentIds = new ObjectId[0];
			}
			if (commitDialog.getCreateChangeId()) {
				ObjectId parentId;
				if (parentIds.length > 0)
					parentId = parentIds[0];
				else
					parentId = null;
				ObjectId changeId = ChangeIdUtil.computeChangeId(parentId, tree.getId(), authorIdent, committerIdent, commitMessage);
				commitMessage = ChangeIdUtil.insertId(commitMessage, changeId);
			}

			Commit commit = new Commit(repo, parentIds);
			commit.setTree(tree);
			commit.setMessage(commitMessage);
			commit.setAuthor(new PersonIdent(authorIdent, commitDate, timeZone));
			commit.setCommitter(new PersonIdent(committerIdent, commitDate, timeZone));

			ObjectWriter writer = new ObjectWriter(repo);
			commit.setCommitId(writer.writeCommit(commit));

			final RefUpdate ru = repo.updateRef(Constants.HEAD);
			ru.setNewObjectId(commit.getCommitId());
			ru.setRefLogMessage(buildReflogMessage(commitMessage), false);
			if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE) {
				throw new TeamException("Failed to update " + ru.getName()
						+ " to commit " + commit.getCommitId() + ".");
			}
		}
	}

	private void prepareTrees(IFile[] selectedItems,
			HashMap<Repository, Tree> treeMap) throws IOException,
			UnsupportedEncodingException {
		if (selectedItems.length == 0) {
			// amending commit - need to put something into the map
			for (IProject proj : getProjectsForSelectedResources()) {
				Repository repo = RepositoryMapping.getMapping(proj).getRepository();
				if (!treeMap.containsKey(repo))
					treeMap.put(repo, repo.mapTree(Constants.HEAD));
			}
		}

		for (IFile file : selectedItems) {

			IProject project = file.getProject();
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			Repository repository = repositoryMapping.getRepository();
			Tree projTree = treeMap.get(repository);
			if (projTree == null) {
				projTree = repository.mapTree(Constants.HEAD);
				if (projTree == null)
					projTree = new Tree(repository);
				treeMap.put(repository, projTree);
				// TODO is this the right Location?
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.UI.getLocation(),
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
				File thisfile = new File(repositoryMapping.getWorkDir(), idxEntry.getName());
				if (!thisfile.isFile()) {
					index.remove(repositoryMapping.getWorkDir(), thisfile);
					index.write();
					// TODO is this the right Location?
					if (GitTraceLocation.UI.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.UI.getLocation(),
								"Phantom file, so removing from index"); //$NON-NLS-1$
					continue;
				} else {
					if (idxEntry.update(thisfile))
						index.write();
				}
			}


			if (idxEntry != null) {
				projTree.addFile(repoRelativePath);
				TreeEntry newMember = projTree.findBlobMember(repoRelativePath);

				newMember.setId(idxEntry.getObjectId());
				// TODO is this the right Location?
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.UI.getLocation(),
							"New member id for " + repoRelativePath //$NON-NLS-1$
									+ ": " + newMember.getId() + " idx id: " //$NON-NLS-1$ //$NON-NLS-2$
									+ idxEntry.getObjectId());
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
		String message = commitStr + firstLine;
		return message;
	}

	private void writeTreeWithSubTrees(Tree tree) throws TeamException {
		if (tree.getId() == null) {
			// TODO is this the right Location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.UI.getLocation(),
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
							if (GitTraceLocation.UI.isActive())
								GitTraceLocation.getTrace().trace(
										GitTraceLocation.UI.getLocation(),
										"BAD JUJU: " //$NON-NLS-1$
												+ entry.getFullName());
						}
					}
				}
				ObjectWriter writer = new ObjectWriter(tree.getRepository());
				tree.setId(writer.writeTree(tree));
			} catch (IOException e) {
				throw new TeamException("Writing trees", e);
			}
		}
	}

	private void buildIndexHeadDiffList() throws IOException {
		for (IProject project : getProjectsInRepositoryOfSelectedResources()) {
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			assert repositoryMapping != null;
			Repository repository = repositoryMapping.getRepository();
			Tree head = repository.mapTree(Constants.HEAD);
			GitIndex index = repository.getIndex();
			IndexDiff indexDiff = new IndexDiff(head, index);
			indexDiff.diff();

			includeList(project, indexDiff.getAdded(), indexChanges);
			includeList(project, indexDiff.getChanged(), indexChanges);
			includeList(project, indexDiff.getRemoved(), indexChanges);
			includeList(project, indexDiff.getMissing(), notIndexed);
			includeList(project, indexDiff.getModified(), notIndexed);
		}
	}

	private void includeList(IProject project, HashSet<String> added, ArrayList<IFile> category) {
		String repoRelativePath = RepositoryMapping.getMapping(project).getRepoRelativePath(project);
		if (repoRelativePath.length() > 0) {
			repoRelativePath += "/"; //$NON-NLS-1$
		}

		for (String filename : added) {
			try {
				if (!filename.startsWith(repoRelativePath))
					continue;
				String projectRelativePath = filename.substring(repoRelativePath.length());
				IResource member = project.getFile(projectRelativePath);
				if (member != null && member instanceof IFile) {
					if (!files.contains(member))
						files.add((IFile) member);
					category.add((IFile) member);
				} else {
					// TODO is this the right Location?
					if (GitTraceLocation.UI.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.UI.getLocation(),
								"Couldn't find " + filename); //$NON-NLS-1$
				}
			} catch (Exception e) {
				if (GitTraceLocation.CORE.isActive())
					GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
				continue;
			} // if it's outside the workspace, bad things happen
		}
	}

	boolean tryAddResource(IFile resource, GitProjectData projectData, ArrayList<IFile> category) {
		if (files.contains(resource))
			return false;

		try {
			RepositoryMapping repositoryMapping = projectData
					.getRepositoryMapping(resource);

			if (isChanged(repositoryMapping, resource)) {
				files.add(resource);
				category.add(resource);
				return true;
			}
		} catch (Exception e) {
			if (GitTraceLocation.CORE.isActive())
				GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
		}
		return false;
	}

	private boolean isChanged(RepositoryMapping map, IFile resource) {
		try {
			Repository repository = map.getRepository();
			GitIndex index = repository.getIndex();
			String repoRelativePath = map.getRepoRelativePath(resource);
			Entry entry = index.getEntry(repoRelativePath);
			if (entry != null)
				return entry.isModified(map.getWorkDir());
			return false;
		} catch (UnsupportedEncodingException e) {
			if (GitTraceLocation.CORE.isActive())
				GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
		} catch (IOException e) {
			if (GitTraceLocation.CORE.isActive())
				GitTraceLocation.getTrace().trace(GitTraceLocation.CORE.getLocation(), e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean isEnabled() {
		return getProjectsInRepositoryOfSelectedResources().length > 0;
	}

}

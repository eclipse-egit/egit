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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.Utils;

/**
 * Scan for modified resources in the same project as the selected resources.
 */
public class CommitAction extends RepositoryAction {

	private Map<Repository, List<IFile>> notIndexed;

	private Map<Repository, List<IFile>> notTracked;

	private Map<Repository, List<IFile>> fileCandidates;

	private Commit previousCommit;

	@Override
	public void execute(IAction act) {
		resetState();

		Repository[] repos = getRepositoriesFor(getProjectsForSelectedResources());
		if (repos.length == 0) {
			return;
		}

		for (Repository repo : repos) {
			if (!repo.getRepositoryState().canCommit()) {
				MessageDialog
						.openError(getTargetPart().getSite().getShell(),
								UIText.CommitAction_cannotCommit, NLS.bind(
										UIText.CommitAction_repositoryState,
										repo.getRepositoryState()
												.getDescription()));
				return;
			}
		}

		try {
			buildIndexHeadDiffList();
		} catch (IOException e) {
			handle(
					new TeamException(UIText.CommitAction_errorComputingDiffs,
							e), UIText.CommitAction_errorDuringCommit,
					UIText.CommitAction_errorComputingDiffs);
			return;
		} catch (CoreException e) {
			handle(
					new TeamException(UIText.CommitAction_errorComputingDiffs,
							e), UIText.CommitAction_errorDuringCommit,
					UIText.CommitAction_errorComputingDiffs);
			return;
		}

		loadPreviousCommit();
		boolean amending = false;
		boolean amendAllowed = repos.length == 1;
		if (fileCandidates.isEmpty()) {
			if (amendAllowed && previousCommit != null) {
				boolean result = MessageDialog
				.openQuestion(getTargetPart().getSite().getShell(),
						UIText.CommitAction_noFilesToCommit,
				UIText.CommitAction_amendCommit);
				if (!result)
					return;
				amending = true;
			} else {
				MessageDialog.openWarning(getTargetPart().getSite().getShell(), UIText.CommitAction_noFilesToCommit, UIText.CommitAction_amendNotPossible);
				return;
			}
		}

		String author = null;
		String committer = null;
		// just take the first one
		final RepositoryConfig config = fileCandidates.keySet().iterator()
				.next().getConfig();
		author = config.getAuthorName();
		final String authorEmail = config.getAuthorEmail();
		author = author + " <" + authorEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$

		committer = config.getCommitterName();
		final String committerEmail = config.getCommitterEmail();
		committer = committer + " <" + committerEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$

		CommitDialog commitDialog = new CommitDialog(getTargetPart().getSite().getShell());
		commitDialog.setAmending(amending);
		commitDialog.setAmendAllowed(amendAllowed);
		commitDialog.setFileList(getFiles());
		commitDialog.setAuthor(author);
		commitDialog.setCommitter(committer);
		if (notTracked.size() == fileCandidates.size())
			commitDialog.setShowUntracked(true);

		if (previousCommit != null) {
			commitDialog.setPreviousCommitMessage(previousCommit.getMessage());
			PersonIdent previousAuthor = previousCommit.getAuthor();
			commitDialog.setPreviousAuthor(previousAuthor.getName() + " <" + previousAuthor.getEmailAddress() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (commitDialog.open() != IDialogConstants.OK_ID)
			return;

		String commitMessage = commitDialog.getCommitMessage();
		amending = commitDialog.isAmending();
		performCommit(commitDialog, commitMessage, amending);
	}

	private ArrayList<IFile> getFiles() {
		ArrayList<IFile> candidateList = new ArrayList<IFile>();
		for (List<IFile> candidates : fileCandidates.values()) {
			candidateList.addAll(candidates);
		}
		return candidateList;
	}

	private void resetState() {
		fileCandidates = new HashMap<Repository, List<IFile>>();
		notIndexed = new HashMap<Repository, List<IFile>>();
		notTracked = new HashMap<Repository, List<IFile>>();
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
			Utils.handleError(getTargetPart().getSite().getShell(), e, UIText.CommitAction_errorDuringCommit, UIText.CommitAction_errorRetrievingCommit);
		}
	}

	private void performCommit(CommitDialog commitDialog,
			final String commitMessage, final boolean amending) {
		final String author = commitDialog.getAuthor();
		final String committer = commitDialog.getCommitter();
		final IFile[] files = commitDialog.getSelectedFiles();
		ISchedulingRule rule = MultiRule.combine(files);

		// there may be multiple commit operations going on at once
		final Commit prevCommit = previousCommit;
		final Map<Repository, List<IFile>> notIndexedFiles = new HashMap<Repository, List<IFile>>(
				notIndexed);
		final Map<Repository, List<IFile>> notTrackedFiles = new HashMap<Repository, List<IFile>>(
				notTracked);

		Job job = new Job("Committing resources...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor subMonitor = SubMonitor.convert(monitor);
				Map<RepositoryMapping, List<IFile>> targets = new HashMap<RepositoryMapping, List<IFile>>();
				for (IFile file : files) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping(file);
					List<IFile> list = targets.get(mapping);
					if (list == null) {
						list = new ArrayList<IFile>();
						targets.put(mapping, list);
					}
					list.add(file);
				}

				subMonitor.beginTask("Committing to the repositories...",
						targets.size());
				IWorkspace workspace = ResourcesPlugin.getWorkspace();

				for (Map.Entry<RepositoryMapping, List<IFile>> target : targets
						.entrySet()) {
					RepositoryMapping mapping = target.getKey();
					Repository repository = mapping.getRepository();
					List<IFile> filesList = target.getValue();
					IFile[] filesToCommit = filesList
							.toArray(new IFile[filesList.size()]);

					CommitOperation operation = new CommitOperation(amending,
							author, committer, commitMessage, filesToCommit,
							repository, mapping, prevCommit, notIndexedFiles
									.get(repository), notTrackedFiles
									.get(repository));

					try {
						workspace.run(operation, MultiRule
								.combine(filesToCommit),
								IWorkspace.AVOID_UPDATE, subMonitor.newChild(1,
										SubMonitor.SUPPRESS_NONE));
					} catch (CoreException e) {
						monitor.done();
						return e.getStatus();
					}
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setRule(rule);
		job.schedule();
	}

	private void buildIndexHeadDiffList() throws IOException, CoreException {
		HashMap<Repository, HashSet<IProject>> repositories = new HashMap<Repository, HashSet<IProject>>();

		for (IProject project : getProjectsInRepositoryOfSelectedResources()) {
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(project);
			assert repositoryMapping != null;

			Repository repository = repositoryMapping.getRepository();

			HashSet<IProject> projects = repositories.get(repository);

			if (projects == null) {
				projects = new HashSet<IProject>();
				repositories.put(repository, projects);
			}

			projects.add(project);
		}

		for (Map.Entry<Repository, HashSet<IProject>> entry : repositories.entrySet()) {
			Repository repository = entry.getKey();
			HashSet<IProject> projects = entry.getValue();

			Tree head = repository.mapTree(Constants.HEAD);
			GitIndex index = repository.getIndex();
			IndexDiff indexDiff = new IndexDiff(head, index);
			indexDiff.diff();

			List<IFile> candidates = new ArrayList<IFile>();
			fileCandidates.put(repository, candidates);

			List<IFile> notIndexedFiles = new ArrayList<IFile>();
			notIndexed.put(repository, notIndexedFiles);

			List<IFile> notTrackedFiles = new ArrayList<IFile>();
			notTracked.put(repository, notTrackedFiles);

			for (IProject project : projects) {
				includeList(project, indexDiff.getAdded(), Collections
						.<IFile> emptyList(), candidates);
				includeList(project, indexDiff.getChanged(), Collections
						.<IFile> emptyList(), candidates);
				includeList(project, indexDiff.getRemoved(), Collections
						.<IFile> emptyList(), candidates);
				includeList(project, indexDiff.getMissing(), notIndexedFiles,
						candidates);
				includeList(project, indexDiff.getModified(), notIndexedFiles,
						candidates);
				addUntrackedFiles(repository, project, notTrackedFiles,
						candidates);
			}
		}
	}

	private void addUntrackedFiles(final Repository repository,
			final IProject project, final List<IFile> notTrackedFiles,
			final List<IFile> candidates) throws CoreException, IOException {
		final Tree headTree = repository.mapTree(Constants.HEAD);
		if (headTree == null) {
			return;
		}

		final GitIndex index = repository.getIndex();
		project.accept(new IResourceVisitor() {

			public boolean visit(IResource resource) throws CoreException {
				if (resource.getType() == IResource.FILE && !Team.isIgnoredHint(resource)) {

					String repoRelativePath = RepositoryMapping.getMapping(project).getRepoRelativePath(resource);
					try {
						TreeEntry headEntry = headTree
								.findBlobMember(repoRelativePath);
						if (headEntry == null) {
							Entry indexEntry = null;
							indexEntry = index.getEntry(repoRelativePath);

							if (indexEntry == null) {
								notTrackedFiles.add((IFile) resource);
								candidates.add((IFile) resource);
							}
						}
					} catch (IOException e) {
						throw new TeamException(UIText.CommitAction_InternalError, e);
					}
				}
				return true;
			}
		});


	}

	private void includeList(IProject project, HashSet<String> added,
			List<IFile> category, List<IFile> candidates) {
		String repoRelativePath = RepositoryMapping.getMapping(project)
				.getRepoRelativePath(project);
		if (repoRelativePath.length() > 0) {
			repoRelativePath += "/"; //$NON-NLS-1$
		}

		for (String filename : added) {
			try {
				if (!filename.startsWith(repoRelativePath))
					continue;
				String projectRelativePath = filename.substring(repoRelativePath.length());
				IResource member = project.getFile(projectRelativePath);
				if (member instanceof IFile) {
					if (!candidates.contains(member))
						candidates.add((IFile) member);
					category.add((IFile) member);
				} else {
					// TODO is this the right Location?
					if (GitTraceLocation.UI.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.UI.getLocation(),
								"Couldn't find " + filename); //$NON-NLS-1$
				}
			} catch (Exception e) {
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
				continue;
			} // if it's outside the workspace, bad things happen
		}
	}

	@Override
	public boolean isEnabled() {
		return getProjectsInRepositoryOfSelectedResources().length > 0;
	}

}

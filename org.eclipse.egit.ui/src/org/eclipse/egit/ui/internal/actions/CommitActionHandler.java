/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Scan for modified resources in the same project as the selected resources.
 */
public class CommitActionHandler extends RepositoryActionHandler {

	private Map<Repository, IndexDiff> indexDiffs;

	private ArrayList<IFile> notIndexed;

	private ArrayList<IFile> indexChanges;

	private ArrayList<IFile> notTracked;

	private ArrayList<IFile> files;

	private RevCommit previousCommit;

	private boolean amendAllowed;

	private boolean amending;

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		// let's see if there is any dirty editor around and
		// ask the user if they want to save or abort
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}

		BasicConfigurationDialog.show();
		resetState();
		final IProject[] projects = getProjectsInRepositoryOfSelectedResources(event);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						buildIndexHeadDiffList(projects, monitor);
					} catch (IOException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Activator.handleError(UIText.CommitAction_errorComputingDiffs, e.getCause(),
					true);
			return null;
		} catch (InterruptedException e) {
			return null;
		}

		Repository[] repos = getRepositoriesFor(getProjectsForSelectedResources(event));
		Repository repository = null;
		Repository mergeRepository = null;
		amendAllowed = repos.length == 1;
		boolean isMergedResolved = false;
		for (Repository repo : repos) {
			repository = repo;
			RepositoryState state = repo.getRepositoryState();
			if (!state.canCommit()) {
				MessageDialog.openError(getShell(event),
						UIText.CommitAction_cannotCommit, NLS.bind(
								UIText.CommitAction_repositoryState, state
										.getDescription()));
				return null;
			}
			else if (state.equals(RepositoryState.MERGING_RESOLVED)) {
				isMergedResolved = true;
				mergeRepository = repo;
			}
		}

		loadPreviousCommit(event);
		if (files.isEmpty()) {
			if (amendAllowed && previousCommit != null) {
				boolean result = MessageDialog.openQuestion(getShell(event),
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendCommit);
				if (!result)
					return null;
				amending = true;
			} else {
				MessageDialog.openWarning(getShell(event),
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendNotPossible);
				return null;
			}
		}

		String author = null;
		String committer = null;
		if (repository != null) {
			final UserConfig config = repository.getConfig().get(UserConfig.KEY);
			author = config.getAuthorName();
			final String authorEmail = config.getAuthorEmail();
			author = author + " <" + authorEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$

			committer = config.getCommitterName();
			final String committerEmail = config.getCommitterEmail();
			committer = committer + " <" + committerEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		CommitDialog commitDialog = new CommitDialog(getShell(event));
		commitDialog.setAmending(amending);
		commitDialog.setAmendAllowed(amendAllowed);
		commitDialog.setFileList(files, indexDiffs);
		commitDialog.setPreselectedFiles(getSelectedFiles(event));
		commitDialog.setAuthor(author);
		commitDialog.setCommitter(committer);
		commitDialog.setAllowToChangeSelection(!isMergedResolved);

		if (previousCommit != null) {
			commitDialog.setPreviousCommitMessage(previousCommit.getFullMessage());
			PersonIdent previousAuthor = previousCommit.getAuthorIdent();
			commitDialog.setPreviousAuthor(previousAuthor.getName()
					+ " <" + previousAuthor.getEmailAddress() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (isMergedResolved) {
			commitDialog.setCommitMessage(getMergeResolveMessage(mergeRepository, event));
		}

		if (commitDialog.open() != IDialogConstants.OK_ID)
			return null;

		final CommitOperation commitOperation = new CommitOperation(
				commitDialog.getSelectedFiles(), notIndexed, notTracked,
				commitDialog.getAuthor(), commitDialog.getCommitter(),
				commitDialog.getCommitMessage());
		if (commitDialog.isAmending()) {
			commitOperation.setAmending(true);
			commitOperation.setPreviousCommit(previousCommit);
			commitOperation.setRepos(repos);
		}
		commitOperation.setComputeChangeId(commitDialog.getCreateChangeId());
		commitOperation.setCommitAll(isMergedResolved);
		if (isMergedResolved)
			commitOperation.setRepos(repos);
		String jobname = UIText.CommitAction_CommittingChanges;
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					commitOperation.execute(monitor);

					for (IProject proj : getProjectsForSelectedResources(event)) {
						RepositoryMapping.getMapping(proj)
								.fireRepositoryChanged();
					}
				} catch (CoreException e) {
					return Activator.createErrorStatus(
							UIText.CommitAction_CommittingFailed, e);
				} catch (ExecutionException e) {
					return Activator.createErrorStatus(
							UIText.CommitAction_CommittingFailed, e);
				} finally {
					GitLightweightDecorator.refresh();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.COMMIT))
					return true;
				return super.belongsTo(family);
			}

		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	private void resetState() {
		files = new ArrayList<IFile>();
		notIndexed = new ArrayList<IFile>();
		indexChanges = new ArrayList<IFile>();
		notTracked = new ArrayList<IFile>();
		amending = false;
		previousCommit = null;
		indexDiffs = new HashMap<Repository, IndexDiff>();
	}

	/**
	 * Retrieves a collection of files that may be committed based on the user's
	 * selection when they performed the commit action. That is, even if the
	 * user only selected one folder when the action was performed, if the
	 * folder contains any files that could be committed, they will be returned.
	 *
	 * @param event
	 *
	 * @return a collection of files that is eligible to be committed based on
	 *         the user's selection
	 * @throws ExecutionException
	 */
	private Collection<IFile> getSelectedFiles(ExecutionEvent event)
			throws ExecutionException {
		List<IFile> preselectionCandidates = new ArrayList<IFile>();
		// get the resources the user selected
		IResource[] selectedResources = getSelectedResources(event);
		// iterate through all the files that may be committed
		for (IFile file : files) {
			for (IResource resource : selectedResources) {
				// if any selected resource contains the file, add it as a
				// preselection candidate
				if (resource.contains(file)) {
					preselectionCandidates.add(file);
					break;
				}
			}
		}
		return preselectionCandidates;
	}

	private void loadPreviousCommit(ExecutionEvent event)
			throws ExecutionException {
		IProject project = getProjectsForSelectedResources(event)[0];

		Repository repo = RepositoryMapping.getMapping(project).getRepository();
		try {
			ObjectId parentId = repo.resolve(Constants.HEAD);
			if (parentId != null)
				previousCommit = new RevWalk(repo).parseCommit(parentId);
		} catch (IOException e) {
			Activator.handleError(UIText.CommitAction_errorRetrievingCommit, e,
					true);
		}
	}

	private void buildIndexHeadDiffList(IProject[] selectedProjects, IProgressMonitor monitor)
			throws IOException, OperationCanceledException {
		HashMap<Repository, HashSet<IProject>> repositories = new HashMap<Repository, HashSet<IProject>>();

		for (IProject project : selectedProjects) {
			RepositoryMapping repositoryMapping = RepositoryMapping
					.getMapping(project);
			assert repositoryMapping != null;

			Repository repository = repositoryMapping.getRepository();

			HashSet<IProject> projects = repositories.get(repository);

			if (projects == null) {
				projects = new HashSet<IProject>();
				repositories.put(repository, projects);
			}

			projects.add(project);
		}

		monitor.beginTask(UIText.CommitActionHandler_calculatingChanges,
				repositories.size());
		for (Map.Entry<Repository, HashSet<IProject>> entry : repositories
				.entrySet()) {
			Repository repository = entry.getKey();
			monitor.subTask(NLS.bind(UIText.CommitActionHandler_repository,
					repository.getDirectory().getPath()));
			HashSet<IProject> projects = entry.getValue();

			IndexDiff indexDiff = new IndexDiff(repository, Constants.HEAD,
					IteratorService.createInitialIterator(repository));
			indexDiff.diff();
			indexDiffs.put(repository, indexDiff);

			for (IProject project : projects) {
				includeList(project, indexDiff.getAdded(), indexChanges);
				includeList(project, indexDiff.getChanged(), indexChanges);
				includeList(project, indexDiff.getRemoved(), indexChanges);
				includeList(project, indexDiff.getMissing(), notIndexed);
				includeList(project, indexDiff.getModified(), notIndexed);
				includeList(project, indexDiff.getUntracked(), notTracked);
			}
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			monitor.worked(1);
		}
		monitor.done();
	}


	private void includeList(IProject project, Set<String> added,
			ArrayList<IFile> category) {
		String repoRelativePath = RepositoryMapping.getMapping(project)
				.getRepoRelativePath(project);
		if (repoRelativePath.length() > 0) {
			repoRelativePath += "/"; //$NON-NLS-1$
		}

		for (String filename : added) {
			try {
				if (!filename.startsWith(repoRelativePath))
					continue;
				String projectRelativePath = filename
						.substring(repoRelativePath.length());
				IFile member = project.getFile(projectRelativePath);
				if (!files.contains(member))
					files.add(member);
				category.add(member);
			} catch (Exception e) {
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.UI.getLocation(), e.getMessage(),
							e);
				continue;
			} // if it's outside the workspace, bad things happen
		}
	}


	@Override
	public boolean isEnabled() {
		return getProjectsInRepositoryOfSelectedResources().length > 0;
	}

	private String getMergeResolveMessage(Repository mergeRepository,
			ExecutionEvent event) throws ExecutionException {
		File mergeMsg = new File(mergeRepository.getDirectory(), Constants.MERGE_MSG);
		FileReader reader;
		try {
			reader = new FileReader(mergeMsg);
			BufferedReader br = new BufferedReader(reader);
			try {
				StringBuilder message = new StringBuilder();
				String s;
				String newLine = newLine();
				while ((s = br.readLine()) != null) {
					message.append(s).append(newLine);
				}
				return message.toString();
			} catch (IOException e) {
				MessageDialog.openError(getShell(event),
						UIText.CommitAction_MergeHeadErrorTitle,
						UIText.CommitAction_ErrorReadingMergeMsg);
				throw new IllegalStateException(e);
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					// Empty
				}
			}
		} catch (FileNotFoundException e) {
			MessageDialog.openError(getShell(event),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_MergeHeadErrorMessage);
			throw new IllegalStateException(e);
		}
	}

	private String newLine() {
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

}

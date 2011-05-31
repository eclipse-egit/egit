/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentStateManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * UI component for performing a commit
 */
public class CommitUI  {

	private IndexDiff indexDiff;

	private Set<String> notIndexed;

	private Set<String> indexChanges;

	private Set<String> notTracked;

	private Set<String> files;

	private boolean amendAllowed = true;

	private boolean amending;

	private Shell shell;

	private Repository repo;

	private IResource[] selectedResources;

	private boolean preselectAll;

	/**
	 * Constructs a CommitUI object
	 * @param shell
	 *            Shell to use for UI interaction. Must not be null.
	 * @param repo
	 *            Repository to commit. Must not be null
	 * @param selectedResources
	 *            Resources selected by the user. A file is preselected in the
	 *            commit dialog if the file is contained in selectedResources or
	 *            if selectedResources contains a resource that is parent of the
	 *            file. selectedResources must not be null.
	 * @param preselectAll
	 * 			  preselect all changed files in the commit dialog.
	 * 			  If set to true selectedResources are ignored.
	 */
	public CommitUI(Shell shell, Repository repo,
			IResource[] selectedResources, boolean preselectAll) {
		this.shell = shell;
		this.repo = repo;
		this.selectedResources = new IResource[selectedResources.length];
		// keep our own copy
		System.arraycopy(selectedResources, 0, this.selectedResources, 0,
				selectedResources.length);
		this.preselectAll = preselectAll;
	}

	/**
	 * Performs a commit
	 */
	public void commit() {
		// let's see if there is any dirty editor around and
		// ask the user if they want to save or abort
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return;
		}

		BasicConfigurationDialog.show(new Repository[]{repo});

		resetState();
		final IProject[] projects = getProjectsOfRepositories();
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
			return;
		} catch (InterruptedException e) {
			return;
		}

		CommitHelper commitHelper = new CommitHelper(repo);

		if (!commitHelper.canCommit()) {
			MessageDialog.openError(
					shell,
					UIText.CommitAction_cannotCommit,
					commitHelper.getCannotCommitMessage());
			return;
		}
		if (files.isEmpty()) {
			if (amendAllowed && commitHelper.getPreviousCommit() != null) {
				boolean result = MessageDialog.openQuestion(shell,
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendCommit);
				if (!result)
					return;
				amending = true;
			} else {
				MessageDialog.openWarning(shell,
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendNotPossible);
				return;
			}
		}

		CommitDialog commitDialog = new CommitDialog(shell);
		commitDialog.setAmending(amending);
		commitDialog.setAmendAllowed(amendAllowed);
		commitDialog.setFiles(repo, files, indexDiff);
		commitDialog.setPreselectedFiles(getSelectedFiles());
		commitDialog.setPreselectAll(preselectAll);
		commitDialog.setAuthor(commitHelper.getAuthor());
		commitDialog.setCommitter(commitHelper.getCommitter());
		commitDialog.setAllowToChangeSelection(!commitHelper.isMergedResolved && !commitHelper.isCherryPickResolved);
		commitDialog.setCommitMessage(commitHelper.getCommitMessage());

		if (commitDialog.open() != IDialogConstants.OK_ID)
			return;

		final CommitOperation commitOperation;
		try {
			commitOperation= new CommitOperation(
					repo,
					commitDialog.getSelectedFiles(), notIndexed, notTracked,
					commitDialog.getAuthor(), commitDialog.getCommitter(),
					commitDialog.getCommitMessage());
		} catch (CoreException e1) {
			Activator.handleError(UIText.CommitUI_commitFailed, e1, true);
			return;
		}
		if (commitDialog.isAmending())
			commitOperation.setAmending(true);
		commitOperation.setComputeChangeId(commitDialog.getCreateChangeId());
		commitOperation.setCommitAll(commitHelper.isMergedResolved);
		if (commitHelper.isMergedResolved)
			commitOperation.setRepository(repo);
		performCommit(repo, commitOperation);
		return;
	}

	/**
	 * Uses a Job to perform the given CommitOperation
	 * @param repository
	 * @param commitOperation
	 */
	public static void performCommit(final Repository repository, final CommitOperation commitOperation) {
		String jobname = UIText.CommitAction_CommittingChanges;
		Job job = new Job(jobname) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					commitOperation.execute(monitor);
					CommitMessageComponentStateManager.deleteState(
							repository);
					RepositoryMapping mapping = RepositoryMapping
							.findRepositoryMapping(repository);
					if (mapping != null)
						mapping.fireRepositoryChanged();
				} catch (CoreException e) {
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
	}

	private IProject[] getProjectsOfRepositories() {
		Set<IProject> ret = new HashSet<IProject>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null && mapping.getRepository() == repo)
				ret.add(project);
		}
		return ret.toArray(new IProject[ret.size()]);
	}

	private void resetState() {
		files = new LinkedHashSet<String>();
		notIndexed = new LinkedHashSet<String>();
		indexChanges = new LinkedHashSet<String>();
		notTracked = new LinkedHashSet<String>();
		amending = false;
		indexDiff = null;
	}

	/**
	 * Retrieves a collection of files that may be committed based on the user's
	 * selection when they performed the commit action. That is, even if the
	 * user only selected one folder when the action was performed, if the
	 * folder contains any files that could be committed, they will be returned.
	 *
	 * @return a collection of files that is eligible to be committed based on
	 *         the user's selection
	 */
	private Set<String> getSelectedFiles() {
		Set<String> preselectionCandidates = new LinkedHashSet<String>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		// iterate through all the files that may be committed
		for (String fileName : files) {
			URI uri = new File(repo.getWorkTree(), fileName).toURI();
			IFile[] workspaceFiles = root.findFilesForLocationURI(uri);
			if (workspaceFiles.length > 0) {
				IFile file = workspaceFiles[0];
				for (IResource resource : selectedResources) {
					// if any selected resource contains the file, add it as a
					// preselection candidate
					if (resource.contains(file)) {
						preselectionCandidates.add(fileName);
						break;
					}
				}
			} else {
				// could be file outside of workspace
				for (IResource resource : selectedResources) {
					if(resource.getFullPath().toFile().equals(new File(uri))) {
						preselectionCandidates.add(fileName);
					}
				}
			}
		}
		return preselectionCandidates;
	}

	private void buildIndexHeadDiffList(IProject[] selectedProjects,
			IProgressMonitor monitor) throws IOException,
			OperationCanceledException {

		monitor.beginTask(UIText.CommitActionHandler_calculatingChanges, 1000);
		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				monitor);
		CountingVisitor counter = new CountingVisitor();
		for (IProject p : selectedProjects) {
			try {
				p.accept(counter);
			} catch (CoreException e) {
				// ignore
			}
		}
		indexDiff = new IndexDiff(repo, Constants.HEAD,
				IteratorService.createInitialIterator(repo));
		indexDiff.diff(jgitMonitor, counter.count, 0, NLS.bind(
				UIText.CommitActionHandler_repository, repo.getDirectory()
						.getPath()));

		includeList(indexDiff.getAdded(), indexChanges);
		includeList(indexDiff.getChanged(), indexChanges);
		includeList(indexDiff.getRemoved(), indexChanges);
		includeList(indexDiff.getMissing(), notIndexed);
		includeList(indexDiff.getModified(), notIndexed);
		includeList(indexDiff.getUntracked(), notTracked);
		if (monitor.isCanceled())
			throw new OperationCanceledException();
		monitor.done();
	}

	static class CountingVisitor implements IResourceVisitor {
		int count;
		public boolean visit(IResource resource) throws CoreException {
			count++;
			return true;
		}
	}

	private void includeList(Set<String> added, Set<String> category) {
		for (String filename : added) {
			if (!files.contains(filename))
				files.add(filename);
			category.add(filename);
		}
	}

}

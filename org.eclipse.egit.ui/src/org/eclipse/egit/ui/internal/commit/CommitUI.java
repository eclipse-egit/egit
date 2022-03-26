/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.lib.CommitConfig;
import org.eclipse.jgit.lib.CommitConfig.CleanupMode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
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
	 *
	 * @return true if a commit operation was triggered
	 */
	public boolean commit() {
		// let's see if there is any dirty editor around and
		// ask the user if they want to save or abort
		if (!UIUtils.saveAllEditors(repo))
			return false;

		BasicConfigurationDialog.show(new Repository[]{repo});

		resetState();
		final IProject[] projects = getProjectsOfRepositories();
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				@Override
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
			return false;
		} catch (InterruptedException e) {
			return false;
		}

		CommitHelper commitHelper = new CommitHelper(repo);

		if (!commitHelper.canCommit()) {
			MessageDialog.openError(
					shell,
					UIText.CommitAction_cannotCommit,
					commitHelper.getCannotCommitMessage());
			return false;
		}
		boolean amendAllowed = commitHelper.amendAllowed();
		if (files.isEmpty()) {
			if (amendAllowed && commitHelper.getPreviousCommit() != null) {
				boolean result = MessageDialog.openQuestion(shell,
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendCommit);
				if (!result)
					return false;
				amending = true;
			} else {
				MessageDialog.openWarning(shell,
						UIText.CommitAction_noFilesToCommit,
						UIText.CommitAction_amendNotPossible);
				return false;
			}
		}

		CommitDialog commitDialog = new CommitDialog(shell);
		commitDialog.setAmending(amending);
		commitDialog.setAmendAllowed(amendAllowed);
		commitDialog.setFiles(repo, files, indexDiff);
		commitDialog.setPreselectedFiles(
				getSelectedFiles(repo, files, selectedResources));
		commitDialog.setPreselectAll(preselectAll);
		commitDialog.setAuthor(commitHelper.getAuthor());
		commitDialog.setCommitter(commitHelper.getCommitter());
		commitDialog.setAllowToChangeSelection(!commitHelper.isMergedResolved && !commitHelper.isCherryPickResolved);
		String initialMessage;
		char commentChar;
		if (commitHelper.shouldUseCommitTemplate()) {
			initialMessage = commitHelper.getCommitTemplate();
			commentChar = '\0';
		} else {
			initialMessage = commitHelper.getCommitMessage();
			commentChar = commitHelper.getCommentChar();
		}
		CommitConfig config = repo.getConfig().get(CommitConfig.KEY);
		CleanupMode mode = config.resolve(CleanupMode.DEFAULT, true);
		if (commentChar == '\0') {
			commentChar = config
					.getCommentChar(Utils.normalizeLineEndings(initialMessage));
		}
		commitDialog.setCleanupMode(mode, commentChar);
		commitDialog.setCommitMessage(initialMessage);
		if (commitDialog.open() != IDialogConstants.OK_ID)
			return false;

		final CommitOperation commitOperation;
		try {
			commitOperation= new CommitOperation(
					repo,
					commitDialog.getSelectedFiles(), notTracked, commitDialog.getAuthor(),
					commitDialog.getCommitter(), commitDialog.getCommitMessage());
		} catch (CoreException e1) {
			Activator.handleError(UIText.CommitUI_commitFailed, e1, true);
			return false;
		}
		if (commitDialog.isAmending())
			commitOperation.setAmending(true);
		commitOperation.setSign(commitDialog.isSignCommit());

		commitOperation.setComputeChangeId(commitDialog.getCreateChangeId());
		commitOperation.setCommitAll(commitHelper.isMergedResolved);
		if (commitHelper.isMergedResolved)
			commitOperation.setRepository(repo);
		Job commitJob = new CommitJob(repo, commitOperation)
				.setPushUpstream(commitDialog.getPushMode())
				.setForce(commitDialog.isForce())
				.setDialog(commitDialog.alwaysShowPushDialog());
		commitJob.schedule();

		return true;
	}

	private IProject[] getProjectsOfRepositories() {
		Set<IProject> ret = new HashSet<>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null && mapping.getRepository() == repo)
				ret.add(project);
		}
		return ret.toArray(new IProject[0]);
	}

	private void resetState() {
		files = new LinkedHashSet<>();
		notIndexed = new LinkedHashSet<>();
		indexChanges = new LinkedHashSet<>();
		notTracked = new LinkedHashSet<>();
		amending = false;
		indexDiff = null;
	}

	/**
	 * Retrieves a collection of files that may be committed based on the user's
	 * selection when they performed the commit action. That is, even if the
	 * user only selected one folder when the action was performed, if the
	 * folder contains any files that could be committed, they will be returned.
	 *
	 * @param repository
	 *            being operated on
	 * @param mayBeCommitted
	 *            {@link Set} containing all files that may be committed
	 * @param resourcesSelected
	 *            the resources currently selected
	 *
	 * @return a collection of files that is eligible to be committed based on
	 *         the user's selection
	 */
	public static Set<String> getSelectedFiles(Repository repository,
			Set<String> mayBeCommitted,
			IResource[] resourcesSelected) {
		Set<String> preselectionCandidates = new LinkedHashSet<>();
		if (repository == null || mayBeCommitted.isEmpty()) {
			return preselectionCandidates;
		}
		List<String> selectedDirectories = new ArrayList<>();
		Set<String> selectedFiles = new HashSet<>();
		for (IResource rsc : resourcesSelected) {
			IPath p = ResourceUtil.getRepositoryRelativePath(rsc.getLocation(),
					repository);
			if (p != null) {
				if (p.isEmpty()) {
					// Resource is the root of the working tree: include all
					return mayBeCommitted;
				} else {
					String rscPath = p.toString();
					if (rsc.getType() == IResource.FILE) {
						selectedFiles.add(rscPath);
					} else {
						selectedDirectories.add(rscPath + '/');
					}
				}
			}
		}
		// Sorting moves parent directories to the front and thus we waste
		// less time re-checking for sub-directories.
		Collections.sort(selectedDirectories);
		String lastAncestor = null;
		for (String prefix : selectedDirectories) {
			if (lastAncestor != null && prefix.startsWith(lastAncestor)) {
				// Skip sub-directories if we already checked for an ancestor.
				continue;
			}
			Iterator<String> iterator = mayBeCommitted.iterator();
			while (iterator.hasNext()) {
				String candidate = iterator.next();
				if (candidate.startsWith(prefix)) {
					iterator.remove();
					preselectionCandidates.add(candidate);
				}
			}
			if (mayBeCommitted.isEmpty()) {
				return preselectionCandidates;
			}
			lastAncestor = prefix;
		}
		selectedFiles.retainAll(mayBeCommitted);
		preselectionCandidates.addAll(selectedFiles);
		return preselectionCandidates;
	}

	/**
	 * Calculates a fresh {@link IndexDiff} for the given repository.
	 *
	 * @param repository
	 *            to compute the {@link IndexDiff} for
	 * @param selectedProjects
	 *            of the repository; used to get an estimate for the progress
	 *            monitor; may be empty
	 * @param monitor
	 *            for progress reporting and cancellation
	 * @return the {@link IndexDiff}
	 * @throws IOException
	 *             if an error occurred
	 * @throws OperationCanceledException
	 *             if the operation was cancelled
	 */
	public static IndexDiff getIndexDiff(Repository repository,
			IProject[] selectedProjects, IProgressMonitor monitor)
			throws IOException, OperationCanceledException {
		SubMonitor progress = SubMonitor.convert(monitor,
				UIText.CommitActionHandler_calculatingChanges, 1000);
		EclipseGitProgressTransformer jgitMonitor = new EclipseGitProgressTransformer(
				progress);
		CountingVisitor counter = new CountingVisitor();
		for (IProject p : selectedProjects) {
			try {
				p.accept(counter);
			} catch (CoreException e) {
				// ignore
			}
		}
		WorkingTreeIterator it = IteratorService
				.createInitialIterator(repository);
		if (it == null) {
			// Workspace is closed
			throw new OperationCanceledException();
		}
		IndexDiff diff = new IndexDiff(repository, Constants.HEAD, it);
		diff.diff(jgitMonitor, counter.count, 0,
				NLS.bind(UIText.CommitActionHandler_repository,
						repository.getDirectory().getPath()));
		if (progress.isCanceled()) {
			throw new OperationCanceledException();
		}
		return diff;
	}

	private void buildIndexHeadDiffList(IProject[] selectedProjects,
			IProgressMonitor monitor) throws IOException,
			OperationCanceledException {

		indexDiff = getIndexDiff(repo, selectedProjects, monitor);
		includeList(indexDiff.getAdded(), indexChanges);
		includeList(indexDiff.getChanged(), indexChanges);
		includeList(indexDiff.getRemoved(), indexChanges);
		includeList(indexDiff.getMissing(), notIndexed);
		includeList(indexDiff.getModified(), notIndexed);
		includeList(indexDiff.getUntracked(), notTracked);
	}

	static class CountingVisitor implements IResourceVisitor {
		int count;
		@Override
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

/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Jing Xue <jingxue@digizenstudio.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Commit either using the {@link CommitUI} dialog or the {@link StagingView}.
 */
public class CommitActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final Repository repository = getRepository(true, event);
		if (repository == null) {
			return null;
		}
		IPreferenceStore uiPreferences = Activator.getDefault()
				.getPreferenceStore();
		boolean useStagingView = uiPreferences
				.getBoolean(UIPreferences.ALWAYS_USE_STAGING_VIEW);
		if (useStagingView) {
			if (uiPreferences.getBoolean(UIPreferences.AUTO_STAGE_ON_COMMIT)) {
				boolean includeUntracked = uiPreferences.getBoolean(
						UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED);
				autoStage(repository, includeUntracked,
						getResourcesInScope(event));
			}
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

				@Override
				public void run() {
					try {
						StagingView view = (StagingView) PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage()
								.showView(StagingView.VIEW_ID);
						if (view.getCurrentRepository() != repository) {
							view.reload(repository);
						}
						view.setFocus();
					} catch (PartInitException e) {
						Activator.logError(e.getMessage(), e);
					}
				}
			});
		} else {
			final Shell shell = getShell(event);
			IResource[] resourcesInScope = getResourcesInScope(event);
			if (resourcesInScope != null) {
				CommitUI commitUi = new CommitUI(shell, repository,
						resourcesInScope, false);
				commitUi.commit();
			}
		}
		return null;
	}

	private IResource[] getResourcesInScope(ExecutionEvent event)
			throws ExecutionException {
		try {
			IResource[] selectedResources = getSelectedResources(event);
			if (selectedResources.length > 0) {
				IWorkbenchPart part = getPart(event);
				return GitScopeUtil.getRelatedChanges(part, selectedResources);
			} else {
				return new IResource[0];
			}
		} catch (InterruptedException e) {
			// ignore, we will not show the commit dialog in case the user
			// cancels the scope operation
			return null;
		}
	}

	private IndexDiffData getIndexDiffData(final @NonNull Repository repository,
			final @NonNull Collection<IProject> projects) {
		IndexDiffCacheEntry diffCacheEntry = org.eclipse.egit.core.Activator
				.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(repository);
		IndexDiffData diff = null;
		if (diffCacheEntry != null) {
			diff = diffCacheEntry.getIndexDiff();
		}
		if (diff != null) {
			return diff;
		}
		final IndexDiffData[] result = { null };
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {

						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								result[0] = new IndexDiffData(
										CommitUI.getIndexDiff(repository,
												projects.toArray(
														new IProject[projects
																.size()]),
												monitor));
							} catch (IOException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
		} catch (InvocationTargetException e) {
			Activator.handleError(UIText.CommitAction_errorComputingDiffs,
					e.getCause(), true);
			return null;
		} catch (InterruptedException e) {
			return null;
		}
		return result[0];
	}

	private void autoStage(final @NonNull Repository repository,
			boolean includeUntracked, IResource[] resourcesInScope) {
		if (resourcesInScope == null || resourcesInScope.length == 0) {
			return;
		}
		final Set<IProject> projects = new HashSet<>();
		for (IResource rsc : resourcesInScope) {
			projects.add(rsc.getProject());
		}
		IndexDiffData diff = getIndexDiffData(repository, projects);
		if (diff == null) {
			return;
		}
		Set<String> mayBeCommitted = new HashSet<>();
		mayBeCommitted.addAll(diff.getAdded());
		mayBeCommitted.addAll(diff.getChanged());
		mayBeCommitted.addAll(diff.getRemoved());
		mayBeCommitted.addAll(diff.getModified());
		mayBeCommitted.addAll(diff.getMissing());
		if (!includeUntracked) {
			mayBeCommitted.removeAll(diff.getUntracked());
		} else {
			mayBeCommitted.addAll(diff.getUntracked());
		}
		mayBeCommitted.removeAll(diff.getAssumeUnchanged());
		final Set<String> toBeStaged = CommitUI.getSelectedFiles(repository,
				mayBeCommitted, resourcesInScope);
		if (toBeStaged.isEmpty()) {
			return;
		}
		Job job = new Job(UIText.AddToIndexAction_addingFiles) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor progress = SubMonitor.convert(monitor,
						toBeStaged.size() + 1);
				try (Git git = new Git(repository)) {
					AddCommand add = git.add();
					for (String toStage : toBeStaged) {
						add.addFilepattern(toStage);
						if (progress.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						progress.worked(1);
					}
					add.call();
					progress.worked(1);
				} catch (GitAPIException e) {
					return Activator.createErrorStatus(
							CoreText.AddToIndexOperation_failed, e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobFamilies.ADD_TO_INDEX.equals(family)
						|| super.belongsTo(family);
			}

		};
		job.setUser(true);
		job.setRule(RuleUtil.getRule(repository));
		job.schedule();
	}

	@Override
	public boolean isEnabled() {
		return selectionMapsToSingleRepository();
	}

}

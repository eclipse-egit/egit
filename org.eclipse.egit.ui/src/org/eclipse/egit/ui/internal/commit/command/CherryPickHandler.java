/******************************************************************************
 *  Copyright (c) 2010, 2016 SAP AG, GitHub Inc., and others
 *  and other copyright owners as documented in the project's IP log.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Handler to cherry pick the commit onto the current branch
 */
public class CherryPickHandler extends SelectionHandler {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.commit.CherryPick"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = getSelectedItem(RevCommit.class, event);
		if (commit == null) {
			return null;
		}
		Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null) {
			return null;
		}
		final Shell shell = getPart(event).getSite().getShell();

		int parentIndex = -1;
		if (commit.getParentCount() > 1) {
			// Merge commit: select parent
			List<RevCommit> parents = new ArrayList<>();
			String branch = null;
			try {
				for (RevCommit parent : commit.getParents()) {
					parents.add(repo.parseCommit(parent));
				}
				branch = repo.getBranch();
			} catch (Exception e) {
				Activator.handleError(e.getLocalizedMessage(), e, true);
				return null;
			}
			CommitSelectDialog selectCommit = new CommitSelectDialog(shell,
					parents, getLaunchMessage(repo));
			selectCommit.create();
			selectCommit.setTitle(UIText.CommitSelectDialog_ChooseParentTitle);
			selectCommit.setMessage(MessageFormat.format(
					UIText.CherryPickHandler_CherryPickMergeMessage,
					commit.abbreviate(7).name(), branch));
			if (selectCommit.open() != Window.OK) {
				return null;
			}
			parentIndex = parents.indexOf(selectCommit.getSelectedCommit());
		} else if (!confirmCherryPick(shell, repo, commit)) {
			return null;
		}

		try {
			if (!UIRepositoryUtils.handleUncommittedFiles(repo, shell))
				return null;
		} catch (GitAPIException e) {
			Activator.logError(e.getMessage(), e);
			return null;
		}

		final CherryPickOperation op = new CherryPickOperation(repo, commit);
		op.setMainlineIndex(parentIndex);

		Job job = new Job(MessageFormat.format(
				UIText.CherryPickHandler_JobName, Integer.valueOf(1))) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
					CherryPickResult cherryPickResult = op.getResult();
					RevCommit newHead = cherryPickResult.getNewHead();
					if (newHead != null
							&& cherryPickResult.getCherryPickedRefs()
									.isEmpty()) {
						showNotPerformedDialog(shell);
					}
					if (newHead == null) {
						CherryPickStatus status = cherryPickResult.getStatus();
						switch (status) {
						case CONFLICTING:
							showConflictDialog(shell);
							break;
						case FAILED:
							showFailure(cherryPickResult);
							break;
						case OK:
							break;
						}
					}
				} catch (CoreException e) {
					Activator.logError(
							UIText.CherryPickOperation_InternalError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.CHERRY_PICK.equals(family)) {
					return true;
				}
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private String getLaunchMessage(Repository repository) {
		ILaunchConfiguration launch = LaunchFinder
				.getRunningLaunchConfiguration(
						Collections.singleton(repository), null);
		if (launch != null) {
			return MessageFormat.format(
					UIText.LaunchFinder_RunningLaunchMessage, launch.getName());
		}
		return null;
	}

	private boolean confirmCherryPick(final Shell shell,
			final Repository repository, final RevCommit commit)
			throws ExecutionException {
		final AtomicBoolean confirmed = new AtomicBoolean(false);
		String message;
		try {
			message = MessageFormat.format(
					UIText.CherryPickHandler_ConfirmMessage, Integer.valueOf(1),
					repository.getBranch());
		} catch (IOException e) {
			throw new ExecutionException(
					"Exception obtaining current repository branch", e); //$NON-NLS-1$
		}

		String launchMessage = getLaunchMessage(repository);
		if (launchMessage != null) {
			message += "\n\n" + launchMessage; //$NON-NLS-1$
		}
		final String question = message;
		shell.getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				ConfirmCherryPickDialog dialog = new ConfirmCherryPickDialog(
						shell, question, repository, Arrays.asList(commit));
				int result = dialog.open();
				confirmed.set(result == Window.OK);
			}
		});
		return confirmed.get();
	}

	private static class ConfirmCherryPickDialog extends MessageDialog {

		private RepositoryCommit[] commits;

		public ConfirmCherryPickDialog(Shell parentShell,
				String message, Repository repository, List<RevCommit> revCommits) {
			super(parentShell, UIText.CherryPickHandler_ConfirmTitle, null,
					message, MessageDialog.CONFIRM, new String[] {
							UIText.CherryPickHandler_cherryPickButtonLabel,
							IDialogConstants.CANCEL_LABEL }, 0);
			setShellStyle(getShellStyle() | SWT.RESIZE);

			List<RepositoryCommit> repoCommits = new ArrayList<>();
			for (RevCommit commit : revCommits)
				repoCommits.add(new RepositoryCommit(repository, commit));
			this.commits = repoCommits.toArray(new RepositoryCommit[0]);
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite area = new Composite(parent, SWT.NONE);
			area.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
					.create());
			area.setLayout(new FillLayout());

			TreeViewer treeViewer = new TreeViewer(area);
			treeViewer.setContentProvider(new ContentProvider());
			treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
					new WorkbenchLabelProvider()));
			treeViewer.setInput(commits);

			return area;
		}

		private static class ContentProvider extends WorkbenchContentProvider {

			@Override
			public Object[] getElements(final Object element) {
				return (Object[]) element;
			}

			@Override
			public Object[] getChildren(Object element) {
				if (element instanceof RepositoryCommit)
					return ((RepositoryCommit) element).getDiffs();
				return super.getChildren(element);
			}
		}
	}

	private void showNotPerformedDialog(final Shell shell) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openWarning(shell,
						UIText.CherryPickHandler_NoCherryPickPerformedTitle,
						UIText.CherryPickHandler_NoCherryPickPerformedMessage);
			}
		});
	}

	private void showConflictDialog(final Shell shell) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openWarning(shell,
						UIText.CherryPickHandler_CherryPickConflictsTitle,
						UIText.CherryPickHandler_CherryPickConflictsMessage);
			}
		});
	}

	private void showFailure(CherryPickResult result) {
		IStatus details = getErrorList(result.getFailingPaths());
		Activator.showErrorStatus(
				UIText.CherryPickHandler_CherryPickFailedMessage, details);
	}

	private IStatus getErrorList(Map<String, MergeFailureReason> failingPaths) {
		MultiStatus result = new MultiStatus(Activator.getPluginId(),
				IStatus.ERROR,
				UIText.CherryPickHandler_CherryPickFailedMessage, null);
		for (Entry<String, MergeFailureReason> entry : failingPaths.entrySet()) {
			String path = entry.getKey();
			String reason = getReason(entry.getValue());
			String errorMessage = NLS.bind(
					UIText.CherryPickHandler_ErrorMsgTemplate, path, reason);
			result.add(Activator.createErrorStatus(errorMessage));
		}
		return result;
	}

	private String getReason(MergeFailureReason mergeFailureReason) {
		switch (mergeFailureReason) {
		case COULD_NOT_DELETE:
			return UIText.CherryPickHandler_CouldNotDeleteFile;
		case DIRTY_INDEX:
			return UIText.CherryPickHandler_IndexDirty;
		case DIRTY_WORKTREE:
			return UIText.CherryPickHandler_WorktreeDirty;
		}
		return UIText.CherryPickHandler_unknown;
	}
}

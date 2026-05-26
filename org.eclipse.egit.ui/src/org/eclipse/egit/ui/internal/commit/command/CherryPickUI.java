/******************************************************************************
 *  Copyright (c) 2010, 2018 SAP AG, GitHub Inc., and others
 *  and other copyright owners as documented in the project's IP log.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - factored out of CherryPickHandler
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.egit.ui.internal.jobs.RepositoryJob;
import org.eclipse.egit.ui.internal.jobs.RepositoryJobResultAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * UI to cherry-pick commits onto HEAD.
 */
public class CherryPickUI {

	/**
	 * UI for doing a cherry-pick. Will prompt for confirmation if
	 * {@code confirm} is {@code true} or the {@code commit} is a merge commit,
	 * in which case the user will also be given the opportunity to choose the
	 * parent commit to define the changes to be applied. Always will inform the
	 * user about running launch configurations that might be affected by the
	 * working tree changes.
	 * <p>
	 * Must be called in the UI thread.
	 * </p>
	 *
	 * @param repository
	 *            to work on
	 * @param commit
	 *            to cherry-pick
	 * @param confirm
	 *            whether to ask for confirmation before doing the operation
	 * @throws CoreException
	 *             if an error occurs
	 */
	public void run(@NonNull
	Repository repository, @NonNull
	RevCommit commit, boolean confirm) throws CoreException {
		@SuppressWarnings("null")
		@NonNull
		List<RevCommit> commits = List.of(commit);
		doRun(repository, commits, confirm);
	}

	/**
	 * UI for doing a cherry-pick. Will prompt for confirmation if
	 * {@code confirm} is {@code true} or the {@code commit} is a merge commit,
	 * in which case the user will also be given the opportunity to choose the
	 * parent commit to define the changes to be applied. The commits will
	 * initially be selected in the given order, but can be reorder by the user
	 * if {@code confirm} is {@code true}. Always will inform the user about
	 * running launch configurations that might be affected by the working tree
	 * changes.
	 * <p>
	 * Must be called in the UI thread.
	 * </p>
	 *
	 * @param repository
	 *            to work on
	 * @param commits
	 *            to cherry-pick
	 * @param confirm
	 *            whether to ask for confirmation before doing the operation.
	 *            Allows reordering of commits is {@code true}
	 * @throws CoreException
	 *             if an error occurs
	 */
	public void run(@NonNull
	Repository repository, @NonNull
	List<RevCommit> commits, boolean confirm) throws CoreException {
		doRun(repository, commits, confirm);
	}

	private static void doRun(@NonNull
	Repository repository, @NonNull
	List<RevCommit> commits, boolean confirm) throws CoreException {
		final Shell shell = PlatformUI.getWorkbench()
				.getModalDialogShellProvider().getShell();

		RevCommit mergeCommit = null;
		for (RevCommit commit : commits) {
			if (commit.getParentCount() > 1) {
				if (mergeCommit == null) {
					mergeCommit = commit;
				} else {
					// Although cherry-picking multiple merge commits is allowed
					// by Git, all of those commits must select the same
					// mainline parent number. This could lead to unexpected
					// behaviour if picked silently and there's no good way to
					// prompt the user for this information, so multiple merge
					// commits are not supported here.
					MessageDialog.openInformation(
							PlatformUI.getWorkbench()
									.getModalDialogShellProvider().getShell(),
							UIText.CherryPickHandler_CherryPickingMultipleMergeCommitsNotSupportedTitle,
							UIText.CherryPickHandler_CherryPickingMultipleMergeCommitsNotSupportedMessage);
					return;
				}
			}
		}

		int parentIndex = -1;
		if (mergeCommit != null) {
			// Merge commit: select parent
			List<RevCommit> parents = new ArrayList<>();
			String branch = null;
			try {
				for (RevCommit parent : mergeCommit.getParents()) {
					parents.add(repository.parseCommit(parent));
				}
				branch = repository.getBranch();
			} catch (Exception e) {
				Activator.handleError(e.getLocalizedMessage(), e, true);
			}
			CommitSelectDialog selectCommit = new CommitSelectDialog(shell,
					parents, getLaunchMessage(repository));
			selectCommit.create();
			selectCommit.setTitle(UIText.CommitSelectDialog_ChooseParentTitle);
			selectCommit.setMessage(MessageFormat.format(
					UIText.CherryPickHandler_CherryPickMergeMessage,
					Utils.getShortObjectId(mergeCommit), branch));
			if (selectCommit.open() != Window.OK) {
				return;
			}
			parentIndex = parents.indexOf(selectCommit.getSelectedCommit());
		}

		List<RevCommit> reorderedCommits = commits;
		if (confirm) {
			Optional<List<RevCommit>> result = confirmCherryPick(shell,
					repository, commits);
			if (result.isEmpty()) {
				return;
			}
			reorderedCommits = result.get();
		} else if (LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
				null)) {
			return;
		}

		doCherryPick(repository, reorderedCommits, parentIndex, true);
	}

	private static void doCherryPick(@NonNull
	Repository repo, List<RevCommit> commits, int parentIndex,
			boolean withCleanup) {
		CherryPickOperation operation = new CherryPickOperation(repo, commits);
		operation.setMainlineIndex(parentIndex);

		Job job = new RepositoryJob(MessageFormat.format(
				UIText.CherryPickHandler_JobName,
				Integer.valueOf(commits.size())), null) {

			private CherryPickResult result;

			@Override
			protected IStatus performJob(IProgressMonitor monitor) {
				try {
					operation.execute(monitor);
					result = operation.getResult();
					if (!withCleanup
							&& result.getStatus() == CherryPickStatus.FAILED) {
						return getErrorList(result.getFailingPaths());
					}
				} catch (CoreException e) {
					return Activator.createErrorStatus(
							UIText.CherryPickOperation_InternalError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			protected IAction getAction() {
				RevCommit newHead = result.getNewHead();
				if (newHead == null) {
					switch (result.getStatus()) {
					case CONFLICTING:
						return new MessageAction(
								UIText.CherryPickHandler_CherryPickConflictsTitle,
								UIText.CherryPickHandler_CherryPickConflictsMessage);
					case FAILED:
						if (!withCleanup) {
							return new RepositoryJobResultAction(repo,
									UIText.CherryPickHandler_CherryPickFailedMessage) {

								@Override
								protected void showResult(
										Repository repository) {
									Activator.showErrorStatus(
											UIText.CherryPickHandler_CherryPickFailedMessage,
											getErrorList(
													result.getFailingPaths()));
								}
							};
						}
						return new CleanupAction(repo,
								UIText.CherryPickHandler_UncommittedFilesTitle,
								result, () -> doCherryPick(repo, commits,
										parentIndex, false));
					case OK:
						return null;
					}
				} else if (result.getCherryPickedRefs().isEmpty()) {
					return new MessageAction(
							UIText.CherryPickHandler_NoCherryPickPerformedTitle,
							UIText.CherryPickHandler_NoCherryPickPerformedMessage);
				}
				return null;
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
		job.setRule(operation.getSchedulingRule());
		job.schedule();
	}

	private static String getLaunchMessage(Repository repository) {
		String launch = LaunchFinder.getRunningLaunchConfiguration(
				Collections.singleton(repository), null);
		if (launch != null) {
			return MessageFormat
					.format(UIText.LaunchFinder_RunningLaunchMessage, launch);
		}
		return null;
	}

	private static Optional<List<RevCommit>> confirmCherryPick(
			final Shell shell, final Repository repository,
			final List<RevCommit> commits) throws CoreException {
		String message;
		try {
			message = MessageFormat.format(
					UIText.CherryPickHandler_ConfirmMessage,
					Integer.valueOf(commits.size()), repository.getBranch());
		} catch (IOException e) {
			throw new CoreException(Activator.createErrorStatus(
					"Exception obtaining current repository branch", e)); //$NON-NLS-1$
		}

		String launchMessage = getLaunchMessage(repository);
		if (launchMessage != null) {
			message += "\n\n" + launchMessage; //$NON-NLS-1$
		}
		final String question = message;
		List<RevCommit> reorderedCommits = new ArrayList<>();
		shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				ConfirmCherryPickDialog dialog = new ConfirmCherryPickDialog(
						shell, question, repository, commits);
				if (dialog.open() == Window.OK) {
					for (RepositoryCommit commit : dialog.getCommits()) {
						reorderedCommits.add(commit.getRevCommit());
					}
				}
			}
		});
		return reorderedCommits.isEmpty() ? Optional.empty()
				: Optional.of(reorderedCommits);
	}

	private static class ConfirmCherryPickDialog extends MessageDialog {

		private RepositoryCommit[] commits;

		private TreeViewer treeViewer;

		private Button upButton;

		private Button downButton;

		private final SelectionListener buttonListener = SelectionListener
				.widgetSelectedAdapter(e -> {
					int index = getSelectedCommitIndex();
					if (index == -1) {
						return;
					}
					Object source = e.getSource();
					int newIndex = source == upButton ? index - 1 : index + 1;
					swapCommits(index, newIndex);
					updateButtonEnablement(newIndex);
					treeViewer.refresh();
				});

		public ConfirmCherryPickDialog(Shell parentShell, String message,
				Repository repository, List<RevCommit> revCommits) {
			super(parentShell,
					MessageFormat.format(UIText.CherryPickHandler_ConfirmTitle,
							Integer.valueOf(revCommits.size())),
					null, message, MessageDialog.CONFIRM,
					new String[] {
							UIText.CherryPickHandler_cherryPickButtonLabel,
							IDialogConstants.CANCEL_LABEL },
					0);
			setShellStyle(getShellStyle() | SWT.RESIZE);

			List<RepositoryCommit> repoCommits = new ArrayList<>();
			for (RevCommit commit : revCommits)
				repoCommits.add(new RepositoryCommit(repository, commit));
			this.commits = repoCommits.toArray(new RepositoryCommit[0]);
		}

		public RepositoryCommit[] getCommits() {
			return commits;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			final boolean showReorderButtons = commits.length > 1;

			Composite area = new Composite(parent, SWT.NONE);
			area.setLayoutData(
					GridDataFactory.fillDefaults().grab(true, true).create());
			area.setLayout(new GridLayout(showReorderButtons ? 2 : 1, false));

			Composite viewerComposite = new Composite(area, SWT.NONE);
			viewerComposite.setLayoutData(
					GridDataFactory.fillDefaults().grab(true, true).create());
			viewerComposite.setLayout(new FillLayout());

			treeViewer = new TreeViewer(viewerComposite);
			treeViewer.setContentProvider(new ContentProvider());
			treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
					new WorkbenchLabelProvider()));
			treeViewer.setInput(getCommits());

			if (showReorderButtons) {
				Composite buttonComposite = new Composite(area, SWT.NONE);
				buttonComposite.setLayoutData(GridDataFactory.fillDefaults()
						.hint(80, SWT.DEFAULT).grab(false, true)
						.align(SWT.BEGINNING, SWT.BEGINNING).create());
				buttonComposite.setLayout(new FillLayout(SWT.VERTICAL));

				upButton = ButtonFactory.newButton(SWT.PUSH)
						.text(UIText.CherryPickHandler_upButtonLabel)
						.create(buttonComposite);
				upButton.setEnabled(false);
				upButton.addSelectionListener(buttonListener);

				downButton = ButtonFactory.newButton(SWT.PUSH)
						.text(UIText.CherryPickHandler_downButtonLabel)
						.create(buttonComposite);
				downButton.setEnabled(false);
				downButton.addSelectionListener(buttonListener);

				treeViewer.addSelectionChangedListener(selection -> {
					int index = getSelectedCommitIndex();
					if (index == -1) {
						upButton.setEnabled(false);
						downButton.setEnabled(false);
						return;
					}
					updateButtonEnablement(index);
				});
			}

			return area;
		}

		private int getSelectedCommitIndex() {
			Object selected = treeViewer.getStructuredSelection()
					.getFirstElement();
			if (selected instanceof RepositoryCommit commit) {
				for (int i = 0; i < commits.length; i++) {
					if (commit == commits[i]) {
						return i;
					}
				}
			}
			return -1;
		}

		private void updateButtonEnablement(int selectedIndex) {
			upButton.setEnabled(selectedIndex > 0);
			downButton.setEnabled(selectedIndex < commits.length - 1);
		}

		private void swapCommits(int firstIndex, int secondIndex) {
			RepositoryCommit temp = commits[firstIndex];
			commits[firstIndex] = commits[secondIndex];
			commits[secondIndex] = temp;
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

	private static IStatus getErrorList(
			Map<String, MergeFailureReason> failingPaths) {
		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR,
				UIText.CherryPickHandler_CherryPickFailedMessage, null);
		for (Entry<String, MergeFailureReason> entry : failingPaths
				.entrySet()) {
			String path = entry.getKey();
			String reason = getReason(entry.getValue());
			String errorMessage = NLS.bind(
					UIText.CherryPickHandler_ErrorMsgTemplate, path, reason);
			result.add(Activator.createErrorStatus(errorMessage));
		}
		return result;
	}

	private static String getReason(MergeFailureReason mergeFailureReason) {
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

	/**
	 * Displays a simple warning dialog with the given title and message.
	 */
	private static class MessageAction extends Action {

		private final String title;

		private final String message;

		public MessageAction(String title, String message) {
			super(title);
			this.title = title;
			this.message = message;
		}

		@Override
		public void run() {
			MessageDialog.openWarning(PlatformUI.getWorkbench()
					.getModalDialogShellProvider().getShell(), title, message);
		}

	}

	/**
	 * If a cherry-pick failure was due to a dirty index or working tree only,
	 * show a dialog giving the user the opportunity to clean-up, and then
	 * re-try the cherry-pick. If there were any other failures, show an error
	 * dialog and abort.
	 */
	private static class CleanupAction extends RepositoryJobResultAction {

		private final CherryPickResult result;

		private final Runnable retry;

		public CleanupAction(@NonNull
		Repository repo, String title, CherryPickResult result,
				Runnable retry) {
			super(repo, title);
			this.result = result;
			this.retry = retry;
		}

		@Override
		protected void showResult(Repository repository) {
			Map<String, MergeFailureReason> failed = result.getFailingPaths();
			List<String> failedPaths = new ArrayList<>(failed.size());
			for (Map.Entry<String, MergeFailureReason> entry : failed
					.entrySet()) {
				MergeFailureReason reason = entry.getValue();
				if (reason == null) {
					Activator.showErrorStatus(
							UIText.CherryPickHandler_CherryPickFailedMessage,
							getErrorList(failed));
					return;
				} else {
					switch (reason) {
					case DIRTY_INDEX:
					case DIRTY_WORKTREE:
						failedPaths.add(entry.getKey());
						break;
					default:
						Activator.showErrorStatus(
								UIText.CherryPickHandler_CherryPickFailedMessage,
								getErrorList(failed));
						return;
					}
				}
			}
			if (UIRepositoryUtils.showCleanupDialog(repository, failedPaths,
					UIText.CherryPickHandler_UncommittedFilesTitle,
					PlatformUI.getWorkbench().getModalDialogShellProvider()
							.getShell())) {
				retry.run();
			}
		}
	}
}

/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.DeleteBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.FilteredCheckboxTree;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Delete a branch pointing to a commit.
 */
public class DeleteBranchOnCommitHandler extends AbstractHistoryCommandHandler {

	private static final class BranchMessageDialog extends MessageDialog {
		private final List<Ref> nodes;

		private BranchMessageDialog(Shell parentShell, List<Ref> nodes) {
			super(
					parentShell,
					UIText.DeleteBranchOnCommitHandler_ConfirmBranchDeletionDialogTitle,
					null,
					UIText.DeleteBranchOnCommitHandler_ConfirmBranchDeletionMessage,
					MessageDialog.QUESTION, new String[] {
							IDialogConstants.OK_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			this.nodes = nodes;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite area = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
					.applyTo(area);
			area.setLayout(new GridLayout(1, false));

			TableViewer branchesList = new TableViewer(area);
			GridDataFactory.fillDefaults().grab(true, true)
					.applyTo(branchesList.getTable());
			branchesList.setContentProvider(ArrayContentProvider.getInstance());
			branchesList.setLabelProvider(new GitLabelProvider());
			branchesList.setInput(nodes);
			return area;
		}

	}

	private static final class BranchSelectionDialog extends MessageDialog {
		private final List<Ref> nodes;

		private final List<Ref> selected = new ArrayList<Ref>();

		private FilteredCheckboxTree fTree;

		private BranchSelectionDialog(Shell parentShell, List<Ref> nodes) {
			super(
					parentShell,
					UIText.DeleteBranchOnCommitHandler_SelectBranchDialogTitle,
					null,
					UIText.DeleteBranchOnCommitHandler_SelectBranchDialogMessage,
					MessageDialog.QUESTION, new String[] {
							IDialogConstants.OK_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			this.nodes = nodes;
		}

		@Override
		public void create() {
			super.create();
			getButton(OK).setEnabled(false);
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite area = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
					.applyTo(area);
			area.setLayout(new GridLayout(1, false));
			fTree = new FilteredCheckboxTree(area, null, SWT.NONE,
					new PatternFilter()) {
				/*
				 * Overridden to check page when refreshing is done.
				 */
				protected WorkbenchJob doCreateRefreshJob() {
					WorkbenchJob refreshJob = super.doCreateRefreshJob();
					refreshJob.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								getDisplay().asyncExec(new Runnable() {
									public void run() {
										checkPage();
									}
								});
							}
						}
					});
					return refreshJob;
				}
			};

			CachedCheckboxTreeViewer viewer = fTree.getCheckboxTreeViewer();
			GridDataFactory.fillDefaults().grab(true, true).applyTo(fTree);
			viewer.setContentProvider(new ITreeContentProvider() {
				public void inputChanged(Viewer actViewer, Object oldInput,
						Object newInput) {
					// nothing
				}

				public void dispose() {
					// nothing
				}

				public boolean hasChildren(Object element) {
					return false;
				}

				public Object getParent(Object element) {
					return null;
				}

				public Object[] getElements(Object inputElement) {
					return ((List) inputElement).toArray();
				}

				public Object[] getChildren(Object parentElement) {
					return null;
				}
			});

			viewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					checkPage();
				}
			});

			viewer.setLabelProvider(new GitLabelProvider());
			viewer.setInput(nodes);
			return area;
		}

		private void checkPage() {
			getButton(OK).setEnabled(
					fTree.getCheckboxTreeViewer().getCheckedLeafCount() > 0);

		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.OK_ID) {
				Object[] checked = fTree.getCheckboxTreeViewer()
						.getCheckedElements();
				for (Object o : checked) {
					if (o instanceof Ref)
						selected.add((Ref) o);
				}
			}
			super.buttonPressed(buttonId);
		}

		List<Ref> getSelected() {
			return selected;
		}

	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		GitHistoryPage page = getPage();

		final Repository repository = getRepository(page);
		if (repository == null)
			return null;

		List<Ref> branchesOfCommit;
		try {
			branchesOfCommit = getBranchesOfCommit(page, repository);
		} catch (IOException e) {
			throw new ExecutionException("Could not obtain current Branch", e); //$NON-NLS-1$
		}
		// this should have been checked by isEnabled()
		if (branchesOfCommit.isEmpty())
			return null;

		final List<Ref> unmergedBranches = new ArrayList<Ref>();
		final Shell shell = getPart(event).getSite().getShell();

		final List<Ref> branchesToDelete;
		if (branchesOfCommit.size() > 1) {
			BranchSelectionDialog dlg = new BranchSelectionDialog(shell,
					branchesOfCommit);
			if (dlg.open() != Window.OK)
				return null;
			branchesToDelete = dlg.getSelected();
		} else
			branchesToDelete = branchesOfCommit;

		try {
			new ProgressMonitorDialog(shell).run(false, false,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								monitor.beginTask(
										UIText.DeleteBranchCommand_DeletingBranchesProgress,
										branchesToDelete.size());
								for (Ref refNode : branchesToDelete) {
									int result = deleteBranch(repository,
											refNode, false);
									if (result == DeleteBranchOperation.REJECTED_CURRENT) {
										throw new CoreException(
												Activator
														.createErrorStatus(
																UIText.DeleteBranchCommand_CannotDeleteCheckedOutBranch,
																null));
									} else if (result == DeleteBranchOperation.REJECTED_UNMERGED) {
										unmergedBranches.add(refNode);
									} else
										monitor.worked(1);
								}
								if (!unmergedBranches.isEmpty()) {
									MessageDialog messageDialog = new BranchMessageDialog(
											shell, unmergedBranches);
									if (messageDialog.open() == Window.OK) {
										for (Ref node : unmergedBranches) {
											deleteBranch(repository, node, true);
											monitor.worked(1);
										}
									}
								}
							} catch (CoreException ex) {
								throw new InvocationTargetException(ex);
							} finally {
								monitor.done();
							}
						}
					});
		} catch (InvocationTargetException e1) {
			Activator.handleError(
					UIText.RepositoriesView_BranchDeletionFailureMessage,
					e1.getCause(), true);
		} catch (InterruptedException e1) {
			// ignore
		}

		return null;
	}

	private List<Ref> getBranchesOfCommit(GitHistoryPage page,
			final Repository repo) throws IOException {
		final List<Ref> branchesOfCommit = new ArrayList<Ref>();
		IStructuredSelection selection = getSelection(page);
		if (selection.isEmpty())
			return branchesOfCommit;
		PlotCommit commit = (PlotCommit) selection.getFirstElement();
		String head = repo.getFullBranch();

		int refCount = commit.getRefCount();
		for (int i = 0; i < refCount; i++) {
			Ref ref = commit.getRef(i);
			String refName = ref.getName();
			if (head != null && refName.equals(head))
				continue;
			if (refName.startsWith(Constants.R_HEADS)
					|| refName.startsWith(Constants.R_REMOTES))
				branchesOfCommit.add(ref);
		}
		return branchesOfCommit;
	}

	private Repository getRepository(GitHistoryPage page) {
		if (page == null)
			return null;
		HistoryPageInput input = page.getInputInternal();
		if (input == null)
			return null;

		final Repository repository = input.getRepository();
		return repository;
	}

	private int deleteBranch(Repository repo, final Ref ref, boolean force)
			throws CoreException {
		DeleteBranchOperation dbop = new DeleteBranchOperation(repo, ref, force);
		dbop.execute(null);
		return dbop.getStatus();
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();

		Repository repository = getRepository(page);
		if (repository == null)
			return false;

		List<Ref> branchesOfCommit;
		try {
			branchesOfCommit = getBranchesOfCommit(page, repository);
		} catch (IOException e) {
			Activator.logError("Could not calculate Enablement", e); //$NON-NLS-1$
			return false;
		}
		return !branchesOfCommit.isEmpty();
	}
}

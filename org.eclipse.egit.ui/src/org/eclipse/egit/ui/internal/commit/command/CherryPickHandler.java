/******************************************************************************
 *  Copyright (c) 2010 SAP AG.
 *  Copyright (c) 2011, 2014 GitHub Inc.
 *  and other copyright owners as documented in the project's IP log.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Maik Schreiber - modify to using interactive rebase mechanics
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIRepositoryUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;
import org.eclipse.egit.ui.internal.rebase.RebaseResultDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
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

	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RevCommit> commits = getSelectedItems(RevCommit.class, event);
		if ((commits == null) || commits.isEmpty())
			return null;
		final Repository repo = getSelectedItem(Repository.class, event);
		if (repo == null)
			return null;
		final Shell parent = getPart(event).getSite().getShell();

		if (!confirmCherryPick(parent, repo, commits))
			return null;

		try {
			if (!UIRepositoryUtils.handleUncommittedFiles(repo, parent))
				return null;
		} catch (GitAPIException e) {
			Activator.logError(e.getMessage(), e);
			return null;
		}

		final CherryPickOperation op = new CherryPickOperation(repo, commits);
		Job job = new Job(MessageFormat.format(
				UIText.CherryPickHandler_JobName,
				Integer.valueOf(commits.size()))) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
					RebaseResult result = op.getResult();
					if (result.getStatus() != RebaseResult.Status.OK) {
						RebaseResultDialog.show(result, repo);
					}
				} catch (CoreException e) {
					Activator.logError(
							UIText.CherryPickOperation_InternalError, e);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.CHERRY_PICK.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private boolean confirmCherryPick(final Shell shell,
			final Repository repository, final List<RevCommit> commits)
			throws ExecutionException {
		final AtomicBoolean confirmed = new AtomicBoolean(false);
		final String message;
		try {
			message = MessageFormat.format(
					UIText.CherryPickHandler_ConfirmMessage,
					Integer.valueOf(commits.size()), repository.getBranch());
		} catch (IOException e) {
			throw new ExecutionException(
					"Exception obtaining current repository branch", e); //$NON-NLS-1$
		}

		shell.getDisplay().syncExec(new Runnable() {

			public void run() {
				ConfirmCherryPickDialog dialog = new ConfirmCherryPickDialog(
						shell, message, repository, commits);
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
							IDialogConstants.OK_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			setShellStyle(getShellStyle() | SWT.RESIZE);

			List<RepositoryCommit> repoCommits = new ArrayList<RepositoryCommit>();
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

			public Object[] getElements(final Object element) {
				return (Object[]) element;
			}

			public Object[] getChildren(Object element) {
				if (element instanceof RepositoryCommit)
					return ((RepositoryCommit) element).getDiffs();
				return super.getChildren(element);
			}
		}
	}
}

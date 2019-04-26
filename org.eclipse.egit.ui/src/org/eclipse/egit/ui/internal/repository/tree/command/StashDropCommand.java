/******************************************************************************
 *  Copyright (C) 2012, 2013 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.StashDropOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditorInput;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Command to drop one or all stashed commits
 */
public class StashDropCommand extends
		RepositoriesViewCommandHandler<StashedCommitNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<StashedCommitNode> nodes = getSelectedNodes(event);
		if (nodes.isEmpty())
			return null;

		final Repository repo = nodes.get(0).getRepository();
		if (repo == null)
			return null;

		// Confirm deletion of selected nodes
		final AtomicBoolean confirmed = new AtomicBoolean();
		final Shell shell = getActiveShell(event);
		shell.getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				final String message;
				if (nodes.size() > 1) {
					message = MessageFormat.format(
							UIText.StashDropCommand_confirmMultiple,
							Integer.toString(nodes.size()));
				} else {
					StashedCommitNode commit = nodes.get(0);
					message = MessageFormat.format(
							UIText.StashDropCommand_confirmSingle,
							Integer.toString(commit.getIndex()),
							commit.getObject().getShortMessage());
				}

				String[] buttonLabels = { UIText.StashDropCommand_buttonDelete,
						IDialogConstants.CANCEL_LABEL };

				MessageDialog confirmDialog = new MessageDialog(shell,
						UIText.StashDropCommand_confirmTitle, null, message,
						MessageDialog.CONFIRM, buttonLabels, 0);

				confirmed.set(confirmDialog.open() == Window.OK);
			}
		});
		if (!confirmed.get())
			return null;

		Job job = new Job(UIText.StashDropCommand_jobTitle) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(UIText.StashDropCommand_jobTitle,
						nodes.size());

				// Sort by highest to lowest stash commit index.
				// This avoids shifting problems that cause the indices of the
				// selected nodes not match the indices in the repository
				Collections.sort(nodes, new Comparator<StashedCommitNode>() {

					@Override
					public int compare(StashedCommitNode n1,
							StashedCommitNode n2) {
						return n1.getIndex() < n2.getIndex() ? 1 : -1;
					}
				});

				for (StashedCommitNode node : nodes) {
					final int index = node.getIndex();
					if (index < 0)
						return null;
					final RevCommit commit = node.getObject();
					if (commit == null)
						return null;
					final String stashName = node.getObject().getName();
					final StashDropOperation op = new StashDropOperation(repo,
							node.getIndex());
					monitor.subTask(stashName);
					try {
						op.execute(monitor);
					} catch (CoreException e) {
						Activator.logError(MessageFormat.format(
								UIText.StashDropCommand_dropFailed,
								node.getObject().name()), e);
					}
					tryToCloseEditor(node);
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			}

			private void tryToCloseEditor(final StashedCommitNode node) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						IWorkbenchPage activePage = PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage();
						IEditorReference[] editorReferences = activePage
								.getEditorReferences();
						for (IEditorReference editorReference : editorReferences) {
							IEditorInput editorInput = null;
							try {
								editorInput = editorReference.getEditorInput();
							} catch (PartInitException e) {
								Activator.handleError(e.getMessage(), e, true);
							}
							if (editorInput instanceof CommitEditorInput) {
								CommitEditorInput comEditorInput = (CommitEditorInput) editorInput;
								if (comEditorInput.getCommit().getRevCommit()
										.equals(node.getObject())) {
									activePage.closeEditor(
											editorReference.getEditor(false),
											false);
								}
							}
						}
					}
				});

			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.STASH.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule((new StashDropOperation(repo, nodes.get(0).getIndex()))
				.getSchedulingRule());
		job.schedule();
		return null;
	}
}

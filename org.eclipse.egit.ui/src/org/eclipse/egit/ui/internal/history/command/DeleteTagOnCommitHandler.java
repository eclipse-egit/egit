/*******************************************************************************
 * Copyright (C) 2014, Andreas Hermann <a.v.hermann@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.DeleteTagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.BranchSelectionDialog;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

/**
 * Delete a tag pointing to a commit.
 */
public class DeleteTagOnCommitHandler extends AbstractHistoryCommandHandler {

	@Override
	public Object execute(ExecutionEvent event)
			throws ExecutionException {
		final Repository repository = getRepository(event);
		if (repository == null)
			return null;

		final Shell shell = getPart(event).getSite().getShell();
		IStructuredSelection selection = getSelection(event);
		List<Ref> tags = getTagsOfCommit(selection);

		// this should have been checked by isEnabled()
		if (tags.isEmpty())
			return null;

		// show a dialog in case there are multiple tags on the selected commit
		final List<Ref> tagsToDelete;
		if (tags.size() > 1) {
			BranchSelectionDialog<Ref> dialog = new BranchSelectionDialog<>(
					shell,
					tags,
					UIText.DeleteTagOnCommitHandler_DeleteTagsDialogTitle,
					UIText.DeleteTagOnCommitHandler_DeleteTagsDialogMessage,
					UIText.DeleteTagOnCommitHandler_DeleteTagsDialogButton,
					SWT.MULTI);
			if (dialog.open() != Window.OK)
				return null;
			tagsToDelete = dialog.getSelectedNodes();
		} else {
			String tagName = Repository.shortenRefName(tags.get(0).getName());
			String message = MessageFormat.format(
					UIText.DeleteTagCommand_messageConfirmSingleTag, tagName);
			boolean confirmed = MessageDialog.openConfirm(shell,
					UIText.DeleteTagCommand_titleConfirm, message);
			if (!confirmed)
				return null;
			tagsToDelete = tags;
		}

		try {
			deleteTagsAsTask(shell, repository, tagsToDelete);
		} catch (InvocationTargetException e1) {
			Activator.handleError(
					UIText.RepositoriesView_TagDeletionFailureMessage,
					e1.getCause(), true);
		} catch (InterruptedException e1) {
			// ignore
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;

		IStructuredSelection selection = getSelection(page);
		if (selection.size() != 1)
			return false;

		List<Ref> tags = getTagsOfCommit(selection);
		return !tags.isEmpty();
	}

	private void deleteTagsAsTask(final Shell shell,
			final Repository repository, final List<Ref> tagsToDelete)
			throws InvocationTargetException,
			InterruptedException {
		new ProgressMonitorDialog(shell).run(true, false,
				new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						try {
							monitor.beginTask(
									UIText.DeleteTagCommand_deletingTagsProgress,
									tagsToDelete.size());
							for (Ref ref : tagsToDelete) {
								deleteTag(repository, ref.getName());
								monitor.worked(1);
							}
						} catch (CoreException ex) {
							throw new InvocationTargetException(ex);
						} finally {
							monitor.done();
						}
					}
				});
	}

	private List<Ref> getTagsOfCommit(IStructuredSelection selection) {
		final List<Ref> tagsOfCommit = new ArrayList<>();
		if (selection.isEmpty())
			return tagsOfCommit;
		PlotCommit commit = (PlotCommit) selection.getFirstElement();
		for (int i = 0; i < commit.getRefCount(); i++) {
			Ref ref = commit.getRef(i);
			if (ref.getName().startsWith(Constants.R_TAGS))
				tagsOfCommit.add(ref);
		}
		return tagsOfCommit;
	}

	private void deleteTag(Repository repository, String tagName)
			throws CoreException {
		DeleteTagOperation operation = new DeleteTagOperation(repository,
				tagName);
		operation.execute(null);
	}
}

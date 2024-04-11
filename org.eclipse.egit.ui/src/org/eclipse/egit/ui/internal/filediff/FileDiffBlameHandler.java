/******************************************************************************
 *  Copyright (c) 2024 Thomas Wolf <twolf@apache.org> and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.filediff;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.storage.CommitFileRevision;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Handler to blame a {@link FileDiff}.
 */
public class FileDiffBlameHandler extends AbstractFileDiffHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		FileDiff diff = getDiff(selection);
		if (diff != null) {
			showBlame(diff);
		}
		return null;
	}

	private void showBlame(FileDiff d) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			RevCommit commit = d.getChange().equals(ChangeType.DELETE)
					? d.getCommit().getParent(0)
					: d.getCommit();
			String path = d.getPath();
			IFileRevision rev = CompareUtils.getFileRevision(path, commit,
					d.getRepository(),
					d.getChange().equals(ChangeType.DELETE) ? d.getBlobs()[0]
							: d.getBlobs()[d.getBlobs().length - 1]);
			if (rev instanceof CommitFileRevision) {
				BlameOperation op = new BlameOperation((CommitFileRevision) rev,
						window.getShell(), page);
				JobUtil.scheduleUserJob(op, UIText.ShowBlameHandler_JobName,
						JobFamilies.BLAME);
			} else {
				String message = NLS.bind(
						UIText.DiffViewer_notContainedInCommit, path,
						d.getCommit().getId().getName());
				Activator.showError(message, null);
			}
		} catch (IOException e) {
			Activator.logError(UIText.GitHistoryPage_openFailed, e);
			Activator.showError(UIText.GitHistoryPage_openFailed, null);
		}
	}
}

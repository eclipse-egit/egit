/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.history.command;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;

/**
 * Executes the CherryPick
 */
public class CherryPickHandler extends AbstractHistoryCommandHandler {
	private static final String SPACE = " ";  //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		RevCommit commit = (RevCommit) getSelection(getPage()).getFirstElement();
		RevCommit newHead;
		Repository repo = getRepository(event);

		CherryPickResult cherryPickResult;
		Git git = new Git(repo);
		Shell parent = getPart(event).getSite().getShell();
		try {
			cherryPickResult = git.cherryPick().include(commit.getId()).call();
			newHead = cherryPickResult.getNewHead();
			if (newHead != null && cherryPickResult.getCherryPickedRefs().isEmpty())
				MessageDialog.openWarning(parent,
						UIText.CherryPickHandler_NoCherryPickPerformedTitle,
						UIText.CherryPickHandler_NoCherryPickPerformedMessage);
		} catch (Exception e) {
			throw new ExecutionException(UIText.CherryPickOperation_InternalError, e);
		}
		if (newHead == null) {
			CherryPickStatus status = cherryPickResult.getStatus();
			switch (status) {
			case CONFLICTING:
				MessageDialog.openWarning(parent,
						UIText.CherryPickHandler_CherryPickConflictsTitle,
						UIText.CherryPickHandler_CherryPickConflictsMessage);
				break;
			case FAILED:
				IStatus details = getErrorList(cherryPickResult.getFailingPaths());
				Activator.showErrorStatus(
						UIText.CherryPickHandler_CherryPickFailedMessage, details);
				break;
			case OK:
				break;
			}
		}
		return null;
	}

	private IStatus getErrorList(Map<String, MergeFailureReason> failingPaths) {
		MultiStatus result = new MultiStatus(Activator.getPluginId(),
				IStatus.ERROR,
				UIText.CherryPickHandler_CherryPickFailedMessage, null);
		for (String path : failingPaths.keySet()) {
			String reason = getReason(failingPaths.get(path));
			String errorMessage = path + SPACE + reason;
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
		return "unknown"; //$NON-NLS-1$

	}
}

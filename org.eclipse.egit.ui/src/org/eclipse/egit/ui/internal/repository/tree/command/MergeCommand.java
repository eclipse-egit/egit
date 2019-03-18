/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - move to command framework
 *    Dariusz Luksza (dariusz@luksza.org - set action disabled when HEAD cannot
 *    										be resolved
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 495777
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.ui.internal.actions.MergeActionHandler;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.egit.ui.internal.dialogs.MergeTargetSelectionDialog;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements "Merge"
 */
public class MergeCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		Repository repository = node.getRepository();

		if (repository == null
				|| !MergeActionHandler.checkMergeIsPossible(repository,
						getShell(event))
				|| LaunchFinder.shouldCancelBecauseOfRunningLaunches(repository,
						null)) {
			return null;
		}
		BasicConfigurationDialog.show(repository);

		String targetRef;
		if (node instanceof RefNode) {
			String refName = ((RefNode) node).getObject().getName();
			try {
				if (refName.equals(repository.getFullBranch()))
					targetRef = null;
				else
					targetRef = refName;
			} catch (IOException e) {
				targetRef = null;
			}
		} else if (node instanceof TagNode) {
			targetRef = ((TagNode) node).getObject().getName();
		} else {
			targetRef = null;
		}
		final String refName;
		final MergeOperation op;

		if (targetRef != null) {
			refName = targetRef;
			op = new MergeOperation(repository, refName);
		} else {
			MergeTargetSelectionDialog mergeTargetSelectionDialog = new MergeTargetSelectionDialog(
					getShell(event), repository);
			if (mergeTargetSelectionDialog.open() != IDialogConstants.OK_ID) {
				return null;
			}
			refName = mergeTargetSelectionDialog.getRefName();
			op = new MergeOperation(repository, refName);
			op.setSquash(mergeTargetSelectionDialog.isMergeSquash());
			op.setFastForwardMode(mergeTargetSelectionDialog
					.getFastForwardMode());
			op.setCommit(mergeTargetSelectionDialog.isCommit());
		}

		MergeActionHandler.doMerge(repository, op, refName);
		return null;
	}

	@Override
	public boolean isEnabled() {
		return selectedRepositoryHasHead();
	}

}

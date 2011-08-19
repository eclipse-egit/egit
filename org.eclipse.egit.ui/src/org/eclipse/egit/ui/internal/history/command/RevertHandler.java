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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.RevertCommitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.BasicConfigurationDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Executes the RevertCommit
 */
public class RevertHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repo = getRepository(event);
		BasicConfigurationDialog.show(repo);
		RevCommit commit = (RevCommit) getSelection(getPage())
				.getFirstElement();

		final Shell shell = getPart(event).getSite().getShell();
		final RevertCommitOperation op = new RevertCommitOperation(repo, commit);

		Job job = new Job(MessageFormat.format(UIText.RevertHandler_JobName,
				commit.name())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.execute(monitor);
					RevCommit newHead = op.getNewHead();
					List<Ref> revertedRefs = op.getRevertedRefs();
					if (newHead != null && revertedRefs.isEmpty())
						showRevertedDialog(shell);
					if (newHead == null)
						Activator
								.showError(UIText.RevertOperation_Failed, null);

				} catch (CoreException e) {
					Activator.handleError(UIText.RevertOperation_InternalError,
							e, true);
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.REVERT_COMMIT.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.setRule(op.getSchedulingRule());
		job.schedule();
		return null;
	}

	private void showRevertedDialog(final Shell shell) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			public void run() {
				MessageDialog.openWarning(shell,
						UIText.RevertHandler_NoRevertTitle,
						UIText.RevertHandler_AlreadyRevertedMessae);
			}
		});
	}
}

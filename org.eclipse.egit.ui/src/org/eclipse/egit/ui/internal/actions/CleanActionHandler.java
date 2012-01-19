/*******************************************************************************
 * Copyright (C) 2011, Chris Aniszczyk <zx@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.CleanOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.clean.CleanProjectsConfirmDialog;
import org.eclipse.egit.ui.internal.clean.CleanResultDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * This operation cleans the repository
 *
 * @see CleanOperation
 */
public class CleanActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		if (resources.length == 0)
			return null;

		final Shell activeShell = HandlerUtil.getActiveShell(event);
		CleanProjectsConfirmDialog dlg = new CleanProjectsConfirmDialog(activeShell);
		dlg.setBlockOnOpen(true);
		dlg.open();

		if(dlg.getReturnCode() == Window.CANCEL)
			return null;

		final CleanOperation op = new CleanOperation(resources, dlg.shouldCleanDirectories(), dlg.shouldDryRun());
		JobUtil.scheduleUserJob(op, "Clean", //$NON-NLS-1$
				JobFamilies.CLEAN, new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						activeShell.getDisplay().syncExec(new Runnable() {
							public void run() {
								CleanResultDialog res = new CleanResultDialog(activeShell, op);
								res.open();
							}
						});
					}
				});

		return null;
	}
}

/*******************************************************************************
 * Copyright (C) 2011, Chris Aniszczyk <zx@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.CleanOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.CleanTreeDialog;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.egit.ui.Activator;

/**
 * This operation cleans the repository
 *
 * @see CleanOperation
 */
public class CleanActionHandler extends RepositoryActionHandler {
	/**
	 *
	 */
	public Repository repo;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		repo = getRepository(true, event);
		if (repo == null)
			return null;

		if (resources.length == 0)
			return null;

		// Do a dry run on CleanCommand to get list of files that would be deleted
		CleanOperation op = new CleanOperation(resources);
		Set<String> fileList = op.dryRun();

		// Get Branch Name
		String currentBranchName;
		try {
			currentBranchName = repo.getBranch();
		} catch (IOException e) {
			Activator
					.handleError(UIText.TagAction_cannotGetBranchName, e, true);
			return null;
		}

		CleanTreeDialog dialog = new CleanTreeDialog(getShell(event), currentBranchName, repo, fileList);

		if (dialog.open() != IDialogConstants.OK_ID)
			return null;

		JobUtil.scheduleUserJob(op, "Clean", //$NON-NLS-1$
				JobFamilies.CLEAN);
		return null;
	}
}

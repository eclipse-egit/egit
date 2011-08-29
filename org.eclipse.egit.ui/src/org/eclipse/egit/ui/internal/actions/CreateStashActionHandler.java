/*******************************************************************************
 * Copyright (C) 2011, Abhishek Bhatnagar <abhatnag@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.op.CreateStashOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jgit.lib.Repository;

/**
 * This operation cleans the repository
 *
 * @see CreateStashOperation
 */
public class CreateStashActionHandler extends RepositoryActionHandler {
	private Repository repo;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		repo = getRepository(true, event);

		// checks
		if (repo == null)
			return null;
		if (resources.length == 0)
			return null;

		// Get author information
		CommitHelper commitHelper = new CommitHelper(repo);
		Pattern p = Pattern.compile("[<>]+"); //$NON-NLS-1$
		String[] author = p.split(commitHelper.getAuthor());
		String[] committer = p.split(commitHelper.getCommitter());
		String commitMessage = commitHelper.getCommitMessage();

		System.out.println(commitHelper.getAuthor());
		System.out.println(commitHelper.getCommitter());
		System.out.println(commitHelper.getPreviousCommit());
		System.out.println(CommitHelper.getHeadCommitInfo(repo));

		CreateStashOperation op = new CreateStashOperation(resources, author, committer, commitMessage);

		// run job
		JobUtil.scheduleUserJob(op, "Create Stash", //$NON-NLS-1$
				JobFamilies.CREATE_STASH);

		// refresh work-tree in package explorer
		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
					IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(
					new org.eclipse.core.runtime.Status(
							IStatus.INFO, Activator.getPluginId(), IStatus.ERROR, e.getMessage(), e
					)
			);
		}

		return null;
	}
}

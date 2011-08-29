/*******************************************************************************
 * Copyright (C) 2011, Abhishek Bhatnagar <abhatnag@redhat.com>
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
import org.eclipse.egit.core.op.CreateStashOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jgit.lib.Repository;

/**
 * This operation cleans the repository
 *
 * @see CreateStashOperation
 */
public class CreateStashActionHandler extends RepositoryActionHandler {
	/**
	 *
	 */
	public Repository repo;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		repo = getRepository(true, event);

		// checks
		if (repo == null)
			return null;
		if (resources.length == 0)
			return null;

		// Do a dry run on CleanCommand to get list of files that would be deleted
		CreateStashOperation op = new CreateStashOperation(resources);

		// run clean job
		JobUtil.scheduleUserJob(op, "Clean", //$NON-NLS-1$
				JobFamilies.CREATE_STASH);

		return null;
	}
}

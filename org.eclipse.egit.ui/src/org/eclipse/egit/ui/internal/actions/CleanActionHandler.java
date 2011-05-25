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
import org.eclipse.egit.core.op.CleanOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.job.JobUtil;

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
		CleanOperation op = new CleanOperation(resources);
		JobUtil.scheduleUserJob(op, "Clean", //$NON-NLS-1$
				JobFamilies.CLEAN);
		return null;
	}
}

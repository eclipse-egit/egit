/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.UntrackOperation;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.job.JobUtil;

/**
 * An action to remove files from a Git repository. The removal does not alter
 * history, only future commits on the same branch will be affected.
 *
 * @see UntrackOperation
 */
public class UntrackActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources();
		if (resources.length == 0)
			return null;
		JobUtil.scheduleUserJob(new UntrackOperation(Arrays.asList(resources)),
				UIText.Untrack_untrack, JobFamilies.UNTRACK);
		return null;
	}

	@Override
	public boolean isEnabled() {
		return !selectionContainsLinkedResources();
	}
}

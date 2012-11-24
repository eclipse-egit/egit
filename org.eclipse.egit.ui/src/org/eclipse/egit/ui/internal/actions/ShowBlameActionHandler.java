/******************************************************************************
 *  Copyright (c) 2011, 2012 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    François Rey - gracefully ignore linked resources
 *****************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Show blame annotations action handler
 */
public class ShowBlameActionHandler extends RepositoryActionHandler {

	/** @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent) */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IResource[] selected = getSelectedResources();
		if (selected.length != 1 || !(selected[0] instanceof IStorage))
			return null;

		Repository repository = getRepository();
		if (repository == null)
			return null;

		RepositoryMapping mapping = RepositoryMapping.getMapping(selected[0]
				.getProject());
		if (mapping == null)
			return null;

		String path = mapping.getRepoRelativePath(selected[0]);
		IStorage storage = (IStorage) selected[0];
		Shell shell = HandlerUtil.getActiveShell(event);
		IWorkbenchPage page = HandlerUtil.getActiveSite(event).getPage();
		JobUtil.scheduleUserJob(new BlameOperation(repository, storage, path,
				null, shell, page), UIText.ShowBlameHandler_JobName,
				JobFamilies.BLAME);
		return null;
	}

	@Override
	public boolean isEnabled() {
		IResource[] selectedResources = getSelectedResources();
		return selectedResources.length == 1 &&
				selectionMapsToSingleRepository();
	}
}

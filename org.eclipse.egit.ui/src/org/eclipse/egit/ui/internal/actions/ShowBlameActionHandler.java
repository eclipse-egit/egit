/******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    François Rey - gracefully ignore linked resources
 *    Thomas Wolf <thomas.wolf@paranor.ch>
 *****************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.storage.CommitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Show blame annotations action handler
 */
public class ShowBlameActionHandler extends RepositoryActionHandler {

	/** @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent) */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.size() != 1) {
			return null;
		}
		Object element = selection.getFirstElement();
		IResource resource = AdapterUtils.adapt(element, IResource.class);
		if (resource instanceof IFile) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			if (mapping != null) {
				String repoRelativePath = mapping.getRepoRelativePath(resource);
				Shell shell = HandlerUtil.getActiveShell(event);
				IWorkbenchPage page = HandlerUtil.getActiveSite(event)
						.getPage();
				JobUtil.scheduleUserJob(
						new BlameOperation(mapping.getRepository(),
								(IFile) resource, repoRelativePath, null, shell,
								page),
						UIText.ShowBlameHandler_JobName, JobFamilies.BLAME);
			}
		} else if (element instanceof CommitFileRevision) {
			Shell shell = HandlerUtil.getActiveShell(event);
			IWorkbenchPage page = HandlerUtil.getActiveSite(event).getPage();
			JobUtil.scheduleUserJob(
					new BlameOperation((CommitFileRevision) element, shell,
							page),
					UIText.ShowBlameHandler_JobName, JobFamilies.BLAME);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		IStructuredSelection selection = getSelection();
		if (selection.size() != 1) {
			return false;
		}
		Object element = selection.getFirstElement();
		IResource resource = AdapterUtils.adapt(element, IResource.class);
		if (resource instanceof IStorage) {
			return RepositoryMapping.getMapping(resource) != null;
		}
		return element instanceof CommitFileRevision;
	}
}

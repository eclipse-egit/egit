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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.storage.CommitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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
		Data data = getData(getSelection(event));

		if (data == null)
			return null;

		Repository repository = data.repository;
		String path = data.repositoryRelativePath;
		IStorage storage = data.storage;
		RevCommit startCommit = data.startCommit;
		Shell shell = HandlerUtil.getActiveShell(event);
		IWorkbenchPage page = HandlerUtil.getActiveSite(event).getPage();
		JobUtil.scheduleUserJob(new BlameOperation(repository, storage, path,
				startCommit, shell, page), UIText.ShowBlameHandler_JobName,
				JobFamilies.BLAME);
		return null;
	}

	@Override
	public boolean isEnabled() {
		IStructuredSelection selection = getSelection();
		return getData(selection) != null;
	}

	private static Data getData(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;

		Object element = selection.getFirstElement();
		if (element instanceof IResource) {
			IResource resource = (IResource) element;

			if (resource instanceof IStorage) {
				IStorage storage = (IStorage) resource;
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resource);

				if (mapping != null) {
					String repoRelativePath = mapping
							.getRepoRelativePath(resource);
					return new Data(mapping.getRepository(), repoRelativePath,
							storage, null);
				}
			}
		} else if (element instanceof CommitFileRevision) {
			CommitFileRevision revision = (CommitFileRevision) element;
			try {
				IStorage storage = revision.getStorage(new NullProgressMonitor());
				return new Data(revision.getRepository(),
						revision.getGitPath(), storage, revision.getRevCommit());
			} catch (CoreException e) {
				// Don't enable
				return null;
			}
		}
		return null;
	}

	private static class Data {
		private final Repository repository;
		private final String repositoryRelativePath;
		private final IStorage storage;
		private final RevCommit startCommit;

		public Data(Repository repository, String repositoryRelativePath,
				IStorage storage, RevCommit startCommit) {
			this.repository = repository;
			this.repositoryRelativePath = repositoryRelativePath;
			this.storage = storage;
			this.startCommit = startCommit;
		}
	}
}

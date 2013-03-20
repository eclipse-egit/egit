/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler to blame a path on a selected commit
 */
public class ShowBlameHandler extends AbstractHistoryCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		GitHistoryPage page = getPage();
		Object input = page.getInputInternal().getSingleItem();
		if (input == null)
			return null;
		Repository repo = getRepository(event);
		if (repo == null)
			return null;
		String path = getPath(repo, page);
		if (path == null)
			return null;
		RevCommit commit = (RevCommit) getSelection(page).getFirstElement();
		if (commit == null)
			return null;

		try {
			IFileRevision revision = CompareUtils.getFileRevision(path, commit,
					repo, null);
			if (revision == null)
				return null;

			IStorage storage = revision.getStorage(new NullProgressMonitor());
			BlameOperation op = new BlameOperation(repo, storage, path, commit,
					HandlerUtil.getActiveShell(event), page.getSite().getPage());
			JobUtil.scheduleUserJob(op, UIText.ShowBlameHandler_JobName,
					JobFamilies.BLAME);
		} catch (IOException e) {
			Activator.showError(UIText.ShowBlameHandler_errorMessage, e);
		} catch (CoreException e) {
			Activator.showError(UIText.ShowBlameHandler_errorMessage, e);
		}
		return null;
	}

	private String getPath(Repository repo, GitHistoryPage page) {
		Object input = page.getInputInternal().getSingleItem();
		if (input == null)
			return null;
		if (input instanceof IFile) {
			IFile file = (IFile) input;
			RepositoryMapping mapping = RepositoryMapping.getMapping(file);
			if (mapping != null)
				return mapping.getRepoRelativePath(file);
		} else if (input instanceof File)
			return getRepoRelativePath(repo, (File) input);
		return null;
	}
}

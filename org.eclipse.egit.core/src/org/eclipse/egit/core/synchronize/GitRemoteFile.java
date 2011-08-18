/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.storage.CommitBlobStorage;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

class GitRemoteFile extends GitRemoteResource {

	private final Repository repo;

	GitRemoteFile(Repository repo, RevCommit commitId, ObjectId objectId,
			String path) {
		super(commitId, objectId, path);
		this.repo = repo;
	}

	public boolean isContainer() {
		return false;
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		CommitBlobStorage content = new CommitBlobStorage(repo, getPath(),
				getObjectId(), getCommitId());
		try {
			setContents(content.getContents(), monitor);
		} catch (CoreException e) {
			Activator.error("", e); //$NON-NLS-1$
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitRemoteFile) {
			GitRemoteFile that = (GitRemoteFile) obj;

			return getPath().equals(that.getPath())
					&& getObjectId().equals(that.getObjectId());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getObjectId().hashCode() ^ getPath().hashCode();
	}

}

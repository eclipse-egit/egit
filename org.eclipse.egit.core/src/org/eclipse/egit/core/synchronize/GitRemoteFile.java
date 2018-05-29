/*******************************************************************************
 * Copyright (C) 2011, 2017 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.storage.CommitBlobStorage;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

class GitRemoteFile extends GitRemoteResource {

	private final Repository repo;

	private final CheckoutMetadata metadata;

	GitRemoteFile(Repository repo, RevCommit commitId, ObjectId objectId,
			String path, CheckoutMetadata metadata) {
		super(commitId, objectId, path);
		this.repo = repo;
		this.metadata = metadata;
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		IStorage content = getStorage(progress.newChild(1));
		try {
			setContents(content.getContents(), progress.newChild(1));
		} catch (CoreException e) {
			Activator.logError("", e); //$NON-NLS-1$
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
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		return new CommitBlobStorage(repo, getCachePath(), getObjectId(),
				getCommitId(), metadata);
	}

	@Override
	public int hashCode() {
		return getObjectId().hashCode() ^ getPath().hashCode();
	}

}

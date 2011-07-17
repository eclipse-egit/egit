/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

class GitRemoteFolder extends GitRemoteResource {

	private final GitSyncObjectCache cachedData;
	private final Repository repo;

	/**
	 *
	 * @param repo
	 * @param cachedData
	 * @param commitId
	 * @param objectId
	 *            given object sha-1 id or {@code null} if this object doesn't
	 *            exist in remote
	 * @param path
	 */
	GitRemoteFolder(Repository repo, GitSyncObjectCache cachedData,
			RevCommit commitId, ObjectId objectId, String path) {
		super(commitId, objectId, path);
		this.repo = repo;
		this.cachedData = cachedData;
	}

	public boolean isContainer() {
		return true;
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		// should be never used on folder
	}

	public boolean equals(Object object) {
		if (object == this)
			return true;

		if (object instanceof GitRemoteFolder) {
			GitRemoteFolder that = (GitRemoteFolder) object;

			return getPath().equals(that.getPath())
					&& getObjectId().equals(that.getObjectId());
		}

		return false;
	}

	public int hashCode() {
		return getObjectId().hashCode() ^ getPath().hashCode();
	}

	GitRemoteResource[] members(IProgressMonitor monitor) {
		List<GitRemoteResource> result = new ArrayList<GitRemoteResource>();

		Collection<GitSyncObjectCache> members = cachedData.members();
		if (members == null || members.size() == 0)
			return new GitRemoteResource[0];

		monitor.beginTask("Fetching members of " + getPath(), cachedData.membersCount()); //$NON-NLS-1$
		try {
			for (GitSyncObjectCache member : members) {
				ThreeWayDiffEntry diffEntry = member.getDiffEntry();
				String memberPath = diffEntry.getPath();

				if (DiffEntry.DEV_NULL.equals(memberPath))
					continue;

				GitRemoteResource obj;
				ObjectId id = diffEntry.getRemoteId().toObjectId();
				if (diffEntry.isTree())
					obj = new GitRemoteFolder(repo, member, getCommitId(), id,
							memberPath);
				else
					obj = new GitRemoteFile(repo, getCommitId(), id, memberPath);

				result.add(obj);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}

		return result.toArray(new GitRemoteResource[result.size()]);
	}

}

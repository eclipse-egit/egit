/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

class GitSyncInfo extends SyncInfo {

	GitSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote, IResourceVariantComparator comparator) {
		super(local, base, remote, comparator);
	}

	@Override
	protected int calculateKind() throws TeamException {
		IResource local = getLocal();
		IResourceVariantComparator comparator = getComparator();
		GitResourceVariant base = (GitResourceVariant) getBase();
		GitResourceVariant remote = (GitResourceVariant) getRemote();

		int description = IN_SYNC;

		boolean localExists = local.exists();

		if (Team.isIgnoredHint(local)) {
			// assume that ignored and transient files are always synchronized
			// in sync
		} else if (base == null) {
			description = hadnleNullBase(local, comparator, remote);
		} else {
			if (!localExists) {
				description = handleLocalDoesntExist(comparator, base, remote);
			} else {
				if (remote == null) {
					if (comparator.compare(local, base))
						description = INCOMING | DELETION;
					else
						description = CONFLICTING | CHANGE;
				} else {
					description = calculateDescrBasedOnGitData(local, base,
							remote);
				}
			}
		}

		return description;
	}

	private int hadnleNullBase(IResource local,
			IResourceVariantComparator comparator, GitResourceVariant remote) {
		int description;
		boolean localExists = local.exists();

		if (remote == null) {
			if (!localExists) {
				description = IN_SYNC;
			} else {
				description = OUTGOING | ADDITION;
			}
		} else {
			if (!localExists) {
				description = INCOMING | ADDITION;
			} else {
				description = CONFLICTING | ADDITION;
				if (comparator.compare(local, remote)) {
					description |= PSEUDO_CONFLICT;
				}
			}
		}

		return description;
	}

	private int handleLocalDoesntExist(IResourceVariantComparator comparator,
			GitResourceVariant base, GitResourceVariant remote) {
		int description;

		if (remote == null)
			description = CONFLICTING | DELETION | PSEUDO_CONFLICT;
		else if (comparator.compare(base, remote))
			description = OUTGOING | DELETION;
		else
			description = CONFLICTING | CHANGE;

		return description;
	}

	private int calculateDescrBasedOnGitData(IResource local,
			GitResourceVariant base, GitResourceVariant remote) throws TeamException {
		int description = IN_SYNC;

		if (!base.isContainer()) {
			if (!remote.isContainer()) {
				GitBlobResourceVariant baseBlob = (GitBlobResourceVariant) base;
				GitBlobResourceVariant remoteBlob = (GitBlobResourceVariant) remote;

				ObjectId baseObjectId = baseBlob.getObjectId();
				ObjectId remoteObjectId = remoteBlob.getObjectId();
				if (getComparator().compare(local, base)) {
					description = calculateDescBasedOnGitCommits(baseBlob,
							remoteBlob);
				} else if (baseObjectId.equals(remoteObjectId)) {
					description = OUTGOING | CHANGE;
				} else {
					description = CONFLICTING | CHANGE;
				}
			} else {
				// file in local, folder on remote branch
				description = CONFLICTING | CHANGE;
			}
		} else {
			if (remote.isContainer())
				description = compareTwoContainers(base, remote);
			else
				// folder in local, file in remote branch
				description = CONFLICTING | CHANGE;
		}

		return description;
	}

	private int calculateDescBasedOnGitCommits(GitBlobResourceVariant baseBlob,
			GitBlobResourceVariant remoteBlob) throws TeamException {
		ObjectId baseId = baseBlob.getObjectId();
		ObjectId remoteId = remoteBlob.getObjectId();

		if (baseId.equals(remoteId))
			return IN_SYNC;
		else {
			RevCommit remoteCommit = remoteBlob.getRevCommit();
			RevCommit baseCommit = baseBlob.getRevCommit();

			if (findCommit(baseBlob.getRepository(), baseCommit, remoteCommit))
				return OUTGOING | CHANGE;
			else if (findCommit(baseBlob.getRepository(), remoteCommit, baseCommit))
				return INCOMING | CHANGE;

			return CONFLICTING | CHANGE;
		}
	}

	private int compareTwoContainers(GitResourceVariant base,
			GitResourceVariant remote) {
		final int description;
		boolean baseExists = base.exists();
		boolean remoteExists = remote.exists();
		// IMPORTANT: remember that local resource exist

		if (baseExists && remoteExists)
			description = IN_SYNC;
		else if (!baseExists && !remoteExists)
			description = OUTGOING | ADDITION;
		else
			description = CONFLICTING | CHANGE;

		return description;
	}

	private boolean findCommit(Repository repo, RevCommit startCommit, RevCommit commitToBeFound)
			throws TeamException {
		RevWalk rw = new RevWalk(repo);
		rw.reset();
		rw.setRevFilter(new RevCommitFilter(commitToBeFound));

		try {
			rw.markStart(startCommit);

			return rw.next() != null;
		} catch (IOException e) {
			throw new TeamException(e.getMessage(), e);
		}

	}

	private final class RevCommitFilter extends RevFilter {

		private final String commitId;

		public RevCommitFilter(RevCommit revCommit) {
			commitId = revCommit.getId().getName();
		}

		@Override
		public boolean include(RevWalk walker, RevCommit cmit)
				throws StopWalkException, MissingObjectException,
				IncorrectObjectTypeException, IOException {
			return cmit.getId().getName().equals(commitId);
		}

		@Override
		public RevFilter clone() {
			return this;
		}
	}

}

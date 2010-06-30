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

import org.eclipse.core.resources.IResource;
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

		if (remote == null) {
			description = CONFLICTING | DELETION | PSEUDO_CONFLICT;
		} else {
			if (comparator.compare(base, remote))
				description = OUTGOING | DELETION;
			else
				description = CONFLICTING | CHANGE;
		}

		return description;
	}

	private int calculateDescrBasedOnGitData(IResource local,
			GitResourceVariant base, GitResourceVariant remote) {
		int description = IN_SYNC;

		if (base instanceof GitBlobResourceVariant) {
			if (remote instanceof GitBlobResourceVariant) {
				if (getComparator().compare(local, base)) {
//					GitBlobResourceVariant baseBlob = (GitBlobResourceVariant) base;
//					GitBlobResourceVariant remoteBlob = (GitBlobResourceVariant) remote;
//
//					if (!baseBlob.getId().equals(remoteBlob.getId())) {
//						description = calculateDescBasedOnGitCommits(baseBlob,
//								remoteBlob);
//					}
					if (!base.getContentIdentifier().equals(remote.getContentIdentifier())) {
						description = CONFLICTING | CHANGE;
					}
				} else {
					description = OUTGOING | CHANGE;
				}
			} else {
				// file in local, folder on remote branch
				description = CONFLICTING | CHANGE;
			}
		} else if (base.isContainer()) {
			if (remote.isContainer()) {
				description = compareTwoContainers(base, remote);
			} else {
				// folder in local, file in remote branch
				description = CONFLICTING | CHANGE;
			}
		}

		return description;
	}

//	private int calculateDescBasedOnGitCommits(GitBlobResourceVariant baseBlob,
//			GitBlobResourceVariant remoteBlob) {
//
//		RevCommitList<RevCommit> baseList = baseBlob.getCommitList();
//		RevCommitList<RevCommit> remoteList = remoteBlob.getCommitList();
//		RevCommit recentRemoteCommit = remoteList.get(0);
//		RevCommit recentBaseCommit = baseList.get(0);
//
//		// TODO can we implement it better ?
//		for (RevCommit baseCommit : baseList) {
//			if (recentRemoteCommit.name().equals(baseCommit.name())) {
//				return OUTGOING | CHANGE;
//			}
//		}
//
//		// TODO can we implement it better ?
//		for (RevCommit remoteCommit : remoteList) {
//			if (recentBaseCommit.name().equals(remoteCommit.name())) {
//				return INCOMING | CHANGE;
//			}
//		}
//
//		return CONFLICTING | CHANGE;
//	}

	private int compareTwoContainers(GitResourceVariant base,
			GitResourceVariant remote) {
		final int description;
		boolean baseExists = base.getResource().exists();
		boolean remoteExists = remote.getResource().exists();

		if (baseExists && remoteExists) {
			// there are no changes
			description = IN_SYNC;
		} else if (baseExists && remoteExists) {
			// folder doesn't exist locally but it exists in
			// common accessor and remote
			description = INCOMING | ADDITION;
		} else {
			// in all other cases
			description = CONFLICTING | CHANGE;
		}

		return description;
	}

}

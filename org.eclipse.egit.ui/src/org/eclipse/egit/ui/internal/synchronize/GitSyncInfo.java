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
package org.eclipse.egit.ui.internal.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevCommitList;
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
		GitResourceVariant base = (GitResourceVariant) getBase();
		GitResourceVariant remote = (GitResourceVariant) getRemote();

		if (Team.isIgnoredHint(local)) {
			// assume that ignored files are always synchronized
			return IN_SYNC;
		} else if (base instanceof GitBlobResourceVariant) {
			if (remote instanceof GitBlobResourceVariant) {
				if (local.exists()) {
					if (getComparator().compare(local, base)) {
						GitBlobResourceVariant baseBlob = (GitBlobResourceVariant) base;
						GitBlobResourceVariant remoteBlob = (GitBlobResourceVariant) remote;

						if (baseBlob.getId().equals(remoteBlob.getId())) {
							// ids are identical, the resources are in sync
							return IN_SYNC;
						}

						RevCommitList<RevCommit> baseList = baseBlob
								.getCommitList();
						RevCommitList<RevCommit> remoteList = remoteBlob
								.getCommitList();

						RevCommit recentRemoteCommit = remoteList.get(0);
						RevCommit recentBaseCommit = baseList.get(0);

						for (RevCommit baseCommit : baseList) {
							if (recentRemoteCommit.name().equals(
									baseCommit.name())) {
								return OUTGOING | CHANGE;
							}
						}

						for (RevCommit remoteCommit : remoteList) {
							if (recentBaseCommit.name().equals(
									remoteCommit.name())) {
								return INCOMING | CHANGE;
							}
						}

						return CONFLICTING | CHANGE;
					} else {
						return OUTGOING | CHANGE;
					}
				}
			} else if (remote == null) {
				// file doesn't exist in the remote branch
				if (getComparator().compare(local, base)) {
					return OUTGOING | ADDITION;
				} else {
					return OUTGOING | CHANGE;
				}
			} else {
				// file in local, folder on remote branch
				return CONFLICTING | CHANGE;
			}
		} else if (base instanceof GitFolderResourceVariant) {
			if (remote instanceof GitFolderResourceVariant) {
				if (!base.getResource().exists()) {
					// folder doesn't exist locally, addition on the branch
					return INCOMING | ADDITION;
				}
			} else if (remote == null) {
				// folder doesn't exist remotely
				return OUTGOING | ADDITION;
			} else {
				// folder in local, file in remote branch
				return CONFLICTING | CHANGE;
			}
		}

		return super.calculateKind();
	}
}

/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.synchronize.ThreeWayDiffEntry.ChangeType;
import org.eclipse.egit.core.synchronize.ThreeWayDiffEntry.Direction;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

class GitSyncInfo extends SyncInfo {

	private final GitSyncObjectCache cache;
	private final Repository repo;

	public GitSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote, IResourceVariantComparator comparator,
			GitSyncObjectCache cache, Repository repo) {
		super(local, base, remote, comparator);
		this.repo = repo;
		this.cache = cache;
	}

	@Override
	protected int calculateKind() throws TeamException {
		if (cache.membersCount() == 0)
			return IN_SYNC;

		String path;
		if (getLocal() != null && getLocal().getLocation() != null)
			path = stripWorkDir(repo.getWorkTree(), getLocal().getLocation().toFile());
		else if (getRemote() != null)
			path = ((GitRemoteResource)getRemote()).getPath();
		else if (getBase() != null)
			path = ((GitRemoteResource)getBase()).getPath();
		else
			return IN_SYNC;

		GitSyncObjectCache obj = cache.get(path);
		if (obj == null)
			return IN_SYNC;

		if (obj.getDiffEntry().isTree()) {
			// Folder state is not important for synchronization, and the state
			// recorded in GitSyncObjCache is bogus anyway.
			return IN_SYNC;
		}
		int direction;
		Direction gitDirection = obj.getDiffEntry().getDirection();
		if (gitDirection == Direction.INCOMING)
			direction = INCOMING;
		else if (gitDirection == Direction.OUTGOING)
			direction = OUTGOING;
		else
			direction = CONFLICTING;

		ChangeType changeType = obj.getDiffEntry().getChangeType();

		if (changeType == ChangeType.MODIFY)
			return direction | CHANGE;
		if (changeType == ChangeType.ADD)
			return direction | ADDITION;
		if (changeType == ChangeType.DELETE)
			return direction | DELETION;

		return IN_SYNC;
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}

/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

class GitSyncInfo extends SyncInfo {

	private final GitSynchronizeData gsd;

	public GitSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote, IResourceVariantComparator comparator,
			GitSynchronizeData gsd) {
		super(local, base, remote, comparator);
		this.gsd = gsd;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;

		if (other instanceof GitSyncInfo) {
			GitSynchronizeData otherGsd = ((GitSyncInfo) other).gsd;
			boolean equalGsd = gsd.getProjects().equals(otherGsd.getProjects())
					&& gsd.getRepository().equals(otherGsd.getRepository())
					&& gsd.getDstRevCommit().equals(otherGsd.getDstRevCommit())
					&& gsd.getSrcRevCommit().equals(otherGsd.getSrcRevCommit());

			// check quality of local, base and remote using super.equals()
			return super.equals(other) && equalGsd;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return gsd.getSrcRevCommit().hashCode() ^ getLocal().hashCode();
	}

	@Override
	protected int calculateKind() throws TeamException {
		String localPath;
		Repository repo = gsd.getRepository();
		if (getLocal().exists()) {
			File local = getLocal().getLocation().toFile();
			localPath = Repository.stripWorkDir(repo.getWorkTree(), local);
		} else if (getRemote() != null)
			localPath = ((GitRemoteResource) getRemote()).getCachePath();
		else if (getBase() != null)
			localPath = ((GitRemoteResource) getBase()).getCachePath();
		else
			// we cannot determinate local path therefore we cannot set proper
			// value for PathFilter, so we use standard calulateKind()
			// implementation
			return super.calculateKind();

		if (localPath.length() == 0)
			return IN_SYNC;

		TreeWalk tw = new TreeWalk(repo);
		tw.setFilter(AndTreeFilter.create(TreeFilter.ANY_DIFF,
				PathFilter.create(localPath)));
		tw.setRecursive(true);

		try {
			int srcNth = tw.addTree(gsd.getSrcRevCommit().getTree());
			int dstNth = tw.addTree(gsd.getDstRevCommit().getTree());

			if (tw.next()) {
				return calculateKindImpl(repo, tw, srcNth, dstNth);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return IN_SYNC;
	}

	private int calculateKindImpl(Repository repo, TreeWalk tw, int srcNth,
			int dstNth) throws IOException {
		ObjectId srcId = tw.getObjectId(srcNth);
		ObjectId dstId = tw.getObjectId(dstNth);

		if (srcId.equals(zeroId()))
			return INCOMING | ADDITION;
		if (dstId.equals(zeroId()))
			return OUTGOING | ADDITION;
		if (!srcId.equals(dstId)) {
			RevWalk rw = new RevWalk(repo);
			RevFlag srcFlag = rw.newFlag("source"); //$NON-NLS-1$
			RevFlag dstFlag = rw.newFlag("destination"); //$NON-NLS-1$
			initializeRevWalk(rw, srcFlag, dstFlag);

			RevCommit commit = rw.next();
			if (commit.has(srcFlag))
				return OUTGOING | CHANGE;
			else if (commit.has(dstFlag))
				return INCOMING | CHANGE;
			else
				return CONFLICTING | CHANGE;
		}

		return IN_SYNC;
	}

	private void initializeRevWalk(RevWalk rw, RevFlag srcFlag, RevFlag dstFlag)
			throws IOException {
		RevCommit srcCommit = rw.parseCommit(gsd.getSrcRevCommit());
		srcCommit.add(srcFlag);

		RevCommit dstCommit = rw.parseCommit(gsd.getDstRevCommit());
		dstCommit.add(dstFlag);

		rw.markStart(srcCommit);
		rw.markStart(dstCommit);

		rw.carry(srcFlag);
		rw.carry(dstFlag);
	}

}

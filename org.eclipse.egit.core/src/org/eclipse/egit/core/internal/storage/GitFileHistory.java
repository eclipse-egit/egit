/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevCommitList;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * A list of revisions for a specific resource according to some filtering
 * criterion. Though git really does not do file tracking, this corresponds to
 * listing all files with the same path.
 */
class GitFileHistory extends FileHistory implements IAdaptable {
	private static final int SINGLE_REVISION = IFileHistoryProvider.SINGLE_REVISION;

	private static final IFileRevision[] NO_REVISIONS = {};

	private static final int BATCH_SIZE = 256;

	private final IResource resource;

	private String gitPath;

	private final Repository db;

	private final RevWalk walk;

	private final IFileRevision[] revisions;

	GitFileHistory(final IResource rsrc, final int flags,
			final IProgressMonitor monitor) {
		resource = rsrc;

		final RepositoryMapping rm = RepositoryMapping.getMapping(resource);
		if (rm == null) {
			Activator.logError(NLS.bind(CoreText.GitFileHistory_gitNotAttached,
					resource.getProject().getName()), null);
			db = null;
			walk = null;
		} else {
			db = rm.getRepository();
			walk = new RevWalk(db);
			gitPath = rm.getRepoRelativePath(resource);
			if (gitPath == null || gitPath.length() == 0) {
				walk.setTreeFilter(TreeFilter.ANY_DIFF);
			} else {
				walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup
						.createFromStrings(Collections.singleton(gitPath)),
						TreeFilter.ANY_DIFF));
			}
		}

		revisions = buildRevisions(monitor, flags);
	}

	private IFileRevision[] buildRevisions(final IProgressMonitor monitor,
			final int flags) {
		if (walk == null)
			return NO_REVISIONS;

		final RevCommit root;
		try {
			final AnyObjectId headId = db.resolve(Constants.HEAD);
			if (headId == null) {
				Activator.logError(NLS.bind(
						CoreText.GitFileHistory_noHeadRevisionAvailable,
						resource.getProject().getName()), null);
				return NO_REVISIONS;
			}

			root = walk.parseCommit(headId);
			if ((flags & SINGLE_REVISION) == SINGLE_REVISION) {
				// If all Eclipse wants is one revision it probably is
				// for the editor "quick diff" feature. We can pass off
				// just the repository HEAD, even though it may not be
				// the revision that most recently modified the path.
				//
				final CommitFileRevision single;
				single = new CommitFileRevision(db, root, gitPath);
				return new IFileRevision[] { single };
			}

			markStartAllRefs(walk, Constants.R_HEADS);
			markStartAllRefs(walk, Constants.R_REMOTES);
			markStartAllRefs(walk, Constants.R_TAGS);

			walk.markStart(root);
		} catch (IOException e) {
			Activator.logError(NLS.bind(
					CoreText.GitFileHistory_invalidHeadRevision, resource
							.getProject().getName()), e);
			return NO_REVISIONS;
		}

		final RevCommitList<RevCommit> list = new RevCommitList<RevCommit>();
		list.source(walk);
		try {
			for (;;) {
				final int oldsz = list.size();
				list.fillTo(oldsz + BATCH_SIZE - 1);
				if (oldsz == list.size())
					break;
				if (monitor != null && monitor.isCanceled())
					break;
			}
		} catch (IOException e) {
			Activator.logError(NLS.bind(
					CoreText.GitFileHistory_errorParsingHistory, resource
							.getFullPath()), e);
			return NO_REVISIONS;
		}

		final IFileRevision[] r = new IFileRevision[list.size()];
		for (int i = 0; i < r.length; i++)
			r[i] = new CommitFileRevision(db, list.get(i), gitPath);
		return r;
	}

	private void markStartAllRefs(RevWalk theWalk, String prefix)
			throws IOException, MissingObjectException,
			IncorrectObjectTypeException {
		for (Entry<String, Ref> refEntry : db.getRefDatabase().getRefs(prefix)
				.entrySet()) {
			Ref ref = refEntry.getValue();
			if (ref.isSymbolic())
				continue;
			markStartRef(theWalk, ref);
		}
	}

	private void markStartRef(RevWalk theWalk, Ref ref) throws IOException,
			IncorrectObjectTypeException {
		try {
			Object refTarget = theWalk.parseAny(ref.getLeaf().getObjectId());
			if (refTarget instanceof RevCommit)
				theWalk.markStart((RevCommit) refTarget);
		} catch (MissingObjectException e) {
			// If there is a ref which points to Nirvana then we should simply
			// ignore this ref. We should not let a corrupt ref cause that the
			// history view is not filled at all
		}
	}

	public IFileRevision[] getContributors(final IFileRevision ifr) {
		if (!(ifr instanceof CommitFileRevision))
			return NO_REVISIONS;

		final CommitFileRevision rev = (CommitFileRevision) ifr;
		final String p = rev.getGitPath();
		final RevCommit c = rev.getRevCommit();
		final IFileRevision[] r = new IFileRevision[c.getParentCount()];
		for (int i = 0; i < r.length; i++)
			r[i] = new CommitFileRevision(db, c.getParent(i), p);
		return r;
	}

	public IFileRevision[] getTargets(final IFileRevision ifr) {
		final IFileRevision[] allRevisions = getFileRevisions();
		final List<IFileRevision> descendants = new ArrayList<IFileRevision>();
		if (!(ifr instanceof CommitFileRevision)) {
			for (IFileRevision candidate : allRevisions) {
				if (candidate.getTimestamp() < ifr.getTimestamp())
					descendants.add(candidate);
			}
		} else {
			final CommitFileRevision rev = (CommitFileRevision) ifr;
			final RevCommit rc = rev.getRevCommit();

			for (IFileRevision candidate : allRevisions) {
				if (candidate instanceof CommitFileRevision) {
					final RevCommit candidateCommit = ((CommitFileRevision) candidate)
							.getRevCommit();
					if (candidateCommit.getCommitTime() < rc.getCommitTime())
						descendants.add(candidate);
				}
			}
		}
		return descendants.toArray(new IFileRevision[descendants.size()]);
	}

	public IFileRevision getFileRevision(final String id) {
		if (id == null || id.equals("") //$NON-NLS-1$
				|| GitFileRevision.WORKSPACE.equals(id))
			return new WorkspaceFileRevision(resource);
		if (GitFileRevision.INDEX.equals(id))
			return new IndexFileRevision(db, gitPath);

		// Only return a revision if it was matched by this filtered history
		for (IFileRevision r : revisions) {
			if (r.getContentIdentifier().equals(id))
				return r;
		}
		return null;
	}

	public IFileRevision[] getFileRevisions() {
		final IFileRevision[] r = new IFileRevision[revisions.length];
		System.arraycopy(revisions, 0, r, 0, r.length);
		return r;
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
}

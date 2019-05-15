/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2013, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.GitRemoteResource;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import org.eclipse.team.core.variants.IResourceVariant;

/**
 * A list of revisions for a specific resource according to some filtering
 * criterion. Though git really does not do file tracking, this corresponds to
 * listing all files with the same path.
 */
class GitFileHistory extends FileHistory implements IAdaptable {
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
			IProject project = resource.getProject();
			String projectName = project != null ? project.getName() : ""; //$NON-NLS-1$
			Activator.logError(NLS.bind(CoreText.GitFileHistory_gitNotAttached,
					projectName), null);
			db = null;
			walk = null;
		} else {
			db = rm.getRepository();
			walk = new KidWalk(db);
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
				IProject project = resource.getProject();
				String projectName = project != null? project.getName() : ""; //$NON-NLS-1$
				Activator.logError(NLS.bind(
						CoreText.GitFileHistory_noHeadRevisionAvailable,
						projectName), null);
				return NO_REVISIONS;
			}

			root = walk.parseCommit(headId);
			if ((flags & IFileHistoryProvider.SINGLE_REVISION) != 0) {
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
			IProject project = resource.getProject();
			String projectName = project != null? project.getName() : ""; //$NON-NLS-1$
			Activator.logError(NLS.bind(
					CoreText.GitFileHistory_invalidHeadRevision, projectName), e);
			return NO_REVISIONS;
		}

		final KidCommitList list = new KidCommitList();
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
		for (Ref ref : db.getRefDatabase().getRefsByPrefix(prefix)) {
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

	@Override
	public IFileRevision[] getContributors(final IFileRevision ifr) {
		String path = getGitPath(ifr);
		RevCommit commit = getRevCommit(ifr);

		if (path != null && commit != null) {
			final IFileRevision[] r = new IFileRevision[commit.getParentCount()];
			for (int i = 0; i < r.length; i++)
				r[i] = new CommitFileRevision(db, commit.getParent(i), path);
			return r;
		}

		return NO_REVISIONS;
	}

	@Override
	public IFileRevision[] getTargets(final IFileRevision ifr) {
		String path = getGitPath(ifr);
		RevCommit commit = getRevCommit(ifr);

		if (path != null && commit instanceof KidCommit) {
			final KidCommit c = (KidCommit) commit;
			final IFileRevision[] r = new IFileRevision[c.children.length];
			for (int i = 0; i < r.length; i++)
				r[i] = new CommitFileRevision(db, c.children[i], path);
			return r;
		}

		return NO_REVISIONS;
	}

	private String getGitPath(IFileRevision revision) {
		if (revision instanceof CommitFileRevision)
			return ((CommitFileRevision) revision).getGitPath();
		else if (revision instanceof IAdaptable) {
			IResourceVariant variant = Adapters.adapt(((IAdaptable) revision),
					IResourceVariant.class);

			if (variant instanceof GitRemoteResource)
				return ((GitRemoteResource) variant).getPath();
		}

		return null;
	}

	private RevCommit getRevCommit(IFileRevision revision) {
		if (revision instanceof CommitFileRevision)
			return ((CommitFileRevision) revision).getRevCommit();
		else if (revision instanceof IAdaptable) {
			IResourceVariant variant = Adapters.adapt(((IAdaptable) revision),
					IResourceVariant.class);
			if (variant instanceof GitRemoteResource) {
				final RevCommit commit = ((GitRemoteResource) variant)
						.getCommitId();
				try {
					return walk.parseCommit(commit);
				} catch (IOException e) {
					Activator.logError(NLS.bind(
							CoreText.GitFileHistory_invalidCommit,
							commit.getName(), resource.getName()), e);
				}
			}
		}

		return null;
	}

	@Override
	public IFileRevision getFileRevision(final String id) {
		if (id == null || id.isEmpty()
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

	@Override
	public IFileRevision[] getFileRevisions() {
		final IFileRevision[] r = new IFileRevision[revisions.length];
		System.arraycopy(revisions, 0, r, 0, r.length);
		return r;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}

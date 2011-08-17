/*******************************************************************************
 * Copyright (C) 2008, 2009 Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.text.Document;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;

class GitDocument extends Document implements RefsChangedListener {
	private final IResource resource;

	private ObjectId lastCommit;

	private ObjectId lastTree;

	private ObjectId lastBlob;

	private ListenerHandle myRefsChangedHandle;

	static Map<GitDocument, Repository> doc2repo = new WeakHashMap<GitDocument, Repository>();

	static GitDocument create(final IResource resource) throws IOException {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.QUICKDIFF.getLocation(),
					"(GitDocument) create: " + resource); //$NON-NLS-1$
		GitDocument ret = null;
		if (RepositoryProvider.getProvider(resource.getProject()) instanceof GitProvider) {
			ret = new GitDocument(resource);
			ret.populate();
			final Repository repository = ret.getRepository();
			if (repository != null)
				ret.myRefsChangedHandle = repository.getListenerList()
						.addRefsChangedListener(ret);
		}
		return ret;
	}

	private GitDocument(IResource resource) {
		this.resource = resource;
		GitDocument.doc2repo.put(this, getRepository());
	}

	private void setResolved(final AnyObjectId commit, final AnyObjectId tree,
			final AnyObjectId blob, final String value) {
		lastCommit = commit != null ? commit.copy() : null;
		lastTree = tree != null ? tree.copy() : null;
		lastBlob = blob != null ? blob.copy() : null;
		set(value);
		if (blob != null)
			if (GitTraceLocation.QUICKDIFF.isActive())
				GitTraceLocation
						.getTrace()
						.trace(
								GitTraceLocation.QUICKDIFF.getLocation(),
								"(GitDocument) resolved " + resource + " to " + lastBlob + " in " + lastCommit + "/" + lastTree); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			else if (GitTraceLocation.QUICKDIFF.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.QUICKDIFF.getLocation(),
						"(GitDocument) unresolved " + resource); //$NON-NLS-1$
	}

	void populate() throws IOException {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.QUICKDIFF.getLocation(), resource);
		TreeWalk tw = null;
		RevWalk rw = null;
		try {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			if (mapping == null) {
				setResolved(null, null, null, ""); //$NON-NLS-1$
				return;
			}
			final String gitPath = mapping.getRepoRelativePath(resource);
			final Repository repository = mapping.getRepository();
			String baseline = GitQuickDiffProvider.baseline.get(repository);
			if (baseline == null)
				baseline = Constants.HEAD;
			ObjectId commitId = repository.resolve(baseline);
			if (commitId != null) {
				if (commitId.equals(lastCommit)) {
					if (GitTraceLocation.QUICKDIFF.isActive())
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.QUICKDIFF.getLocation(),
								"(GitDocument) already resolved"); //$NON-NLS-1$
					return;
				}
			} else {
				String msg = NLS.bind(UIText.GitDocument_errorResolveQuickdiff,
						new Object[] { baseline, resource, repository });
				Activator.logError(msg, new Throwable());
				setResolved(null, null, null, ""); //$NON-NLS-1$
				return;
			}
			rw = new RevWalk(repository);
			RevCommit baselineCommit;
			try {
				baselineCommit = rw.parseCommit(commitId);
			} catch (IOException err) {
				String msg = NLS
						.bind(UIText.GitDocument_errorLoadCommit, new Object[] {
								commitId, baseline, resource, repository });
				Activator.logError(msg, err);
				setResolved(null, null, null, ""); //$NON-NLS-1$
				return;
			}
			RevTree treeId = baselineCommit.getTree();
			if (treeId.equals(lastTree)) {
				if (GitTraceLocation.QUICKDIFF.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.QUICKDIFF.getLocation(),
							"(GitDocument) already resolved"); //$NON-NLS-1$
				return;
			}

			tw = TreeWalk.forPath(repository, gitPath, treeId);
			if (tw == null) {
				if (GitTraceLocation.QUICKDIFF.isActive())
					GitTraceLocation
							.getTrace()
							.trace(
									GitTraceLocation.QUICKDIFF.getLocation(),
									"(GitDocument) resource " + resource + " not found in " + treeId + " in " + repository + ", baseline=" + baseline); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				setResolved(null, null, null, ""); //$NON-NLS-1$
				return;
			}
			ObjectId id = tw.getObjectId(0);
			if (id.equals(ObjectId.zeroId())) {
				setResolved(null, null, null, ""); //$NON-NLS-1$
				String msg = NLS
						.bind(UIText.GitDocument_errorLoadTree, new Object[] {
								treeId.getName(), baseline, resource, repository });
				Activator.logError(msg, new Throwable());
				setResolved(null, null, null, ""); //$NON-NLS-1$
				return;
			}
			if (!id.equals(lastBlob)) {
				if (GitTraceLocation.QUICKDIFF.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.QUICKDIFF.getLocation(),
							"(GitDocument) compareTo: " + baseline); //$NON-NLS-1$
				ObjectLoader loader = repository.open(id, Constants.OBJ_BLOB);
				byte[] bytes = loader.getBytes();
				String charset;
				charset = CompareCoreUtils.getResourceEncoding(resource);
				// Finally we could consider validating the content with respect
				// to the content. We don't do that here.
				String s = new String(bytes, charset);
				setResolved(commitId, treeId, id, s);
				if (GitTraceLocation.QUICKDIFF.isActive())
					GitTraceLocation
							.getTrace()
							.trace(GitTraceLocation.QUICKDIFF.getLocation(),
									"(GitDocument) has reference doc, size=" + s.length() + " bytes"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				if (GitTraceLocation.QUICKDIFF.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.QUICKDIFF.getLocation(),
							"(GitDocument) already resolved"); //$NON-NLS-1$
			}
		} finally {
			if (tw != null)
				tw.release();
			if (rw != null)
				rw.release();
			if (GitTraceLocation.QUICKDIFF.isActive())
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.QUICKDIFF.getLocation());
		}

	}

	void dispose() {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.QUICKDIFF.getLocation(),
					"(GitDocument) dispose: " + resource); //$NON-NLS-1$
		doc2repo.remove(this);

		if (myRefsChangedHandle != null) {
			myRefsChangedHandle.remove();
			myRefsChangedHandle = null;
		}
	}

	public void onRefsChanged(final RefsChangedEvent e) {
		try {
			populate();
		} catch (IOException e1) {
			Activator.logError(UIText.GitDocument_errorRefreshQuickdiff, e1);
		}
	}

	private Repository getRepository() {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		return (mapping != null) ? mapping.getRepository() : null;
	}

	/**
	 * A change occurred to a repository. Update any GitDocument instances
	 * referring to such repositories.
	 *
	 * @param repository
	 *            Repository which changed
	 * @throws IOException
	 */
	static void refreshRelevant(final Repository repository) throws IOException {
		for (Map.Entry<GitDocument, Repository> i : doc2repo.entrySet()) {
			if (i.getValue() == repository) {
				i.getKey().populate();
			}
		}
	}
}

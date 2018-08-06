/*******************************************************************************
 * Copyright (C) 2008, 2009 Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.text.Document;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffConfig.RenameDetectionType;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.LfsFactory;
import org.eclipse.osgi.util.NLS;

class GitDocument extends Document implements RefsChangedListener {
	private final IResource resource;

	private ObjectId lastCommit;

	private ObjectId lastTree;

	private ObjectId lastBlob;

	private ListenerHandle myRefsChangedHandle;

	// Job that reloads the document when something has changed
	private Job reloadJob;

	private boolean disposed;

	static Map<GitDocument, Repository> doc2repo = new WeakHashMap<>();

	static GitDocument create(final IResource resource) throws IOException {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.QUICKDIFF.getLocation(),
					"(GitDocument) create: " + resource); //$NON-NLS-1$
		GitDocument ret = null;
		if (ResourceUtil.isSharedWithGit(resource.getProject())) {
			ret = new GitDocument(resource);
			ret.populate();
			final Repository repository = ret.getRepository();
			if (repository != null) {
				ret.myRefsChangedHandle = repository.getListenerList()
						.addRefsChangedListener(ret);
			}
		}
		return ret;
	}

	private GitDocument(IResource resource) {
		this.resource = resource;
		synchronized (doc2repo) {
			doc2repo.put(this, getRepository());
		}
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

		// Do not populate if already disposed
		if (disposed)
			return;

		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		if (mapping == null) {
			setResolved(null, null, null, ""); //$NON-NLS-1$
			return;
		}
		final String gitPath = mapping.getRepoRelativePath(resource);
		if (gitPath == null) {
			setResolved(null, null, null, ""); //$NON-NLS-1$
			return;
		}
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
			if (repository.exactRef(Constants.HEAD) == null) {
				// Complain only if not an unborn branch
				String msg = NLS.bind(UIText.GitDocument_errorResolveQuickdiff,
						new Object[] { baseline, resource, repository });
				Activator.logError(msg, new Throwable());
			}
			setResolved(null, null, null, ""); //$NON-NLS-1$
			return;
		}

		RevCommit baselineCommit;
		String oldPath = gitPath;

		try (RevWalk rw = new RevWalk(repository);
				ObjectReader reader = repository.newObjectReader()) {
			baselineCommit = rw.parseCommit(commitId);
			DiffConfig diffConfig = repository.getConfig().get(DiffConfig.KEY);
			if (diffConfig.getRenameDetectionType() != RenameDetectionType.FALSE) {
				TreeWalk walk = new TreeWalk(repository);
				CanonicalTreeParser baseLineIterator = new CanonicalTreeParser();
				baseLineIterator.reset(reader, baselineCommit.getTree());
				walk.addTree(baseLineIterator);
				walk.addTree(new DirCacheIterator(repository.readDirCache()));
				List<DiffEntry> diffs = DiffEntry.scan(walk, true);
				RenameDetector renameDetector = new RenameDetector(repository);
				renameDetector.addAll(diffs);
				List<DiffEntry> renames = renameDetector.compute();
				for (DiffEntry e : renames) {
					if (e.getNewPath().equals(gitPath)) {
						oldPath = e.getOldPath();
						break;
					}
				}
			}
		} catch (IOException err) {
			String msg = NLS.bind(UIText.GitDocument_errorLoadCommit,
					new Object[] { commitId, baseline, resource, repository });
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

		try (TreeWalk tw = TreeWalk.forPath(repository, oldPath, treeId)) {
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
				ObjectLoader loader = LfsFactory.getInstance().applySmudgeFilter(
						repository, repository.open(id, Constants.OBJ_BLOB),
						tw.getAttributes().get(Constants.ATTR_DIFF));
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
			if (GitTraceLocation.QUICKDIFF.isActive()) {
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.QUICKDIFF.getLocation());
			}
		}

	}

	void dispose() {
		if (GitTraceLocation.QUICKDIFF.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.QUICKDIFF.getLocation(),
					"(GitDocument) dispose: " + resource); //$NON-NLS-1$
		synchronized (doc2repo) {
			doc2repo.remove(this);
		}
		if (myRefsChangedHandle != null) {
			myRefsChangedHandle.remove();
			myRefsChangedHandle = null;
		}
		cancelReloadJob();
		disposed = true;
	}

	@Override
	public void onRefsChanged(final RefsChangedEvent event) {
		cancelReloadJob();

		reloadJob = new Job(UIText.GitDocument_ReloadJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					populate();
					return Status.OK_STATUS;
				} catch (IOException e) {
					return Activator.createErrorStatus(
							UIText.GitDocument_ReloadJobError, e);
				}
			}
		};
		reloadJob.schedule();
	}

	private void cancelReloadJob() {
		if (reloadJob != null && reloadJob.getState() != Job.NONE)
			reloadJob.cancel();
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
		final Entry[] docs;
		synchronized (doc2repo) {
			docs = doc2repo.entrySet().toArray(new Entry[0]);
		}
		for (Entry doc : docs)
			if (doc.getValue() == repository)
				((GitDocument) doc.getKey()).populate();
	}
}

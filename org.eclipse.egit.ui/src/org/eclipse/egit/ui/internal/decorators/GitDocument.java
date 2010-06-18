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

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.text.Document;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexChangedEvent;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.RefsChangedEvent;
import org.eclipse.jgit.lib.RepositoryListener;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;

class GitDocument extends Document implements RepositoryListener {
	private final IResource resource;

	private ObjectId lastCommit;
	private ObjectId lastTree;
	private ObjectId lastBlob;

	static Map<GitDocument,Repository> doc2repo = new WeakHashMap<GitDocument, Repository>();

	static GitDocument create(final IResource resource) throws IOException {
		// TODO is this the right location?
		if (GitTraceLocation.UI.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.UI.getLocation(),
					"(GitDocument) create: " + resource); //$NON-NLS-1$
		GitDocument ret = null;
		if (RepositoryProvider.getProvider(resource.getProject()) instanceof GitProvider) {
			ret = new GitDocument(resource);
			ret.populate();
			final Repository repository = ret.getRepository();
			if (repository != null)
				repository.addRepositoryChangedListener(ret);
		}
		return ret;
	}

	private GitDocument(IResource resource) {
		this.resource = resource;
		GitDocument.doc2repo.put(this, getRepository());
	}

	private void setResolved(final AnyObjectId commit, final AnyObjectId tree, final AnyObjectId blob, final String value) {
		lastCommit = commit != null ? commit.copy() : null;
		lastTree = tree != null ? tree.copy() : null;
		lastBlob = blob != null ? blob.copy() : null;
		set(value);
		if (blob != null)
			// TODO is this the right location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation
						.getTrace()
						.trace(
								GitTraceLocation.UI.getLocation(),
								"(GitDocument) resolved " + resource + " to " + lastBlob + " in " + lastCommit + "/" + lastTree); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			else
			// TODO is this the right location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.UI.getLocation(),
						"(GitDocument) unresolved " + resource); //$NON-NLS-1$
	}

	void populate() throws IOException {
		// TODO is this the right location?
		if (GitTraceLocation.UI.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.UI.getLocation(),"(GitDocument) populate: " + resource); //$NON-NLS-1$
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
				// TODO is this the right location?
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.UI.getLocation(),
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
		Commit baselineCommit = repository.mapCommit(commitId);
		if (baselineCommit == null) {
			String msg = NLS.bind(UIText.GitDocument_errorLoadCommit,
					new Object[] { commitId, baseline, resource, repository });
			Activator.logError(msg, new Throwable());
			setResolved(null, null, null, ""); //$NON-NLS-1$
			return;
		}
		ObjectId treeId = baselineCommit.getTreeId();
		if (treeId.equals(lastTree)) {
			// TODO is this the right location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.UI.getLocation(),
						"(GitDocument) already resolved"); //$NON-NLS-1$
			return;
		}
		Tree baselineTree = baselineCommit.getTree();
		if (baselineTree == null) {
			String msg = NLS.bind(UIText.GitDocument_errorLoadTree,
					new Object[] { treeId, baseline, resource, repository });
			Activator.logError(msg, new Throwable());
			setResolved(null, null, null, ""); //$NON-NLS-1$
			return;
		}
		TreeEntry blobEntry = baselineTree.findBlobMember(gitPath);
		if (blobEntry != null && !blobEntry.getId().equals(lastBlob)) {
			// TODO is this the right location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.UI.getLocation(),
						"(GitDocument) compareTo: " + baseline); //$NON-NLS-1$
			ObjectLoader loader = repository.openBlob(blobEntry.getId());
			byte[] bytes = loader.getBytes();
			String charset;
			// Get the encoding for the current version. As a matter of
			// principle one might want to use the eclipse settings for the
			// version we are retrieving as that may be defined by the
			// project settings, but there is no historic API for this.
			IEncodedStorage encodedStorage = ((IEncodedStorage)resource);
			try {
				charset = encodedStorage.getCharset();
				if (charset != null)
					charset = resource.getParent().getDefaultCharset();
			} catch (CoreException e) {
				charset = Constants.CHARACTER_ENCODING;
			}
			// Finally we could consider validating the content with respect
			// to the content. We don't do that here.
			String s = new String(bytes, charset);
			setResolved(commitId, baselineTree.getId(), blobEntry.getId(), s);
			// TODO is this the right location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation
						.getTrace()
						.trace(
								GitTraceLocation.UI.getLocation(),
								"(GitDocument) has reference doc, size=" + s.length() + " bytes"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			if (blobEntry == null)
				setResolved(null, null, null, ""); //$NON-NLS-1$
			else
			// TODO is this the right location?
			if (GitTraceLocation.UI.isActive())
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.UI.getLocation(),
						"(GitDocument) already resolved"); //$NON-NLS-1$
		}
	}

	void dispose() {
		// TODO is this the right location?
		if (GitTraceLocation.UI.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.UI.getLocation(),
					"(GitDocument) dispose: " + resource); //$NON-NLS-1$
		doc2repo.remove(this);
		Repository repository = getRepository();
		if (repository != null)
			repository.removeRepositoryChangedListener(this);
	}

	public void refsChanged(final RefsChangedEvent e) {
		try {
			populate();
		} catch (IOException e1) {
			Activator.logError(UIText.GitDocument_errorRefreshQuickdiff, e1);
		}
	}

	public void indexChanged(final IndexChangedEvent e) {
		// Index not relevant at this moment
	}

	private Repository getRepository() {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		return (mapping != null) ? mapping.getRepository() : null;
	}

	/**
	 * A change occurred to a repository. Update any GitDocument instances
	 * referring to such repositories.
	 *
	 * @param repository Repository which changed
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

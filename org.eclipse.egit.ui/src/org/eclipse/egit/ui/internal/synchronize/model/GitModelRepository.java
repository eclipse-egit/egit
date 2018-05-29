/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.egit.core.synchronize.StagedChangeCache;
import org.eclipse.egit.core.synchronize.WorkingTreeChangeCache;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Representation of Git repository in Git ChangeSet model.
 */
public class GitModelRepository extends GitModelObjectContainer implements HasProjects {

	private IPath location;

	private final GitSynchronizeData gsd;

	private GitModelObject[] children;

	/**
	 * @param gsd
	 *            synchronization data
	 * @throws IOException
	 */
	public GitModelRepository(GitSynchronizeData gsd) throws IOException {
		super(null);
		this.gsd = gsd;
	}

	@Override
	public GitModelObject[] getChildren() {
		List<GitModelObjectContainer> result = new ArrayList<>();
		Repository repo = gsd.getRepository();
		RevCommit srcRevCommit = gsd.getSrcRevCommit();
		RevCommit dstRevCommit = gsd.getDstRevCommit();
		TreeFilter pathFilter = gsd.getPathFilter();
		List<Commit> commitCache;
		if (srcRevCommit != null && dstRevCommit != null)
			try {
				commitCache = GitCommitsModelCache.build(repo, srcRevCommit,
						dstRevCommit, pathFilter);
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
				commitCache = null;
			}
		else
			commitCache = null;
		if (commitCache != null && !commitCache.isEmpty())
			result.addAll(getListOfCommit(commitCache));

		result.addAll(getWorkingChanges());
		disposeOldChildren();
		children = result.toArray(new GitModelObjectContainer[result.size()]);

		return children;
	}

	@Override
	public String getName() {
		return gsd.getRepository().getWorkTree().toString();
	}

	@Override
	public IProject[] getProjects() {
		return gsd.getProjects().toArray(new IProject[gsd.getProjects().size()]);
	}

	@Override
	public int repositoryHashCode() {
		return hashCode();
	}

	/**
	 * @return source {@link RevObject}
	 */
	public ObjectId getSrcRev() {
		return gsd.getSrcRevCommit();
	}

	@Override
	public IPath getLocation() {
		if (location == null)
			location = new Path(gsd.getRepository().getWorkTree().toString());

		return location;
	}

	@Override
	public int getKind() {
		return Differencer.CHANGE;
	}

	@Override
	public void dispose() {
		disposeOldChildren();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj instanceof GitModelRepository) {
			File objWorkTree = ((GitModelRepository) obj).gsd.getRepository()
					.getWorkTree();
			return objWorkTree.equals(gsd.getRepository().getWorkTree());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return gsd.getRepository().getWorkTree().hashCode();
	}

	@Override
	public String toString() {
		return "ModelRepository[" + gsd.getRepository().getWorkTree() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private List<GitModelObjectContainer> getListOfCommit(List<Commit> commitCache) {
		Repository repo = gsd.getRepository();
		Set<IProject> projectsSet = gsd.getProjects();
		IProject[] projects = projectsSet.toArray(new IProject[projectsSet.size()]);
		List<GitModelObjectContainer> result = new ArrayList<>();

		for (Commit commit : commitCache)
			result.add(new GitModelCommit(this, repo, commit, projects));

		return result;
	}

	private List<GitModelObjectContainer> getWorkingChanges() {
		List<GitModelObjectContainer> result = new ArrayList<>();
		if (gsd.shouldIncludeLocal()) {
			Repository repo = gsd.getRepository();
			Map<String, Change> stagedChanges = StagedChangeCache.build(repo);
			GitModelCache gitCache = new GitModelCache(this, repo,
					stagedChanges);
			int gitCacheLen = gitCache.getChildren().length;

			Map<String, Change> workingChanges = WorkingTreeChangeCache.build(repo);
			GitModelWorkingTree gitWorkingTree = new GitModelWorkingTree(this,
					repo, workingChanges);
			int gitWorkingTreeLen = gitWorkingTree.getChildren().length;

			if (gitCacheLen > 0 || gitWorkingTreeLen > 0) {
				result.add(gitCache);
				result.add(gitWorkingTree);
			}
		}

		return result;
	}

	/**
	 * @return repository
	 */
	public Repository getRepository() {
		return gsd.getRepository();
	}

	private void disposeOldChildren() {
		if (children == null)
			return;
		for (GitModelObject child : children)
			child.dispose();

	}

}

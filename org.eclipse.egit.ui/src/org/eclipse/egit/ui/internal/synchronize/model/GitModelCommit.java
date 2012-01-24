/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.jgit.lib.Repository;

/**
 * Git commit object representation in Git ChangeSet
 */
public class GitModelCommit extends GitModelObjectContainer implements
		HasProjects {

	private final Commit commit;

	private final Repository repo;

	private final IProject[] projects;

	private final Map<String, GitModelObject> cachedTreeMap = new HashMap<String, GitModelObject>();

	/**
	 * @param parent
	 *            parent object
	 * @param repo
	 *            repository associated with this object
	 * @param commit
	 *            instance of commit that will be associated with this model
	 *            object
	 * @param projects
	 *            list of changed projects
	 */
	public GitModelCommit(GitModelRepository parent, Repository repo,
			Commit commit, IProject[] projects) {
		super(parent);
		this.repo = repo;
		this.commit = commit;
		this.projects = projects;
	}

	@Override
	public IPath getLocation() {
		return new Path(repo.getWorkTree().getAbsolutePath());
	}

	public IProject[] getProjects() {
		return projects;
	}

	@Override
	public int getKind() {
		return commit.getDirection() | Differencer.CHANGE;
	}

	@Override
	public int repositoryHashCode() {
		return repo.getWorkTree().hashCode();
	}

	@Override
	public String getName() {
		return commit.getShortMessage();
	}

	@Override
	public GitModelObject[] getChildren() {
		List<GitModelObject> result = new ArrayList<GitModelObject>();

		if (commit.getChildren() != null) // prevent from NPE in empty commits
			for (Entry<String, Change> cacheEntry : commit.getChildren().entrySet()) {
				GitModelObject nested = addChild(cacheEntry.getValue(), cacheEntry.getKey());
				if (nested != null)
					result.add(nested);
			}

		return result.toArray(new GitModelObject[result.size()]);
	}

	/**
	 * @return cached commit object
	 */
	public Commit getCachedCommitObj() {
		return commit;
	}

	@Override
	public void dispose() {
		for (GitModelObject value : cachedTreeMap.values())
			value.dispose();

		cachedTreeMap.clear();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj == null)
			return false;

		if (obj.getClass() != getClass())
			return false;

		GitModelCommit objCommit = (GitModelCommit) obj;

		return objCommit.commit.getId().equals(commit.getId());
	}

	@Override
	public int hashCode() {
		return commit.hashCode();
	}

	@Override
	public String toString() {
		return "ModelCommit[" + commit.getId() + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}

	private GitModelObject addChild(Change change, String nestedPath) {
		GitModelObject firstObject = null;
		IPath tmpLocation = getLocation();
		String[] segments = nestedPath.split("/"); //$NON-NLS-1$
		GitModelObjectContainer tmpPartent = this;
		Map<String, GitModelObject> tmpCache = cachedTreeMap;

		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			tmpLocation = tmpLocation.append(segment);
			if (i < segments.length - 1) {
				GitModelTree tree = (GitModelTree) tmpCache.get(segment);
				if (tree == null) {
					tree = new GitModelTree(tmpPartent, tmpLocation, change.getKind());
					tmpCache.put(segment, tree);
				}
				tmpPartent = tree;
				tmpCache = tree.cachedTreeMap;
				if (i == 0)
					firstObject = tmpPartent;
			} else { // handle last segment, it should be a file name
				GitModelBlob blob = new GitModelBlob(tmpPartent, repo, change,
						tmpLocation);
				tmpCache.put(segment, blob);
				if (i == 0)
					firstObject = blob;
			}
		}

		return firstObject;
	}

}

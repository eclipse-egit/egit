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

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Change;
import org.eclipse.egit.core.synchronize.GitCommitsModelCache.Commit;
import org.eclipse.egit.ui.internal.synchronize.model.TreeBuilder.FileModelFactory;
import org.eclipse.egit.ui.internal.synchronize.model.TreeBuilder.TreeModelFactory;
import org.eclipse.jgit.lib.Repository;

/**
 * Git commit object representation in Git ChangeSet
 */
public class GitModelCommit extends GitModelObjectContainer implements
		HasProjects {

	private final Commit commit;

	private final Repository repo;

	private final IProject[] projects;

	private GitModelObject[] children;

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

	@Override
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
		if (children == null)
			children = createChildren();
		return children;
	}

	private GitModelObject[] createChildren() {
		FileModelFactory fileModelFactory = new FileModelFactory() {
			@Override
			public GitModelBlob createFileModel(GitModelObjectContainer parent,
					Repository repository, Change change, IPath fullPath) {
				return new GitModelBlob(parent, repository, change, fullPath);
			}

			@Override
			public boolean isWorkingTree() {
				return false;
			}
		};
		TreeModelFactory treeModelFactory = new TreeModelFactory() {
			@Override
			public GitModelTree createTreeModel(GitModelObjectContainer parent,
					IPath fullPath, int kind) {
				return new GitModelTree(parent, fullPath, kind);
			}
		};
		return TreeBuilder.build(this, repo, commit.getChildren(),
				fileModelFactory, treeModelFactory);
	}

	/**
	 * @return cached commit object
	 */
	public Commit getCachedCommitObj() {
		return commit;
	}

	@Override
	public void dispose() {
		if (children != null) {
			for (GitModelObject child : children)
				child.dispose();
			children = null;
		}
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

}

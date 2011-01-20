/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize.dto;

import static org.eclipse.core.runtime.Assert.isNotNull;
import static org.eclipse.egit.core.RevUtils.getCommonAncestor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Simple data transfer object containing all necessary information for
 * launching synchronization
 */
public class GitSynchronizeData {

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace()
					.getRoot();

	private final boolean includeLocal;

	private final Repository repo;

	private final RevCommit srcRev;

	private final RevCommit dstRev;

	private final RevCommit commonAncestorRev;

	private final Set<IProject> projects;

	private final String repoParentPath;

	/**
	 * Constructs {@link GitSynchronizeData} object
	 *
	 * @param repository
	 * @param srcRev
	 * @param dstRev
	 * @param includeLocal
	 *            <code>true</code> if local changes should be included in
	 *            comparison
	 * @throws IOException
	 */
	public GitSynchronizeData(Repository repository, String srcRev,
			String dstRev, boolean includeLocal) throws IOException {
		isNotNull(repository);
		isNotNull(srcRev);
		isNotNull(dstRev);
		repo = repository;

		ObjectWalk ow = new ObjectWalk(repo);
		this.srcRev = ow.parseCommit(repo.resolve(srcRev));
		this.dstRev = ow.parseCommit(repo.resolve(dstRev));
		this.commonAncestorRev = getCommonAncestor(repo, this.srcRev, this.dstRev);

		this.includeLocal = includeLocal;
		repoParentPath = repo.getDirectory().getParentFile().getAbsolutePath();

		projects = new HashSet<IProject>();
		final IProject[] workspaceProjects = ROOT.getProjects();
		for (IProject project : workspaceProjects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null && mapping.getRepository() == repo)
				projects.add(project);
		}

	}

	/**
	 * @return instance of repository that should be synchronized
	 */
	public Repository getRepository() {
		return repo;
	}

	/**
	 * @return synchronize source rev name
	 */
	public RevCommit getSrcRevCommit() {
		return srcRev;
	}

	/**
	 * @return synchronize destination rev name
	 */
	public RevCommit getDstRevCommit() {
		return dstRev;
	}

	/**
	 * @return list of project's that are connected with this repository
	 */
	public Set<IProject> getProjects() {
		return Collections.unmodifiableSet(projects);
	}

	/**
	 * @param file
	 * @return <true> if given {@link File} is contained by this repository
	 */
	public boolean contains(File file) {
		return file.getAbsoluteFile().toString().startsWith(repoParentPath);
	}

	/**
	 * @return <code>true</code> if local changes should be included in
	 *         comparison
	 */
	public boolean shouldIncludeLocal() {
		return includeLocal;
	}

	/**
	 * @return common ancestor commit
	 */
	public RevCommit getCommonAncestorRev() {
		return commonAncestorRev;
	}

}

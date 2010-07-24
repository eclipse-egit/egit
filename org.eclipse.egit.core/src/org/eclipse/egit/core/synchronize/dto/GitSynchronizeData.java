/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize.dto;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Simple data transfer object containing all necessary information for
 * launching synchronization
 */
public class GitSynchronizeData {

	private final boolean includeLocal;

	private final Repository repo;

	private final Ref srcRev;

	private final Ref dstRev;

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
		repo = repository;
		this.srcRev = repo.getRef(srcRev);
		this.dstRev = repo.getRef(dstRev);
		this.includeLocal = includeLocal;
		repoParentPath = repo.getDirectory().getParentFile().getAbsolutePath();

		projects = new HashSet<IProject>();
		final IProject[] workspaceProjects = ResourcesPlugin.getWorkspace()
				.getRoot().getProjects();
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
	public Ref getSrcRev() {
		return srcRev;
	}

	/**
	 * @return synchronize destination rev name
	 */
	public Ref getDstRev() {
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

}

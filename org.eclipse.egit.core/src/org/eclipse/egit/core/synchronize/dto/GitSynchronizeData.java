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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.lib.Tree;

/**
 * Simple data transfer object containing all necessary information for
 * launching synchronization
 */
public class GitSynchronizeData {

	private final boolean includeLocal;

	private final Repository repo;

	private final String srcRev;

	private final String dstRev;

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
	 */
	public GitSynchronizeData(Repository repository, String srcRev,
			String dstRev, boolean includeLocal) {
		repo = repository;
		this.srcRev = srcRev;
		this.dstRev = dstRev;
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
	public String getSrcRev() {
		return srcRev;
	}

	/**
	 * @return synchronize destination rev name
	 */
	public String getDstRev() {
		return dstRev;
	}

	/**
	 * @return source {@link Tree}
	 * @throws IOException
	 */
	public Tree mapSrcTree() throws IOException {
		return mapTree(srcRev);
	}

	/**
	 * @return destination {@link Tree}
	 * @throws IOException
	 */
	public Tree mapDstTree() throws IOException {
		return mapTree(dstRev);
	}

	/**
	 * @return source {@link ObjectId}
	 * @throws IOException
	 */
	public ObjectId getSrcObjectId() throws IOException {
		return getObjecId(srcRev);
	}

	/**
	 * @return destination {@link ObjectId}
	 * @throws IOException
	 */
	public ObjectId getDstObjectId() throws IOException {
		return getObjecId(dstRev);
	}

	/**
	 * @return list of project's that are connected with this repository
	 */
	public Set<IProject> getProjects() {
		return Collections.unmodifiableSet(projects);
	}

	/**
	 *
	 * @param resource
	 * @return <true> if given {@link IResource} is contained by this repository
	 */
	public boolean contains(IResource resource) {
		return resource.getFullPath().toString().startsWith(repoParentPath);
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

	private Tree mapTree(String rev) throws IOException {
		if (rev.startsWith(Constants.R_TAGS)) {
			Tag tag = repo.mapTag(rev);
			if (tag != null) {
				Commit commit = repo.mapCommit(tag.getObjId());
				if (commit != null)
					return commit.getTree();
			}
			return null;
		} else
			return repo.mapTree(rev);
	}

	private ObjectId getObjecId(String rev) throws IOException {
		if (rev.startsWith(Constants.R_TAGS)) {
			Tag mapTag = repo.mapTag(rev);
			if (mapTag != null)
				return mapTag.getObjId();
		} else {
			Commit mapCommit = repo.mapCommit(rev);
			if (mapCommit != null)
				return mapCommit.getCommitId();
		}
		return null;
	}

	/**
	 *
	 * @return source RevCommit object
	 */
	public Commit getCommit() {
		try {
			return repo.mapCommit(srcRev);
		} catch (IOException e) {
			// TODO handle exception
			return null;
		}
	}

}

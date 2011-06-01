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
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_REMOTE;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Ref;
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

	private static final Pattern BRANCH_NAME_PATTERN = Pattern.compile("^" + R_HEADS + ".*?"); //$NON-NLS-1$ //$NON-NLS-2$

	private final boolean includeLocal;

	private final Repository repo;

	private final String srcRemote;

	private final String dstRemote;

	private final String srcMerge;

	private final String dstMerge;

	private RevCommit srcRevCommit;

	private RevCommit dstRevCommit;

	private RevCommit ancestorRevCommit;

	private final Set<IProject> projects;

	private final String repoParentPath;

	private final String srcRev;

	private final String dstRev;

	private static class RemoteConfig {
		final String remote;
		final String merge;
		public RemoteConfig(String remote, String merge) {
			this.remote = remote;
			this.merge = merge;
		}
	}

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
		this.srcRev = srcRev;
		this.dstRev = dstRev;
		this.includeLocal = includeLocal;

		RemoteConfig srcRemoteConfig = extractRemoteName(srcRev);
		RemoteConfig dstRemoteConfig = extractRemoteName(dstRev);

		srcRemote = srcRemoteConfig.remote;
		srcMerge = srcRemoteConfig.merge;

		dstRemote = dstRemoteConfig.remote;
		dstMerge = dstRemoteConfig.merge;

		repoParentPath = repo.getDirectory().getParentFile().getAbsolutePath();

		projects = new HashSet<IProject>();
		final IProject[] workspaceProjects = ROOT.getProjects();
		for (IProject project : workspaceProjects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null && mapping.getRepository() == repo)
				projects.add(project);
		}
		updateRevs();
	}

	/**
	 * Recalculates source, destination and ancestor Rev commits
	 *
	 * @throws IOException
	 */
	public void updateRevs() throws IOException {
		ObjectWalk ow = new ObjectWalk(repo);
		if (srcRev.length() > 0)
			this.srcRevCommit = ow.parseCommit(repo.resolve(srcRev));
		else
			this.srcRevCommit = null;

		if (dstRev.length() > 0)
			this.dstRevCommit = ow.parseCommit(repo.resolve(dstRev));
		else
			this.dstRevCommit = null;

		if (this.dstRevCommit != null || this.srcRevCommit != null)
			this.ancestorRevCommit = getCommonAncestor(repo, this.srcRevCommit,
					this.dstRevCommit);
		else
			this.ancestorRevCommit = null;
	}

	/**
	 * @return instance of repository that should be synchronized
	 */
	public Repository getRepository() {
		return repo;
	}

	/**
	 * @return name of source remote or {@code null} when source branch is not a
	 *         remote branch
	 */
	public String getSrcRemoteName() {
		return srcRemote;
	}

	/**
	 * @return ref specification of destination merge branch
	 */
	public String getDstMerge() {
		return dstMerge;
	}

	/**
	 * @return ref specification of source merge branch
	 */
	public String getSrcMerge() {
		return srcMerge;
	}

	/**
	 * @return name of destination remote or {@code null} when destination
	 *         branch is not a remote branch
	 */
	public String getDstRemoteName() {
		return dstRemote;
	}

	/**
	 * @return synchronize source rev name
	 */
	public RevCommit getSrcRevCommit() {
		return srcRevCommit;
	}

	/**
	 * @return synchronize destination rev name
	 */
	public RevCommit getDstRevCommit() {
		return dstRevCommit;
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
		return ancestorRevCommit;
	}

	private RemoteConfig extractRemoteName(String rev) {
		if (rev.contains(R_REMOTES)) {
			String remoteWithBranchName = rev.replaceAll(R_REMOTES, ""); //$NON-NLS-1$
			int firstSeparator = remoteWithBranchName.indexOf("/"); //$NON-NLS-1$

			String remote = remoteWithBranchName.substring(0, firstSeparator);
			String name = remoteWithBranchName.substring(firstSeparator,
					remoteWithBranchName.length());

			return new RemoteConfig(remote, name);
		} else {
			String realName;
			Ref ref;
			try {
				ref = repo.getRef(rev);
			} catch (IOException e) {
				ref = null;
			}
			if (ref != null && ref.isSymbolic())
				realName = ref.getTarget().getName();
			else
				realName = rev;
			String name = BRANCH_NAME_PATTERN.matcher(realName).replaceAll(""); //$NON-NLS-1$
			String remote = repo.getConfig().getString(CONFIG_BRANCH_SECTION,
					name, CONFIG_KEY_REMOTE);
			String merge = repo.getConfig().getString(CONFIG_BRANCH_SECTION,
					name, ConfigConstants.CONFIG_KEY_MERGE);

			return new RemoteConfig(remote, merge);
		}
	}

}

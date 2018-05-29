/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Shunichi Fuji <palglowr@gmail.com>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, François Rey <eclipse.org_@_francois_._rey_._name>
 * Copyright (C) 2013, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andre Bossert <anb0s@anbos.de> - Extended support for nested repositories in project.
 *******************************************************************************/
package org.eclipse.egit.core.project;

import static org.eclipse.egit.core.internal.util.ResourceUtil.isNonWorkspace;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;

/**
 * This class provides means to map resources, projects and repositories
 */
public class RepositoryMapping {
	static boolean isInitialKey(final String key) {
		return key.endsWith(".gitdir");  //$NON-NLS-1$
	}

	private final String containerPathString;

	private IPath containerPath;

	private final String gitDirPathString;

	private IPath gitDirPath;

	private IPath gitDirAbsolutePath;

	private Repository db;

	private String workdirPrefix;

	private IContainer container;

	/**
	 * Construct a {@link RepositoryMapping} for a previously connected
	 * container.
	 *
	 * @param p
	 *            {@link Properties} to read the git directory from. See also
	 *            {@link GitProjectData}.
	 * @param initialKey
	 *            property key to use to read the git directory
	 */
	public RepositoryMapping(final @NonNull Properties p,
			final @NonNull String initialKey) {
		final int dot = initialKey.lastIndexOf('.');

		containerPathString = initialKey.substring(0, dot);
		gitDirPathString = p.getProperty(initialKey);
	}

	/**
	 * Construct a {@link RepositoryMapping} for previously unmapped container.
	 *
	 * @param mappedContainer
	 *            to create the mapping for
	 * @param gitDir
	 *            for the new mapping
	 * @return a new RepositoryMapping for given container. Returns {@code null}
	 *         if container does not exist or is not local.
	 */
	@Nullable
	public static RepositoryMapping create(@NonNull IContainer mappedContainer,
			@NonNull File gitDir) {
		IPath location = mappedContainer.getLocation();
		if (location == null) {
			return null;
		}
		return new RepositoryMapping(mappedContainer, location, gitDir);
	}

	private RepositoryMapping(final @NonNull IContainer mappedContainer,
			final @NonNull IPath location, final @NonNull File gitDir) {
		container = mappedContainer;
		containerPathString = container.getProjectRelativePath()
				.toPortableString();
		if (!gitDir.isAbsolute()) {
			// It is relative to location, so we can set it right away.
			gitDirPathString = Path.fromOSString(gitDir.getPath())
					.removeTrailingSeparator().toPortableString();
			return;
		}
		java.nio.file.Path gPath = gitDir.toPath();
		java.nio.file.Path lPath = location.toFile().toPath();
		// Require at least one common component for using a relative path
		if (lPath.getNameCount() > 0 && gPath.getNameCount() > 0
				&& (gPath.getRoot() == lPath.getRoot()
						|| gPath.getRoot() != null
								&& gPath.getRoot().equals(lPath.getRoot()))
				&& gPath.getName(0).equals(lPath.getName(0))) {
			gPath = lPath.relativize(gPath);
		}
		gitDirPathString = Path.fromOSString(gPath.toString())
				.removeTrailingSeparator().toPortableString();
	}

	/**
	 * @return the container path corresponding to git repository
	 */
	@NonNull
	public IPath getContainerPath() {
		if (containerPath == null)
			containerPath = Path.fromPortableString(containerPathString);
		return containerPath;
	}

	@NonNull
	IPath getGitDirPath() {
		if (gitDirPath == null)
			gitDirPath = Path.fromPortableString(gitDirPathString);
		return gitDirPath;
	}

	/**
	 * @return the workdir file, i.e. where the files are checked out, or null
	 *         if repository is bare
	 */
	@Nullable
	public File getWorkTree() {
		Repository repo = getRepository();
		if (repo.isBare()) {
			return null;
		}
		return repo.getWorkTree();
	}

	synchronized void clear() {
		db = null;
		workdirPrefix = null;
		container = null;
	}

	/**
	 * @return a reference to the repository object handled by this mapping
	 */
	/* TODO currently the value is @Nullable but it must be NonNull */
	public synchronized Repository getRepository() {
		return db;
	}

	synchronized void setRepository(final Repository r) {
		db = r;
		File workTree = getWorkTree();
		if (workTree == null) {
			return;
		}
		workdirPrefix = workTree.getAbsolutePath();
		workdirPrefix = workdirPrefix.replace('\\', '/');
		if (!workdirPrefix.endsWith("/")) {  //$NON-NLS-1$
			workdirPrefix += "/";  //$NON-NLS-1$
		}
	}

	/**
	 * @return the mapped container (currently project)
	 */
	/* TODO currently the value is @Nullable but it must be NonNull */
	public synchronized IContainer getContainer() {
		return container;
	}

	synchronized void setContainer(final IContainer c) {
		container = c;
	}

	synchronized void store(final Properties p) {
		p.setProperty(containerPathString + ".gitdir", gitDirPathString); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		IPath absolutePath = getGitDirAbsolutePath();
		return "RepositoryMapping[" //$NON-NLS-1$
				+ format(containerPathString)
				+ " -> '" //$NON-NLS-1$
				+ format(gitDirPathString)
				+ "', absolute path: '"  //$NON-NLS-1$
				+ format(absolutePath) + "' ]"; //$NON-NLS-1$
	}

	private String format(Object o) {
		if (o == null)
			return "<null>"; //$NON-NLS-1$
		else if (o.toString().length() == 0)
			return "<empty>"; //$NON-NLS-1$
		else
			return o.toString();
	}

	/**
	 * This method should only be called for resources that are actually in this
	 * repository, so we can safely assume that their path prefix matches
	 * {@link #getWorkTree()}. Testing that here is rather expensive so we don't
	 * bother.
	 *
	 * @param rsrc
	 * @return the path relative to the Git repository, including base name. An
	 *         empty string (<code>""</code>) if passed resource corresponds to
	 *         working directory (root). <code>null</code> if the path cannot be
	 *         determined.
	 */
	@Nullable
	public String getRepoRelativePath(final @NonNull IResource rsrc) {
		IPath location = rsrc.getLocation();
		if (location == null)
			return null;
		return getRepoRelativePath(location);
	}

	/**
	 * This method should only be called for resources that are actually in this
	 * repository, so we can safely assume that their path prefix matches
	 * {@link #getWorkTree()}. Testing that here is rather expensive so we don't
	 * bother.
	 *
	 * @param location
	 * @return the path relative to the Git repository, including base name. An
	 *         empty string (<code>""</code>) if passed location corresponds to
	 *         working directory (root). <code>null</code> if the path cannot be
	 *         determined.
	 */
	@Nullable
	public synchronized String getRepoRelativePath(@NonNull IPath location) {
		if (workdirPrefix == null) {
			return null;
		}
		final int pfxLen = workdirPrefix.length();
		final String p = location.toString();
		final int pLen = p.length();
		if (pLen > pfxLen) {
			return p.substring(pfxLen);
		}
		if (pLen == pfxLen - 1) {
			return ""; //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Get the repository mapping for a resource. If the given resource is a
	 * linked resource, the raw location of the resource will be used to
	 * determine a repository mapping.
	 *
	 * @param resource
	 *            to find the mapping for
	 * @return the RepositoryMapping for this resource, or null for non
	 *         GitProvider.
	 */
	@Nullable
	public static RepositoryMapping getMapping(
			final @NonNull IResource resource) {
		if (isNonWorkspace(resource)) {
			return null;
		}
		if (resource.isLinked(IResource.CHECK_ANCESTORS)) {
			IPath location = resource.getLocation();
			if (location == null) {
				return null;
			}
			return getMapping(location);
		}
		return findMapping(resource);
	}

	/**
	 * Get the repository mapping for a project.
	 *
	 * @param project
	 *            to find the mapping for
	 * @return the RepositoryMapping for this project, or null for non
	 *         GitProvider.
	 */
	@Nullable
	public static RepositoryMapping getMapping(
			final @Nullable IProject project) {
		if (project == null) {
			return null;
		}
		return findMapping(project);
	}

	/**
	 * Get the git project data for a project.
	 *
	 * @param project
	 *            to find the data for
	 * @return the git project data for this project, or null for non
	 *         GitProvider.
	 */
	@Nullable
	private static GitProjectData getProjectData(
			final @Nullable IProject project) {
		if (project == null || isNonWorkspace(project)) {
			return null;
		}
		final GitProvider rp = ResourceUtil.getGitProvider(project);
		GitProjectData data;
		// The provider could not yet be mapped
		if (rp == null) {
			// Load the data directly
			data = GitProjectData.get(project);
			if (data == null) {
				return null;
			}
		} else {
			data = rp.getData();
		}
		return data;
	}

	/**
	 * Get the repository mapping for a resource.
	 *
	 * @param resource
	 *            to find the mapping for
	 * @return the RepositoryMapping for this resource, or null if resource is
	 *         not associated with Git managed project.
	 */
	@Nullable
	private static RepositoryMapping findMapping(
			final @NonNull IResource resource) {
		GitProjectData data = getProjectData(resource.getProject());
		if (data == null) {
			return null;
		}
		return data.getRepositoryMapping(resource);
	}

	/**
	 * Get all repository mappings for a project.
	 *
	 * @param project
	 * @return all RepositoryMappings for this project, can be empty list for
	 *         non GitProvider.
	 */
	@NonNull
	private static Map<IPath, RepositoryMapping> getMappings(
			final @Nullable IProject project) {
		GitProjectData data = getProjectData(project);
		if (data == null) {
			return Collections.emptyMap();
		}
		return data.getRepositoryMappings();
	}

	/**
	 * Get the repository mapping for a path if it exists.
	 *
	 * @param path
	 * @return the RepositoryMapping for this path, or null for non GitProvider.
	 */
	@Nullable
	public static RepositoryMapping getMapping(@NonNull IPath path) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		IPath bestWorkingTree = null;
		RepositoryMapping bestMapping = null;
		for (IProject project : projects) {
			if (isNonWorkspace(project)) {
				continue;
			}
			for (RepositoryMapping mapping : getMappings(project).values()) {
				File workTree = mapping.getWorkTree();
				if (workTree == null) {
					continue;
				}
				IPath workingTree = new Path(workTree.toString());
				if (workingTree.isPrefixOf(path)) {
					if (bestWorkingTree == null || workingTree
							.segmentCount() > bestWorkingTree.segmentCount()) {
						bestWorkingTree = workingTree;
						bestMapping = mapping;
					}
				}
			}
		}
		return bestMapping;
	}

	/**
	 * Finds a RepositoryMapping related to a given repository
	 *
	 * @param repository
	 * @return a RepositoryMapping related to repository. Null if no
	 *         RepositoryMapping exists.
	 */
	@Nullable
	public static RepositoryMapping findRepositoryMapping(
			@NonNull Repository repository) {
		for (IProject project : ProjectUtil.getProjectsUnderPath(
				new Path(repository.getWorkTree().getAbsolutePath()))) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null && mapping.getRepository() == repository) {
				return mapping;
			}
		}
		return null;
	}

	/**
	 * @return the name of the .git directory
	 */
	public String getGitDir() {
		return gitDirPathString;
	}

	/**
	 * @return The GIT DIR absolute path, or null if path is container relative
	 *         and container does not exist
	 */
	@Nullable
	public synchronized IPath getGitDirAbsolutePath() {
		if (gitDirAbsolutePath == null) {
			IPath p = getGitDirPath();
			if (p.isAbsolute())
				gitDirAbsolutePath = p;
			else if (container != null) {
				IPath cloc = container.getLocation();
				if (cloc != null)
					gitDirAbsolutePath = cloc.append(p);
			}
		}
		return gitDirAbsolutePath;
	}
}

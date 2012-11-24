/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Shunichi Fuji <palglowr@gmail.com>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, François Rey <eclipse.org_@_francois_._rey_._name>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.project;

import static org.eclipse.egit.core.internal.util.ResourceUtil.isNonWorkspace;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.RepositoryProvider;

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
	 * Construct a {@link RepositoryMapping} for a previously connected project.
	 *
	 * @param p TODO
	 * @param initialKey TODO
	 */
	public RepositoryMapping(final Properties p, final String initialKey) {
		final int dot = initialKey.lastIndexOf('.');

		containerPathString = initialKey.substring(0, dot);
		gitDirPathString = p.getProperty(initialKey);
	}

	/**
	 * Construct a {@link RepositoryMapping} for previously
	 * unknown project.
	 *
	 * @param mappedContainer
	 * @param gitDir
	 */
	public RepositoryMapping(final IContainer mappedContainer, final File gitDir) {
		final IPath cLoc = mappedContainer.getLocation()
				.removeTrailingSeparator();
		final IPath gLoc = Path.fromOSString(gitDir.getAbsolutePath())
				.removeTrailingSeparator();
		final IPath gLocParent = gLoc.removeLastSegments(1);

		container = mappedContainer;
		containerPathString = container.getProjectRelativePath()
				.toPortableString();

		if (cLoc.isPrefixOf(gLoc)) {
			int matchingSegments = gLoc.matchingFirstSegments(cLoc);
			IPath remainder = gLoc.removeFirstSegments(matchingSegments);
			String device = remainder.getDevice();
			if (device == null)
				gitDirPathString = remainder.toPortableString();
			else
				gitDirPathString = remainder.toPortableString().substring(
						device.length());
		} else if (gLocParent.isPrefixOf(cLoc)) {
			int cnt = cLoc.segmentCount() - cLoc.matchingFirstSegments(gLocParent);
			StringBuilder p = new StringBuilder("");  //$NON-NLS-1$
			while (cnt-- > 0) {
				p.append("../");  //$NON-NLS-1$
			}
			p.append(gLoc.segment(gLoc.segmentCount() - 1));
			gitDirPathString = p.toString();
		} else {
			gitDirPathString = gLoc.toPortableString();
		}
	}

	/**
	 * @return the container path corresponding to git repository
	 */
	public IPath getContainerPath() {
		if (containerPath == null)
			containerPath = Path.fromPortableString(containerPathString);
		return containerPath;
	}

	IPath getGitDirPath() {
		if (gitDirPath == null)
			gitDirPath = Path.fromPortableString(gitDirPathString);
		return gitDirPath;
	}

	/**
	 * @return the workdir file, i.e. where the files are checked out
	 */
	public File getWorkTree() {
		return getRepository().getWorkTree();
	}

	synchronized void clear() {
		db = null;
		workdirPrefix = null;
		container = null;
	}

	/**
	 * @return a reference to the repository object handled by this mapping
	 */
	public synchronized Repository getRepository() {
		return db;
	}

	synchronized void setRepository(final Repository r) {
		db = r;

		try {
			workdirPrefix = getWorkTree().getCanonicalPath();
		} catch (IOException err) {
			workdirPrefix = getWorkTree().getAbsolutePath();
		}
		workdirPrefix = workdirPrefix.replace('\\', '/');
		if (!workdirPrefix.endsWith("/"))  //$NON-NLS-1$
			workdirPrefix += "/";  //$NON-NLS-1$
	}

	/**
	 * @return the mapped container (currently project)
	 */
	public synchronized IContainer getContainer() {
		return container;
	}

	synchronized void setContainer(final IContainer c) {
		container = c;
	}

	/**
	 * Notify registered {@link RepositoryChangeListener}s of a change.
	 *
	 * @see GitProjectData#addRepositoryChangeListener(RepositoryChangeListener)
	 */
	public void fireRepositoryChanged() {
		GitProjectData.fireRepositoryChanged(this);
	}

	synchronized void store(final Properties p) {
		p.setProperty(containerPathString + ".gitdir", gitDirPathString); //$NON-NLS-1$
	}

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
	 * @return the path relative to the Git repository, including base name.
	 *         <code>null</code> if the path cannot be determined.
	 */
	public String getRepoRelativePath(final IResource rsrc) {
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
	 * @return the path relative to the Git repository, including base name.
	 *         <code>null</code> if the path cannot be determined.
	 */
	public String getRepoRelativePath(IPath location) {
		final int pfxLen = workdirPrefix.length();
		final String p = location.toString();
		final int pLen = p.length();
		if (pLen > pfxLen)
			return p.substring(pfxLen);
		if (pLen == pfxLen - 1)
			return ""; //$NON-NLS-1$
		return null;
	}

	/**
	 * Get the repository mapping for a resource. If the given resource is a
	 * linked resource, the raw location of the resource will be used to
	 * determine a repository mapping.
	 *
	 * @param resource
	 * @return the RepositoryMapping for this resource, or null for non
	 *         GitProvider.
	 */
	public static RepositoryMapping getMapping(final IResource resource) {
		if (isNonWorkspace(resource))
			return null;

		if (resource.isLinked(IResource.CHECK_ANCESTORS))
			return getMapping(resource.getRawLocation());

		IProject project = resource.getProject();
		if (project == null)
			return null;

		final RepositoryProvider rp = RepositoryProvider.getProvider(project);
		if (!(rp instanceof GitProvider))
			return null;

		if (((GitProvider)rp).getData() == null)
			return null;

		return ((GitProvider)rp).getData().getRepositoryMapping(resource);
	}

	/**
	 * Get the repository mapping for a path if it exists.
	 *
	 * @param path
	 * @return the RepositoryMapping for this path,
	 *         or null for non GitProvider.
	 */
	public static RepositoryMapping getMapping(IPath path) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();

		for (IProject project : projects) {
			if (isNonWorkspace(project))
				continue;
			RepositoryMapping mapping = getMapping(project);
			if (mapping == null)
				continue;

			Path workingTree = new Path(mapping.getWorkTree().toString());
			IPath relative = path.makeRelativeTo(workingTree);
			String firstSegment = relative.segment(0);

			if (firstSegment == null || !"..".equals(firstSegment)) //$NON-NLS-1$
				return mapping;
		}

		return null;
	}

	/**
	 * Finds a RepositoryMapping related to a given repository
	 *
	 * @param repository
	 * @return a RepositoryMapping related to repository. Null if no
	 *         RepositoryMapping exists.
	 */
	public static RepositoryMapping findRepositoryMapping(Repository repository) {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping != null && mapping.getRepository() == repository)
				return mapping;
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
	 * @return The GIT DIR absolute path
	 */
	public synchronized IPath getGitDirAbsolutePath() {
		if (gitDirAbsolutePath == null) {
			if (container != null) {
				IPath cloc = container.getLocation();
				if (cloc != null)
					gitDirAbsolutePath = cloc.append(getGitDirPath());
			}
		}
		return gitDirAbsolutePath;
	}
}

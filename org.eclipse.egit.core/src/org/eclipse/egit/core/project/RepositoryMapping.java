/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Shunichi Fuji <palglowr@gmail.com>
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.project;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.eclipse.core.filesystem.URIUtil;
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
		IPath destLocation;
		URI containerLocation = mappedContainer.getLocationURI();
		if (containerLocation == null) {
			IPath rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation();
			destLocation = rootLocation.append(mappedContainer.getName());
		} else
			destLocation = URIUtil.toPath(containerLocation);
		final IPath cLoc = destLocation
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
		return "RepositoryMapping[" //$NON-NLS-1$
				+ containerPathString + " -> " //$NON-NLS-1$
				+ gitDirPathString + "]"; //$NON-NLS-1$
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
		final int pfxLen = workdirPrefix.length();
		IPath location = rsrc.getLocation();
		if (location == null)
			return null;
		final String p = location.toString();
		final int pLen = p.length();
		if (pLen > pfxLen)
			return p.substring(pfxLen);
		else if (p.length() == pfxLen - 1)
			return "";  //$NON-NLS-1$
		return null;
	}

	/**
	 * Get the repository mapping for a resource
	 *
	 * @param resource
	 * @return the RepositoryMapping for this resource,
	 *         or null for non GitProvider.
	 */
	public static RepositoryMapping getMapping(final IResource resource) {
		final IProject project = resource.getProject();
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
		if (gitDirAbsolutePath == null)
			gitDirAbsolutePath = container.getLocation()
					.append(getGitDirPath());
		return gitDirAbsolutePath;
	}
}

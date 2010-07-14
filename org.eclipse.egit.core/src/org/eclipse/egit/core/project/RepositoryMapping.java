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
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.egit.core.IgnoreCache;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.lib.GitIndex.Entry;

/**
 * This class keeps track
 */
public class RepositoryMapping {
	static boolean isInitialKey(final String key) {
		return key.endsWith(".gitdir");  //$NON-NLS-1$
	}

	private final String containerPath;

	private final String gitdirPath;

	private Repository db;

	private IgnoreCache cache;

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

		containerPath = initialKey.substring(0, dot);
		gitdirPath = p.getProperty(initialKey);
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
		String p;
		int cnt;

		container = mappedContainer;
		containerPath = container.getProjectRelativePath().toPortableString();

		if (cLoc.isPrefixOf(gLoc)) {
			int matchingSegments = gLoc.matchingFirstSegments(cLoc);
			IPath remainder = gLoc.removeFirstSegments(matchingSegments);
			String device = remainder.getDevice();
			if (device == null)
				gitdirPath = remainder.toPortableString();
			else
				gitdirPath = remainder.toPortableString().substring(device.length());
		} else if (gLocParent.isPrefixOf(cLoc)) {
			cnt = cLoc.segmentCount() - cLoc.matchingFirstSegments(gLocParent);
			p = "";  //$NON-NLS-1$
			while (cnt-- > 0) {
				p += "../";  //$NON-NLS-1$
			}
			p += gLoc.segment(gLoc.segmentCount() - 1);
			gitdirPath = p;
		} else {
			gitdirPath = gLoc.toPortableString();
		}
	}

	/**
	 * @return the container path corresponding to git repository
	 */
	public IPath getContainerPath() {
		return Path.fromPortableString(containerPath);
	}

	IPath getGitDirPath() {
		return Path.fromPortableString(gitdirPath);
	}

	/**
	 * @return the workdir file, i.e. where the files are checked out
	 */
	public File getWorkDir() {
		return getRepository().getWorkDir();
	}

	synchronized void clear() {
		db = null;
		cache = null;
		workdirPrefix = null;
		container = null;
	}

	/**
	 * @return a reference to the repository object handled by this mapping
	 */
	public synchronized Repository getRepository() {
		return db;
	}

	synchronized void setRepository(final Repository r) throws IOException {
		db = r;
		cache = new IgnoreCache(this);
		cache.initialize();

		try {
			workdirPrefix = getWorkDir().getCanonicalPath();
		} catch (IOException err) {
			workdirPrefix = getWorkDir().getAbsolutePath();
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
		p.setProperty(containerPath + ".gitdir", gitdirPath);  //$NON-NLS-1$
	}

	public String toString() {
		return "RepositoryMapping[" //$NON-NLS-1$
				+ containerPath + " -> " //$NON-NLS-1$
				+ gitdirPath + "]"; //$NON-NLS-1$
	}

	/**
	 * Check whether a resource has been changed relative to the checked out
	 * version. Content is assumed changed by this routine if the resource's
	 * modification time differs from what is recorded in the index, but the
	 * real content hasn't changed. The reason is performance.
	 *
	 * @param rsrc
	 * @return true if a resource differs in the workdir or index relative to
	 *         HEAD
	 *
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public boolean isResourceChanged(IResource rsrc) throws IOException, UnsupportedEncodingException {
		Repository repository = getRepository();
		GitIndex index = repository.getIndex();
		String repoRelativePath = getRepoRelativePath(rsrc);
		Tree headTree = repository.mapTree(Constants.HEAD);
		TreeEntry blob = headTree!=null ? headTree.findBlobMember(repoRelativePath) : null;
		Entry entry = index.getEntry(repoRelativePath);
		if (rsrc instanceof IFile && entry == null && blob == null)
			return false;
		if (entry == null)
			return true; // flags new resources as changes
		if (blob == null)
			return true; // added in index
		boolean hashesDiffer = !entry.getObjectId().equals(blob.getId());
		return hashesDiffer || entry.isModified(getWorkDir());
	}

	/**
	 * This method should only be called for resources that are actually in this
	 * repository, so we can safely assume that their path prefix matches
	 * {@link #getWorkDir()}. Testing that here is rather expensive so we don't
	 * bother.
	 *
	 * @param rsrc
	 * @return the path relative to the Git repository, including base name.
	 */
	public String getRepoRelativePath(final IResource rsrc) {
		final int pfxLen = workdirPrefix.length();
		final String p = rsrc.getLocation().toString();
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
	 * @return the name of the .git directory
	 */
	public String getGitDir() {
		return gitdirPath;
	}

	/**
	 * @return The GIT DIR absolute path
	 */
	public IPath getGitDirAbsolutePath() {
		return container.getLocation().append(getGitDirPath());
	}

	/**
	 * @return The IgnoreCache implementation for this repository mapping
	 */
	public IgnoreCache getIgnoreCache() {
		return cache;
	}


	/**
	 * Refresh a single ignore node in the cache. There is no function for
	 * deleting nodes, any node refreshed here will automatically be cleared.
	 * Nodes will not be parsed again until a call to {@link #isIgnored(IResource)}
	 * is made.
	 *
	 * @param resource
	 * 			  The .gitignore file to refresh for.
	 * @throws IOException
	 * 			  Failed to initialize cache.
	 *
	 *
	 */
	public void refreshIgnoreNode(IResource resource) throws IOException {
		if (cache != null) {
			cache.refreshNode(resource);
		}
	}

	/**
	 * Check the ignored status of the given resource. Resource must be
	 * part of a RepositoryMapping. Returns false if no mapping can be found.
	 *
	 * @param rsrc
	 * 			  Resource to check.
	 * @return True if resource is ignored. False if the resource is not
	 * ignored or if there is no RepositoryMapping for the resource
	 * @throws IOException
	 * 			  Failed to check ignore status
	 */
	public static boolean isIgnored(IResource rsrc) throws IOException {
		RepositoryMapping m = getMapping(rsrc);
		if (m != null) {
			return m.getIgnoreCache().isIgnored(m.getRepoRelativePath(rsrc));
		}
		return false;
	}

}

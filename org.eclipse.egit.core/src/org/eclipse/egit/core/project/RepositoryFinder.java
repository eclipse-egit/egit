/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2012, François Rey <eclipse.org_@_francois_._rey_._name>
 * Copyright (C) 2013, Carsten Pfeiffer <carsten.pfeiffer@gebit.de>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.SystemReader;

/**
 * Searches for existing Git repositories associated with a project's files.
 * <p>
 * This finder algorithm searches a project's contained files to see if any of
 * them are located within the working directory of an existing Git repository.
 * By default linked resources are ignored and not included in the search.
 * <p>
 * The search algorithm is exhaustive, it will find all matching repositories.
 * For the project itself and possibly for each linked container within the
 * project it scans down the local filesystem trees to locate any Git
 * repositories which may be found there. Descending into children can be
 * disabled, see {@link #setFindInChildren(boolean)}.
 * <p>
 * It also scans up the local filesystem tree to locate any Git repository which
 * may be outside of Eclipse's workspace-view of the world.
 * <p>
 * In short, if there is a Git repository associated, it finds it.
 * </p>
 */
public class RepositoryFinder {
	private final IProject proj;

	private final List<RepositoryMapping> results = new ArrayList<>();
	private final Set<File> gitdirs = new HashSet<>();

	private final Set<File> ceilingDirectories = new HashSet<>();

	private boolean findInChildren = true;

	/**
	 * Create a new finder to locate Git repositories for a project.
	 *
	 * @param p
	 *            the project this new finder should locate the existing Git
	 *            repositories of.
	 */
	public RepositoryFinder(final IProject p) {
		proj = p;
		String ceilingDirectoriesVar = SystemReader.getInstance().getenv(
				Constants.GIT_CEILING_DIRECTORIES_KEY);
		if (ceilingDirectoriesVar != null) {
			for (String path : ceilingDirectoriesVar.split(File.pathSeparator))
				ceilingDirectories.add(new File(path));
		}
	}

	/**
	 * @param findInChildren
	 *            whether children of the project should also be scanned for a
	 *            .git directory
	 * @since 3.4
	 */
	public void setFindInChildren(boolean findInChildren) {
		this.findInChildren = findInChildren;
	}

	/**
	 * Run the search algorithm, ignoring linked resources.
	 *
	 * @param m
	 *            a progress monitor to report feedback to; may be null.
	 * @return all found {@link RepositoryMapping} instances associated with the
	 *         project supplied to this instance's constructor, in the order
	 *         they were found.
	 * @throws CoreException
	 *             Eclipse was unable to access its workspace, and threw up on
	 *             us. We're throwing it back at the caller.
	 */
	public List<RepositoryMapping> find(IProgressMonitor m)
			throws CoreException {
		return find(m, false);
	}

	/**
	 * Run the search algorithm.
	 *
	 * @param m
	 *            a progress monitor to report feedback to; may be null.
	 * @param searchLinkedFolders
	 *            specify if linked folders should be included in the search
	 * @return all found {@link RepositoryMapping} instances associated with the
	 *         project supplied to this instance's constructor, in the order
	 *         they were found.
	 * @throws CoreException
	 *             Eclipse was unable to access its workspace, and threw up on
	 *             us. We're throwing it back at the caller.
	 * @since 2.3
	 */
	public List<RepositoryMapping> find(IProgressMonitor m,
			boolean searchLinkedFolders)
			throws CoreException {
		find(m, proj, searchLinkedFolders);
		return results;
	}

	private void find(final IProgressMonitor m, final IContainer c,
			boolean searchLinkedFolders)
				throws CoreException {
		if (!searchLinkedFolders && c.isLinked()) {
			return; // Ignore linked folders
		}
		final IPath loc = c.getLocation();
		if (loc == null) {
			return; // Either gone, or provided by an EFS
		}
		SubMonitor progress = SubMonitor.convert(m, 101);
		progress.subTask(CoreText.RepositoryFinder_finding);

		final File fsLoc = loc.toFile();
		assert fsLoc.isAbsolute();

		if (c instanceof IProject)
			findInDirectoryAndParents(c, fsLoc);
		else
			findInDirectory(c, fsLoc);
		progress.worked(1);

		if (findInChildren) {
			final IResource[] children = c.members();
			if (children != null && children.length > 0) {
				progress.setWorkRemaining(children.length);
				for (IResource o : children) {
					if (o instanceof IContainer
							&& !o.getName().equals(Constants.DOT_GIT)) {
						find(progress.newChild(1), (IContainer) o,
								searchLinkedFolders);
					} else {
						progress.worked(1);
					}
				}
			}
		}
	}

	private void findInDirectoryAndParents(IContainer container, File startPath) {
		File path = startPath;
		while (path != null && !ceilingDirectories.contains(path)) {
			findInDirectory(container, path);
			path = path.getParentFile();
		}
	}

	private void findInDirectory(final IContainer container,
			final File path) {
		if (GitTraceLocation.CORE.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.CORE.getLocation(),
					"Looking at candidate dir: " //$NON-NLS-1$
							+ path);

		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		File parent = path.getParentFile();
		if (parent != null)
			builder.addCeilingDirectory(parent);
		builder.findGitDir(path);
		File gitDir = builder.getGitDir();
		if (gitDir != null)
			register(container, gitDir);
	}

	private void register(final IContainer c, final File gitdir) {
		File f = gitdir.getAbsoluteFile();
		if (gitdirs.contains(f)) {
			return;
		}
		gitdirs.add(f);
		RepositoryMapping mapping = RepositoryMapping.create(c, f);
		if (mapping != null) {
			results.add(mapping);
		}
	}
}

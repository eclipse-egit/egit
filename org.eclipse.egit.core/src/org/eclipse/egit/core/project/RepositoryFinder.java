/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.SystemReader;

/**
 * Searches for existing Git repositories associated with a project's files.
 * <p>
 * This finder algorithm searches a project's contained files to see if any of
 * them are located within the working directory of an existing Git repository.
 * The finder searches through linked resources, as the EGit core is capable of
 * dealing with linked directories spanning multiple repositories in the same
 * project.
 * </p>
 * <p>
 * The search algorithm is exhaustive, it will find all matching repositories.
 * For the project itself as well as for each linked container within the
 * project it scans down the local filesystem trees to locate any Git
 * repositories which may be found there. It also scans up the local filesystem
 * tree to locate any Git repository which may be outside of Eclipse's
 * workspace-view of the world, but which contains the project or a linked
 * resource within the project. In short, if there is a Git repository
 * associated, it finds it.
 * </p>
 */
public class RepositoryFinder {
	private final IProject proj;

	private final Collection<RepositoryMapping> results = new ArrayList<RepositoryMapping>();
	private final Set<File> gitdirs = new HashSet<File>();

	private Set<String> ceilingDirectories = new HashSet<String>();

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
			ceilingDirectories.addAll(Arrays.asList(ceilingDirectoriesVar
					.split(File.pathSeparator)));
		}
	}

	/**
	 * Run the search algorithm.
	 *
	 * @param m
	 *            a progress monitor to report feedback to; may be null.
	 * @return all found {@link RepositoryMapping} instances associated with the
	 *         project supplied to this instance's constructor.
	 * @throws CoreException
	 *             Eclipse was unable to access its workspace, and threw up on
	 *             us. We're throwing it back at the caller.
	 */
	public Collection<RepositoryMapping> find(IProgressMonitor m)
			throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		find(monitor, proj);
		return results;
	}

	private void find(final IProgressMonitor m, final IContainer c)
			throws CoreException {
		if (c.isLinked()) return; // Ignore linked folders
		final IPath loc = c.getLocation();

		m.beginTask("", 101);  //$NON-NLS-1$
		m.subTask(CoreText.RepositoryFinder_finding);
		try {
			if (loc != null) {
				final File fsLoc = loc.toFile();
				assert fsLoc.isAbsolute();
				final File ownCfg = configFor(fsLoc);
				final IResource[] children;

				if (ownCfg.isFile()) {
					register(c, ownCfg.getParentFile());
				}
				if (c instanceof IProject) {
					File p = fsLoc.getParentFile();
					while (p != null) {
						// TODO is this the right location?
						if (GitTraceLocation.CORE.isActive())
							GitTraceLocation.getTrace().trace(
									GitTraceLocation.CORE.getLocation(),
									"Looking at candidate dir: " //$NON-NLS-1$
											+ p);
						final File pCfg = configFor(p);
						if (pCfg.isFile()) {
							register(c, pCfg.getParentFile());
						}
						if (ceilingDirectories.contains(p.getPath()))
							break;
						p = p.getParentFile();
					}
				}
				m.worked(1);

				children = c.members();
				if (children != null && children.length > 0) {
					final int scale = 100 / children.length;
					for (int k = 0; k < children.length; k++) {
						final IResource o = children[k];
						if (o instanceof IContainer
								&& !o.getName().equals(Constants.DOT_GIT)) {
							find(new SubProgressMonitor(m, scale),
									(IContainer) o);
						} else {
							m.worked(scale);
						}
					}
				}
			}
		} finally {
			m.done();
		}
	}

	private File configFor(final File fsLoc) {
		return new File(new File(fsLoc, Constants.DOT_GIT),
				"config");  //$NON-NLS-1$
	}

	private void register(final IContainer c, final File gitdir) {
		File f;
		try {
			f = gitdir.getCanonicalFile();
		} catch (IOException ioe) {
			f = gitdir.getAbsoluteFile();
		}
		if (gitdirs.contains(f))
			return;
		gitdirs.add(f);
		results.add(new RepositoryMapping(c, f));
	}
}

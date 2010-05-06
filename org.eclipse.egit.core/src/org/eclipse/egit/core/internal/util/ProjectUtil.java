/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.util;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.lib.Repository;

/**
 * This class contains utility methods related to projects
 * TODO: rename to RefreshUtil or ResourceUtil?
 */
public class ProjectUtil {
	/**
	 * The method refreshes all projects contained in the given Git repository
	 * @param repository
	 * @param monitor
	 * @throws CoreException
	 */
	public static void refreshProjects(Repository repository,
			IProgressMonitor monitor) throws CoreException {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		try {
			monitor.beginTask(CoreText.ProjectUtil_refreshingProjects,
					projects.length);
			final File parentFile = repository.getWorkDir();
			for (IProject p : projects) {
				if (monitor.isCanceled())
					break;
				final File file = p.getLocation().toFile();
				if (file.getAbsolutePath().startsWith(
						parentFile.getAbsolutePath())) {
					p.refreshLocal(IResource.DEPTH_INFINITE,
							new SubProgressMonitor(monitor, 1));
					monitor.worked(1);
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * The method refreshes resources
	 *
	 * @param resources
	 *            resources to refresh
	 * @param monitor
	 * @throws CoreException
	 */
	public static void refreshResources(IResource[] resources,
			IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(CoreText.ProjectUtil_refreshing,
					resources.length);
			for (IResource resource : resources) {
				if (monitor.isCanceled())
					break;
				resource.refreshLocal(IResource.DEPTH_INFINITE,
						new SubProgressMonitor(monitor, 1));
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}

	}

}

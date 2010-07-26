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
import java.util.ArrayList;
import java.util.List;

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
	 * The method returns all valid projects contained in the given Git
	 * repository. A project is considered as valid if the .project file exists.
	 * @see ProjectUtil#refreshValidProjects(IProject[], IProgressMonitor)
	 * @param repository
	 * @return valid projects
	 * @throws CoreException
	 */
	public static IProject[] getValidProjects(Repository repository)
			throws CoreException {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		List<IProject> result = new ArrayList<IProject>();
		final File parentFile = repository.getWorkTree();
		for (IProject p : projects) {
			String projectFilePath = p.getLocation()
					.append(".project").toOSString(); //$NON-NLS-1$
			File projectFile = new File(projectFilePath);
			if (projectFile.exists()) {
				final File file = p.getLocation().toFile();
				if (file.getAbsolutePath().startsWith(
						parentFile.getAbsolutePath())) {
					result.add(p);
				}
			}
		}
		return result.toArray(new IProject[result.size()]);
	}

	/**
	 * The method refreshes the given projects. Projects with missing .project
	 * file are deleted. The method should be called in the following flow:<br>
	 * <ol>
	 * <li>Call {@link ProjectUtil#getValidProjects(Repository)}
	 * <li>Perform a workdir checkout (e.g. branch, reset)
	 * <li>Call
	 * {@link ProjectUtil#refreshValidProjects(IProject[], IProgressMonitor)}
	 * </ol>
	 *
	 * @param projects
	 *            list of valid projects before workdir checkout.
	 * @param monitor
	 * @throws CoreException
	 */
	public static void refreshValidProjects(IProject[] projects,
			IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(CoreText.ProjectUtil_refreshingProjects,
					projects.length);
			for (IProject p : projects) {
				if (monitor.isCanceled())
					break;
				String projectFilePath = p.getLocation().append(".project").toOSString();  //$NON-NLS-1$
				File projectFile = new File(projectFilePath);
				if (projectFile.exists())
						p.refreshLocal(IResource.DEPTH_INFINITE,
								new SubProgressMonitor(monitor, 1));

				 else
					p.delete(false, true, new SubProgressMonitor(monitor, 1));

				monitor.worked(1);
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

/*******************************************************************************
 * Copyright (C) 2019, Peter Severin <peter@wireframesketcher.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch>
 *    Peter Severin <peter@wireframesketcher.com> - Bug 546329
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;

class DebugUIPluginFacade implements IDebugUIPluginFacade {
	@Override
	public String getRunningLaunchConfigurationName(
			final Collection<Repository> repositories,
			IProgressMonitor monitor) {
		ILaunchConfiguration launch = getRunningLaunchConfiguration(
				repositories, monitor);
		if (launch != null)
			return launch.getName();

		return null;
	}

	public static ILaunchConfiguration getRunningLaunchConfiguration(
			final Collection<Repository> repositories,
			IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		final ILaunchConfiguration[] result = { null };
		IRunnableWithProgress operation = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor m)
					throws InvocationTargetException, InterruptedException {
				Set<IProject> projects = new HashSet<>();
				for (Repository repository : repositories) {
					projects.addAll(
							Arrays.asList(ProjectUtil.getProjects(repository)));
				}
				result[0] = findLaunch(projects, m);
			}
		};
		try {
			if (ModalContext.isModalContextThread(Thread.currentThread())) {
				operation.run(progress);
			} else {
				ModalContext.run(operation, true, progress,
						PlatformUI.getWorkbench().getDisplay());
			}
		} catch (InvocationTargetException e) {
			// ignore
		} catch (InterruptedException e) {
			// ignore
		}
		return result[0];
	}

	private static ILaunchConfiguration findLaunch(Set<IProject> projects,
			IProgressMonitor monitor) {
		ILaunchManager launchManager = DebugPlugin.getDefault()
				.getLaunchManager();
		ILaunch[] launches = launchManager.getLaunches();
		SubMonitor progress = SubMonitor.convert(monitor,
				UIText.LaunchFinder_SearchLaunchConfiguration, launches.length);
		for (ILaunch launch : launches) {
			if (progress.isCanceled()) {
				break;
			}
			if (launch.isTerminated()) {
				progress.worked(1);
				continue;
			}
			ISourceLocator locator = launch.getSourceLocator();
			if (locator instanceof ISourceLookupDirector) {
				ISourceLookupDirector director = (ISourceLookupDirector) locator;
				ISourceContainer[] containers = director.getSourceContainers();
				if (isAnyProjectInSourceContainers(containers, projects,
						progress.newChild(1))) {
					return launch.getLaunchConfiguration();
				}
			} else {
				progress.worked(1);
			}
		}
		return null;
	}

	private static boolean isAnyProjectInSourceContainers(
			ISourceContainer[] containers, Set<IProject> projects,
			IProgressMonitor monitor) {
		if (containers == null) {
			return false;
		}
		SubMonitor progress = SubMonitor.convert(monitor, containers.length);
		for (ISourceContainer container : containers) {
			if (progress.isCanceled()) {
				break;
			}
			if (container instanceof ProjectSourceContainer) {
				ProjectSourceContainer projectContainer = (ProjectSourceContainer) container;
				if (projects.contains(projectContainer.getProject())) {
					progress.worked(1);
					return true;
				}
			}
			try {
				boolean found = isAnyProjectInSourceContainers(
						container.getSourceContainers(), projects,
						progress.newChild(1));
				if (found) {
					return true;
				}
			} catch (CoreException e) {
				// Ignore the child source containers, continue search
			}
		}
		return false;
	}
}

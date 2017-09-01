/*******************************************************************************
 * Copyright (C) 2011, 2014 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * Utilities for creating (importing) projects
 */
public class ProjectUtils {
	/**
	 * Create (import) a set of existing projects. The projects are
	 * automatically connected to the repository they reside in.
	 *
	 * @param projectsToCreate
	 *            the projects to create
	 * @param selectedWorkingSets
	 *            the workings sets to add the created projects to, may be null
	 *            or empty
	 * @param monitor
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public static void createProjects(
			final Set<ProjectRecord> projectsToCreate,
			final IWorkingSet[] selectedWorkingSets, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		createProjects(projectsToCreate, false, selectedWorkingSets, monitor);
	}

	/**
	 * Create (import) a set of existing projects. The projects are
	 * automatically connected to the repository they reside in.
	 *
	 * @param projectsToCreate
	 *            the projects to create
	 * @param open
	 *            true to open existing projects, false to leave in current
	 *            state
	 * @param selectedWorkingSets
	 *            the workings sets to add the created projects to, may be null
	 *            or empty
	 * @param monitor
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public static void createProjects(final Set<ProjectRecord> projectsToCreate,
			final boolean open, final IWorkingSet[] selectedWorkingSets,
			IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		if (projectsToCreate.isEmpty()) {
			return;
		}
		IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor actMonitor) throws CoreException {
				IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
						.getWorkingSetManager();
				if (actMonitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				Map<IProject, File> projectsToConnect = new HashMap<>();
				SubMonitor progress = SubMonitor.convert(actMonitor,
						projectsToCreate.size() * 2 + 1);
				for (ProjectRecord projectRecord : projectsToCreate) {
					if (progress.isCanceled()) {
						throw new OperationCanceledException();
					}
					progress.setTaskName(projectRecord.getProjectLabel());
					IProject project = createExistingProject(projectRecord,
							open, progress.newChild(1));
					if (project == null) {
						continue;
					}

					RepositoryFinder finder = new RepositoryFinder(project);
					finder.setFindInChildren(false);
					Collection<RepositoryMapping> mappings = finder
							.find(progress.newChild(1));
					if (!mappings.isEmpty()) {
						RepositoryMapping mapping = mappings.iterator().next();
						IPath absolutePath = mapping.getGitDirAbsolutePath();
						if (absolutePath != null) {
							projectsToConnect.put(project,
									absolutePath.toFile());
						}
					}

					if (selectedWorkingSets != null
							&& selectedWorkingSets.length > 0) {
						workingSetManager.addToWorkingSets(project,
								selectedWorkingSets);
					}
				}

				if (!projectsToConnect.isEmpty()) {
					ConnectProviderOperation connect = new ConnectProviderOperation(
							projectsToConnect);
					connect.execute(progress.newChild(1));
				}
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(wsr, monitor);
		} catch (OperationCanceledException e) {
			throw new InterruptedException();
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private static IProject createExistingProject(final ProjectRecord record,
			final boolean open, IProgressMonitor monitor) throws CoreException {
		String projectName = record.getProjectName();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(projectName);
		if (project.exists()) {
			if (open && !project.isOpen()) {
				SubMonitor progress = SubMonitor.convert(monitor, 2);
				IPath location = project.getFile(
						IProjectDescription.DESCRIPTION_FILE_NAME)
						.getLocation();
				if (location != null
						&& location.toFile().equals(
								record.getProjectSystemFile())) {
					project.open(progress.newChild(1));
					project.refreshLocal(IResource.DEPTH_INFINITE,
							progress.newChild(1));
				}
			}
			return null;
		}
		if (record.getProjectDescription() == null) {
			// error case; should not occur.
			String message = MessageFormat.format(
					UIText.ProjectUtils_Invalid_ProjectFile,
					record.getProjectSystemFile(), projectName);
			Activator.logError(message, new IllegalStateException(message));
			return null;
		}
		record.getProjectDescription().setName(projectName);

		SubMonitor progress = SubMonitor.convert(monitor,
				UIText.WizardProjectsImportPage_CreateProjectsTask, 8);
		project.create(record.getProjectDescription(), progress.newChild(3));
		project.open(IResource.BACKGROUND_REFRESH, progress.newChild(5));
		return project;
	}
}

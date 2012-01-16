/*******************************************************************************
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.ProjectReference;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * Processes project references, clones and imports them.
 */
public class ProjectReferenceImporter {

	private final String[] referenceStrings;

	/**
	 * @param referenceStrings the reference strings to import
	 */
	public ProjectReferenceImporter(String[] referenceStrings) {
		this.referenceStrings = referenceStrings;
	}

	/**
	 * Imports the projects as described in the reference strings.
	 *
	 * @param monitor progress monitor
	 * @return the imported projects
	 * @throws TeamException
	 */
	public List<IProject> run(IProgressMonitor monitor) throws TeamException {

		final Map<URIish, Map<String, Set<ProjectReference>>> repositories = parseReferenceStrings();

		final List<IProject> importedProjects = new ArrayList<IProject>();

		for (final Map.Entry<URIish, Map<String, Set<ProjectReference>>> entry : repositories
				.entrySet()) {
			final URIish gitUrl = entry.getKey();
			final Map<String, Set<ProjectReference>> branches = entry
					.getValue();

			for (final Map.Entry<String, Set<ProjectReference>> branchEntry : branches
					.entrySet()) {
				final String branch = branchEntry.getKey();
				final Set<ProjectReference> projects = branchEntry.getValue();

				final IPath workDir = getWorkingDir(gitUrl, branch,
						branches.keySet());
				if (workDir.toFile().exists()) {
					final Collection<String> projectNames = new LinkedList<String>();
					for (final ProjectReference projectReference : projects)
						projectNames.add(projectReference.getProjectDir());
					throw new TeamException(
							NLS.bind(
									CoreText.GitProjectSetCapability_CloneToExistingDirectory,
									new Object[] { workDir, projectNames,
											gitUrl }));
				}

				try {
					int timeout = 60;
					String refName = Constants.R_HEADS + branch;
					final CloneOperation cloneOperation = new CloneOperation(
							gitUrl, true, null, workDir.toFile(), refName,
							Constants.DEFAULT_REMOTE_NAME, timeout);
					cloneOperation.run(monitor);

					final File repositoryPath = workDir.append(
							Constants.DOT_GIT_EXT).toFile();

					Activator.getDefault().getRepositoryUtil()
							.addConfiguredRepository(repositoryPath);

					List<IProject> p = importProjects(projects, workDir,
							repositoryPath, monitor);
					importedProjects.addAll(p);
				} catch (final InvocationTargetException e) {
					throw getTeamException(e);

				} catch (final InterruptedException e) {
					// was canceled by user
					return Collections.emptyList();
				}
			}
		}
		return importedProjects;
	}

	private Map<URIish, Map<String, Set<ProjectReference>>> parseReferenceStrings()
			throws TeamException {
		final Map<URIish, Map<String, Set<ProjectReference>>> repositories = new LinkedHashMap<URIish, Map<String, Set<ProjectReference>>>();

		for (final String reference : referenceStrings) {
			try {
				final ProjectReference projectReference = new ProjectReference(
						reference);
				Map<String, Set<ProjectReference>> repositoryBranches = repositories
						.get(projectReference.getRepository());
				if (repositoryBranches == null) {
					repositoryBranches = new HashMap<String, Set<ProjectReference>>();
					repositories.put(projectReference.getRepository(),
							repositoryBranches);
				}
				Set<ProjectReference> projectReferences = repositoryBranches
						.get(projectReference.getBranch());
				if (projectReferences == null) {
					projectReferences = new LinkedHashSet<ProjectReference>();
					repositoryBranches.put(projectReference.getBranch(),
							projectReferences);
				}

				projectReferences.add(projectReference);
			} catch (final IllegalArgumentException e) {
				throw new TeamException(reference, e);
			} catch (final URISyntaxException e) {
				throw new TeamException(reference, e);
			}
		}

		return repositories;
	}

	/**
	 * @param gitUrl
	 * @param branch
	 *            the branch to check out
	 * @param allBranches
	 *            all branches which should be checked out for this gitUrl
	 * @return the directory where the project should be checked out
	 */
	private static IPath getWorkingDir(URIish gitUrl, String branch,
			Set<String> allBranches) {
		final IPath workspaceLocation = ResourcesPlugin.getWorkspace()
				.getRoot().getRawLocation();
		final String humanishName = gitUrl.getHumanishName();
		String extendedName;
		if (allBranches.size() == 1 || branch.equals(Constants.MASTER))
			extendedName = humanishName;
		else
			extendedName = humanishName + "_" + branch; //$NON-NLS-1$
		final IPath workDir = workspaceLocation.append(extendedName);
		return workDir;
	}

	private List<IProject> importProjects(final Set<ProjectReference> projects,
			final IPath workDir, final File repositoryPath,
			final IProgressMonitor monitor) throws TeamException {
		try {

			List<IProject> importedProjects = new ArrayList<IProject>();

			// import projects from the current repository to workspace
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final IWorkspaceRoot root = workspace.getRoot();
			for (final ProjectReference projectToImport : projects) {
				final IPath projectDir = workDir.append(projectToImport
						.getProjectDir());
				final IProjectDescription projectDescription = workspace
						.loadProjectDescription(projectDir
								.append(IProjectDescription.DESCRIPTION_FILE_NAME));
				final IProject project = root.getProject(projectDescription
						.getName());
				project.create(projectDescription, monitor);
				importedProjects.add(project);

				project.open(monitor);
				final ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation(
						project, repositoryPath);
				connectProviderOperation.execute(monitor);
			}

			return importedProjects;

		} catch (final CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	private TeamException getTeamException(final Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null)
			current = current.getCause();
		return new TeamException(current.getMessage(), current);
	}
}

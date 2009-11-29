/*******************************************************************************
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;

/**
 * Serializer/deserializer for references to projects.
 */
public final class GitProjectSetCapability extends ProjectSetCapability {
	private final class ProjectReferenceComparator implements
			Comparator<ProjectReference> {
		public int compare(ProjectReference o1, ProjectReference o2) {
			final boolean reposEqual = o1.repository.equals(o2.repository);
			final boolean branchesEqual = o1.branchToCloneFrom
					.equals(o2.branchToCloneFrom);
			final boolean projectDirsEqual = o1.projectDir
					.equals(o2.projectDir);
			return reposEqual && branchesEqual && projectDirsEqual ? 0 : 1;
		}
	}

	final class ProjectReference {
		/** Separator char */
		private static final String SEP_RE = "\\|";

		private static final String DEFAULT_BRANCH = Constants.R_HEADS
				+ Constants.MASTER;

		/**
		 * a relative path (from the repository root) to a project
		 */
		String projectDir;

		/**
		 * <code>repository</code> parameter
		 */
		URIish repository;

		/**
		 * the remote branch that will be checked out, see <code>--branch</code>
		 * option
		 */
		String branchToCloneFrom = DEFAULT_BRANCH;

		/**
		 * use this name instead of using the remote name origin to keep track
		 * of the upstream repository, see <code>--origin</code> option.
		 */
		String origin = Constants.DEFAULT_REMOTE_NAME;

		ProjectReference(String reference) throws URISyntaxException {
			String[] tokens = reference.split(SEP_RE);
			if (tokens.length != 3) {
				throw new IllegalArgumentException();
			}
			this.repository = new URIish(tokens[0]);
			if (!tokens[1].equals("")) {
				this.branchToCloneFrom = tokens[1];
			}
			this.projectDir = tokens[2];
		}
	}

	@Override
	public IProject[] addToWorkspace(String[] referenceStrings,
			ProjectSetSerializationContext context, IProgressMonitor monitor)
			throws TeamException {
		Map<URIish, Set<ProjectReference>> repositories = new HashMap<URIish, Set<ProjectReference>>();
		for (String reference : referenceStrings) {
			try {
				ProjectReference projectReference = new ProjectReference(
						reference);
				Set<ProjectReference> projectsInRepository = repositories
						.get(projectReference.repository);
				if (projectsInRepository == null) {
					projectsInRepository = new TreeSet<ProjectReference>(
							new ProjectReferenceComparator());
					repositories.put(projectReference.repository,
							projectsInRepository);
				}
				projectsInRepository.add(projectReference);
			} catch (IllegalArgumentException e) {
				// TODO add a message?
				throw new TeamException(reference, e);
			} catch (URISyntaxException e) {
				// TODO add a message?
				throw new TeamException("", e);
			}
		}
		ArrayList<IProject> importedProjects = new ArrayList<IProject>();
		for (URIish gitUrl : repositories.keySet()) {
			try {
				final IPath workDir = getWorkingDir(gitUrl);

				final CloneOperation cloneOperation = new CloneOperation(
						gitUrl, true, null, workDir.toFile(),
						ProjectReference.DEFAULT_BRANCH, Constants.DEFAULT_REMOTE_NAME);
				cloneOperation.run(monitor);

				// import projects from the current repository to workspace
				Set<ProjectReference> projects = repositories.get(gitUrl);
				final IWorkspace workspace = ResourcesPlugin.getWorkspace();
				final IWorkspaceRoot root = workspace.getRoot();
				for (ProjectReference projectToImport : projects) {
					final IPath repoLocation = workDir.append(projectToImport.projectDir);
					final IProjectDescription projectDescription = workspace
							.loadProjectDescription(repoLocation
									.append(IProjectDescription.DESCRIPTION_FILE_NAME));
					IProject project = root.getProject(projectDescription.getName());
					project.create(projectDescription, monitor);
					importedProjects.add(project);

					project.open(monitor);
					final ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation(
							project, workDir.append(".git").makeRelativeTo(project.getLocation()).toFile());
					connectProviderOperation.run(monitor);
				}
			} catch (InvocationTargetException e) {
				throw TeamException.asTeamException(e);
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			} catch (InterruptedException e) {
				// TODO add message
				throw new TeamException("", e);
			}
		}
		final IProject[] result = importedProjects
				.toArray(new IProject[importedProjects.size()]);
		return result;
	}

	/**
	 * @param gitUrl
	 * @return the directory where the project should be checked out
	 */
	private static IPath getWorkingDir(URIish gitUrl) {
		final IPath workspaceLocation = ResourcesPlugin.getWorkspace()
				.getRoot().getRawLocation();
		final String humanishName = gitUrl.getHumanishName();
		// TODO ask user where to checkout
		final IPath workDir = workspaceLocation.append(humanishName);
		return workDir;
	}

}

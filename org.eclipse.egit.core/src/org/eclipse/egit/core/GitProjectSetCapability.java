/*******************************************************************************
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manuel Doninger <manuel.doninger@googlemail.com>
 *     Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.GitURI;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;

/**
 * Capability for exporting and importing projects shared with Git as part of a
 * team project set.
 */
public final class GitProjectSetCapability extends ProjectSetCapability {

	private static final String VERSION = "1.0"; //$NON-NLS-1$

	@Override
	public String[] asReference(IProject[] projects,
			ProjectSetSerializationContext context, IProgressMonitor monitor)
			throws TeamException {
		String[] references = new String[projects.length];
		for (int i = 0; i < projects.length; i++)
			references[i] = asReference(projects[i]);
		return references;
	}

	private String asReference(IProject project) throws TeamException {
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		String branch;
		try {
			branch = mapping.getRepository().getBranch();
		} catch (IOException e) {
			throw new TeamException(NLS.bind(
					CoreText.GitProjectSetCapability_ExportCouldNotGetBranch,
					project.getName()));
		}
		StoredConfig config = mapping.getRepository().getConfig();
		String remote = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION,
				branch, ConfigConstants.CONFIG_KEY_REMOTE);
		String url = config.getString(ConfigConstants.CONFIG_REMOTE_SECTION,
				remote, ConfigConstants.CONFIG_KEY_URL);
		if (url == null)
			throw new TeamException(NLS.bind(
					CoreText.GitProjectSetCapability_ExportNoRemote,
					project.getName()));

		String projectPath = mapping.getRepoRelativePath(project);
		if (projectPath.equals("")) //$NON-NLS-1$
			projectPath = "."; //$NON-NLS-1$

		return asReference(url, branch, projectPath);
	}

	private String asReference(String url, String branch, String projectPath) {
		StringBuilder sb = new StringBuilder();

		sb.append(VERSION);
		sb.append(ProjectReference.SEPARATOR);
		sb.append(url);
		sb.append(ProjectReference.SEPARATOR);
		sb.append(branch);
		sb.append(ProjectReference.SEPARATOR);
		sb.append(projectPath);

		return sb.toString();
	}

	@Override
	public IProject[] addToWorkspace(final String[] referenceStrings,
			final ProjectSetSerializationContext context,
			final IProgressMonitor monitor) throws TeamException {
		final ArrayList<IProject> importedProjects = new ArrayList<IProject>();

		try{
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

				public void run(IProgressMonitor wsOpMonitor) throws CoreException {
					final Map<URIish, Map<String, Set<ProjectReference>>> repositories =
						new LinkedHashMap<URIish, Map<String, Set<ProjectReference>>>();
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
							Set<ProjectReference> projectReferences = repositoryBranches.get(projectReference.getBranch());
							if (projectReferences == null) {
								projectReferences = new LinkedHashSet<ProjectReference>();
								repositoryBranches.put(projectReference.getBranch(), projectReferences);
							}

							projectReferences.add(projectReference);
						} catch (final IllegalArgumentException e) {
							throw new TeamException(reference, e);
						} catch (final URISyntaxException e) {
							throw new TeamException(reference, e);
						}
					}
					for (final Map.Entry<URIish, Map<String, Set<ProjectReference>>> entry : repositories.entrySet()) {
						final URIish gitUrl = entry.getKey();
						final Map<String, Set<ProjectReference>> branches = entry.getValue();

						for (final Map.Entry<String, Set<ProjectReference>> branchEntry : branches.entrySet()) {
							final String branch = branchEntry.getKey();
							final Set<ProjectReference> projects = branchEntry.getValue();

							try {
								final IPath workDir = getWorkingDir(gitUrl, branch,
										branches.keySet());
								if (workDir.toFile().exists()) {
									final Collection<String> projectNames = new LinkedList<String>();
									for (final ProjectReference projectReference : projects)
										projectNames.add(projectReference.getProjectDir());
									throw new TeamException(NLS.bind(
											CoreText.GitProjectSetCapability_CloneToExistingDirectory,
											new Object[] { workDir, projectNames, gitUrl }));
								}

								int timeout = 60;
								String refName = Constants.R_HEADS + branch;
								final CloneOperation cloneOperation = new CloneOperation(
										gitUrl, true, null, workDir.toFile(), refName,
										Constants.DEFAULT_REMOTE_NAME, timeout);
								cloneOperation.run(wsOpMonitor);

								final File repositoryPath = workDir.append(Constants.DOT_GIT_EXT).toFile();

								Activator.getDefault().getRepositoryUtil().addConfiguredRepository(repositoryPath);

								// import projects from the current repository to workspace
								final IWorkspace workspace = ResourcesPlugin.getWorkspace();
								final IWorkspaceRoot root = workspace.getRoot();
								for (final ProjectReference projectToImport : projects) {
									final IPath projectDir = workDir
											.append(projectToImport.getProjectDir());
									final IProjectDescription projectDescription = workspace
											.loadProjectDescription(projectDir
													.append(IProjectDescription.DESCRIPTION_FILE_NAME));
									final IProject project = root
											.getProject(projectDescription.getName());
									project.create(projectDescription, wsOpMonitor);
									importedProjects.add(project);

									project.open(wsOpMonitor);
									final ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation(
											project, repositoryPath);
									connectProviderOperation.execute(wsOpMonitor);
								}
							} catch (final InvocationTargetException e) {
								throwTeamException(e);
							} catch (final CoreException e) {
								throw TeamException.asTeamException(e);
							} catch (final InterruptedException e) {
								// was canceled by user
								importedProjects.clear();
							}
						}
					}

				}
			}, ResourcesPlugin.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
		final IProject[] result = importedProjects
				.toArray(new IProject[importedProjects.size()]);
		return result;
	}

	@Override
	public String asReference(URI uri, String projectName) {
		GitURI gitURI = new GitURI(uri);
		return asReference(gitURI.getRepository().toString(), gitURI.getTag(), gitURI.getPath().toString());
	}

	private TeamException throwTeamException(Throwable th) throws TeamException{
		Throwable current = th;
		while(current.getCause()!=null){
			current = current.getCause();
		}
		throw new TeamException(current.getMessage(), current);
	}

	/**
	 * @param gitUrl
	 * @param branch the branch to check out
	 * @param allBranches all branches which should be checked out for this gitUrl
	 * @return the directory where the project should be checked out
	 */
	private static IPath getWorkingDir(URIish gitUrl, String branch, Set<String> allBranches) {
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

}
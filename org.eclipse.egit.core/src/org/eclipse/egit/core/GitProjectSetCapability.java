/*******************************************************************************
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2011, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Manuel Doninger <manuel.doninger@googlemail.com>
 *     Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.GitURI;
import org.eclipse.egit.core.internal.ProjectReferenceImporter;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.StoredConfig;
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
		List<String> references = new ArrayList<>(projects.length);
		for (IProject project : projects) {
			String reference = asReference(project);
			if(reference != null){
				references.add(reference);
			}
		}
		return references.toArray(new String[0]);
	}

	@Nullable
	private String asReference(IProject project) throws TeamException {
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null) {
			return null;
		}
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
		if (projectPath == null) {
			return null;
		}
		if (projectPath.isEmpty())
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
		final ArrayList<IProject> importedProjects = new ArrayList<>();

		try{
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor wsOpMonitor) throws CoreException {
					ProjectReferenceImporter importer = new ProjectReferenceImporter(referenceStrings);
					List<IProject> p = importer.run(wsOpMonitor);
					importedProjects.addAll(p);
				}
			}, ResourcesPlugin.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
		final IProject[] result = importedProjects
				.toArray(new IProject[0]);
		return result;
	}

	@Nullable
	@Override
	public String asReference(URI uri, String projectName) {
		try {
			GitURI gitURI = new GitURI(uri);
			return asReference(gitURI.getRepository().toString(),
					gitURI.getTag(), gitURI.getPath().toString());
		} catch (IllegalArgumentException e) {
			Activator.logError(e.getMessage(), e);
			// we must not fail but return null on invalid or unknown URI's.
			return null;
		}
	}
}

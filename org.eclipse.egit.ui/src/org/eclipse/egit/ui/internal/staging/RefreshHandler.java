/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The handler for the <code>org.eclipse.ui.file.refresh</code> command in the
 * 'Staging' view.
 */
public class RefreshHandler extends AbstractHandler {

	private void addProject(Map<Repository, Set<IProject>> versionedProjects,
			IResource resource) {
		RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
		Repository repository = mapping.getRepository();
		Set<IProject> projects = versionedProjects.get(repository);
		if (projects == null) {
			projects = new HashSet<IProject>();
			versionedProjects.put(repository, projects);
		}
		projects.add(resource.getProject());
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart activePart = HandlerUtil.getActivePartChecked(event);
		if (activePart instanceof StagingView) {
			IStructuredSelection selection = (IStructuredSelection) HandlerUtil
					.getCurrentSelection(event);

			Map<Repository, Set<IProject>> versionedProjects = new HashMap<Repository, Set<IProject>>();
			for (Object object : selection.toArray()) {
				if (object instanceof IFile) {
					IFile file = (IFile) object;
					addProject(versionedProjects, file);
				} else if (object instanceof ResourceNode) {
					ResourceNode node = (ResourceNode) object;
					addProject(versionedProjects, node.getResource());
				} else {
					Repository repository = object instanceof Repository ? (Repository) object
							: ((StatusNode) object).getRepository();
					Set<IProject> projects = versionedProjects.get(repository);
					if (projects == null) {
						projects = new HashSet<IProject>();
						versionedProjects.put(repository, projects);
					}

					for (IProject project : ResourcesPlugin.getWorkspace()
							.getRoot().getProjects()) {
						RepositoryMapping mapping = RepositoryMapping
								.getMapping(project);
						if (mapping != null
								&& mapping.getRepository() == repository) {
							projects.add(project);
						}
					}
				}
			}

			((StagingView) activePart).scheduleRepositoryAnalysis(
					versionedProjects, false);
		}
		return null;
	}

}

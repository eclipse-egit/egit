/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 * Copyright (C) 2019, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.events.WorkingTreeModifiedEvent;
import org.eclipse.jgit.events.WorkingTreeModifiedListener;
import org.eclipse.jgit.lib.Repository;

/**
 * Refreshes parts of the workspace changed by JGit operations. This will
 * not refresh any git-ignored resources since those are not reported in the
 * {@link WorkingTreeModifiedEvent}.
 */
public class ResourceRefreshHandler implements WorkingTreeModifiedListener {

	@Override
	public void onWorkingTreeModified(WorkingTreeModifiedEvent event) {
		if (event.isEmpty()) {
			return;
		}
		Repository repo = event.getRepository();
		if (repo == null || repo.isBare()) {
			return; // Should never occur
		}
		if (GitTraceLocation.REFRESH.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REFRESH.getLocation(),
					"Triggered refresh for repo: " + repo); //$NON-NLS-1$
		}
		try {
			refreshRepository(event);
		} catch (OperationCanceledException oe) {
			return;
		} catch (CoreException e) {
			Activator.error(CoreText.Activator_refreshFailed, e);
		}
	}

	private void refreshRepository(WorkingTreeModifiedEvent event)
			throws CoreException {
		if (event.isEmpty()) {
			return; // Should actually not occur
		}
		File workTree = event.getRepository().getWorkTree().getAbsoluteFile();
		Map<IPath, IProject> roots = getProjectLocations(workTree);
		if (roots.isEmpty()) {
			// No open projects from this repository in the workspace
			return;
		}
		IPath wt = new Path(workTree.getPath());
		Map<IResource, Boolean> toRefresh = computeResources(
				event.getModified(), event.getDeleted(), wt, roots);
		if (toRefresh.isEmpty()) {
			return;
		}
		if (GitTraceLocation.REFRESH.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REFRESH.getLocation(),
					"Refreshing repository " + workTree + ' ' //$NON-NLS-1$
							+ toRefresh.size());
		}
		for (Map.Entry<IResource, Boolean> entry : toRefresh.entrySet()) {
			entry.getKey()
					.refreshLocal(entry.getValue().booleanValue()
							? IResource.DEPTH_INFINITE
							: IResource.DEPTH_ONE, null);
		}
		if (GitTraceLocation.REFRESH.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REFRESH.getLocation(),
					"Refreshed repository " + workTree + ' ' //$NON-NLS-1$
							+ toRefresh.size());
		}
	}

	private Map<IPath, IProject> getProjectLocations(File workTree) {
		IProject[] projects = RuleUtil.getProjects(workTree);
		if (projects == null) {
			return Collections.emptyMap();
		}
		Map<IPath, IProject> result = new HashMap<>();
		for (IProject project : projects) {
			if (project.isAccessible()) {
				IPath path = project.getLocation();
				if (path != null) {
					IPath projectFilePath = path.append(
							IProjectDescription.DESCRIPTION_FILE_NAME);
					if (projectFilePath.toFile().exists()) {
						result.put(path, project);
					}
				}
			}
		}
		return result;
	}

	private Map<IResource, Boolean> computeResources(
			Collection<String> modified, Collection<String> deleted,
			IPath workTree, Map<IPath, IProject> roots) {
		// Attempt to minimize the refreshes by returning IContainers if
		// more than one file in a container has changed.
		if (GitTraceLocation.REFRESH.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REFRESH.getLocation(),
					"Calculating refresh for repository " + workTree + ' ' //$NON-NLS-1$
							+ modified.size() + ' ' + deleted.size());
		}
		Set<IPath> fullRefreshes = new HashSet<>();
		Map<IPath, IFile> handled = new HashMap<>();
		Map<IResource, Boolean> result = new HashMap<>();
		Stream.concat(modified.stream(), deleted.stream()).forEach(path -> {
			IPath filePath = "/".equals(path) ? workTree //$NON-NLS-1$
					: workTree.append(path);
			IProject project = roots.get(filePath);
			if (project != null) {
				// Eclipse knows this as a project. Make sure it gets
				// refreshed as such. One can refresh a folder via an IFile,
				// but not an IProject.
				handled.put(filePath, null);
				result.put(project, Boolean.FALSE);
				return;
			}
			if (fullRefreshes.stream()
					.anyMatch(full -> full.isPrefixOf(filePath))
					|| !roots.keySet().stream()
							.anyMatch(root -> root.isPrefixOf(filePath))) {
				// Not in workspace or covered by a full container refresh
				return;
			}
			IPath containerPath;
			boolean isFile;
			if (path.endsWith("/")) { //$NON-NLS-1$
				// It's already a directory
				isFile = false;
				containerPath = filePath.removeTrailingSeparator();
			} else {
				isFile = true;
				containerPath = filePath.removeLastSegments(1);
			}
			if (!handled.containsKey(containerPath)) {
				if (!isFile && containerPath != null) {
					IContainer container = getContainerForLocation(
							containerPath);
					if (container != null) {
						IFile file = handled.get(containerPath);
						handled.put(containerPath, null);
						if (file != null) {
							result.remove(file);
						}
						result.put(container, Boolean.FALSE);
					}
				} else if (isFile) {
					// First file in this container. Find the deepest
					// existing container and record its child.
					String lastPart = filePath.lastSegment();
					while (containerPath != null
							&& workTree.isPrefixOf(containerPath)) {
						IContainer container = getContainerForLocation(
								containerPath);
						if (container == null) {
							lastPart = containerPath.lastSegment();
							containerPath = containerPath
									.removeLastSegments(1);
							isFile = false;
							continue;
						}
						if (container.getType() == IResource.ROOT) {
							// Missing project... ignore it and anything
							// beneath. The user or our own branch project
							// tracker will have to properly add/import the
							// project.
							containerPath = containerPath.append(lastPart);
							fullRefreshes.add(containerPath);
							handled.put(containerPath, null);
						} else if (isFile) {
							IFile file = container
									.getFile(new Path(lastPart));
							handled.put(containerPath, file);
							result.put(file, Boolean.FALSE);
						} else {
							// New or deleted folder.
							container = container
									.getFolder(new Path(lastPart));
							containerPath = containerPath.append(lastPart);
							fullRefreshes.add(containerPath);
							handled.put(containerPath, null);
							result.put(container, Boolean.TRUE);
						}
						break;
					}
				}
			} else {
				IFile file = handled.get(containerPath);
				if (file != null) {
					// Second file in this container: replace file by
					// its container.
					handled.put(containerPath, null);
					result.remove(file);
					result.put(file.getParent(), Boolean.FALSE);
				}
				// Otherwise we already have this container.
			}
		});

		if (GitTraceLocation.REFRESH.isActive()) {
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.REFRESH.getLocation(),
					"Calculated refresh for repository " + workTree); //$NON-NLS-1$
		}
		return result;
	}

	private static IContainer getContainerForLocation(@NonNull IPath location) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer dir = root.getContainerForLocation(location);
		if (dir == null) {
			return null;
		}
		if (isValid(dir)) {
			return dir;
		}
		URI uri = URIUtil.toURI(location);
		IContainer[] containers = root.findContainersForLocationURI(uri);
		return Arrays.stream(containers).filter(ResourceRefreshHandler::isValid)
				.findFirst().orElse(null);
	}

	private static boolean isValid(@NonNull IResource resource) {
		return resource.isAccessible()
				&& !resource.isLinked(IResource.CHECK_ANCESTORS);
	}

}
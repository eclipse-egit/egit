/*******************************************************************************
 * Copyright (C) 2010, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

/**
 *
 */
public class GitSynchronizeDataSet implements Iterable<GitSynchronizeData> {

	private boolean containsFolderLevelSynchronizationRequest = false;

	private final Set<GitSynchronizeData> gsdSet;

	private final Map<String, GitSynchronizeData> projectMapping;

	private final boolean forceFetch;

	/**
	 * Constructs GitSynchronizeDataSet.
	 */
	public GitSynchronizeDataSet() {
		this(false);
	}

	/**
	 * Constructs GitSynchronizeDataSet.
	 *
	 * @param forceFetch
	 *            {@code true} for forcing fetch action before synchronization
	 */
	public GitSynchronizeDataSet(boolean forceFetch) {
		this.forceFetch = forceFetch;
		gsdSet = new HashSet<>();
		projectMapping = new HashMap<>();
	}

	/**
	 * Constructs GitSynchronizeDataSet and adds given element to set.
	 *
	 * @param data
	 */
	public GitSynchronizeDataSet(GitSynchronizeData data) {
		this();
		add(data);
	}

	/**
	 * @param data
	 */
	public void add(GitSynchronizeData data) {
		gsdSet.add(data);
		if (data.getIncludedResources() != null
				&& data.getIncludedResources().size() > 0)
			containsFolderLevelSynchronizationRequest = true;

		for (IProject proj : data.getProjects()) {
			projectMapping.put(proj.getName(), data);
		}
	}

	/**
	 * @param project
	 * @return <code>true</code> if project has corresponding data
	 */
	public boolean contains(IProject project) {
		return projectMapping.containsKey(project.getName());
	}

	/**
	 * @return {@code true} when at least one {@link GitSynchronizeData} is
	 *         configured to include changes only for particular folder,
	 *         {@code false} otherwise
	 */
	public boolean containsFolderLevelSynchronizationRequest() {
		return containsFolderLevelSynchronizationRequest;
	}

	/**
	 * @return number of {@link GitSynchronizeData} that are included in this
	 *         set
	 */
	public int size() {
		return gsdSet.size();
	}

	/**
	 * @param projectName
	 * @return <code>null</code> if project does not have corresponding data
	 */
	public GitSynchronizeData getData(String projectName) {
		return projectMapping.get(projectName);
	}

	/**
	 * @param project
	 * @return <code>null</code> if project does not have corresponding data
	 */
	public GitSynchronizeData getData(IProject project) {
		return projectMapping.get(project.getName());
	}

	@Override
	public Iterator<GitSynchronizeData> iterator() {
		return gsdSet.iterator();
	}

	/**
	 * @return list of all resources
	 */
	public IProject[] getAllProjects() {
		Set<IProject> resource = new HashSet<>();
		for (GitSynchronizeData data : gsdSet) {
			resource.addAll(data.getProjects());
		}
		return resource.toArray(new IProject[0]);
	}

	/**
	 * @param res
	 * @return whether the given resource should be included in the
	 *         synchronization.
	 */
	public boolean shouldBeIncluded(IResource res) {
		final IProject project = res.getProject();
		if (project == null)
			return false;

		final GitSynchronizeData syncData = getData(project);
		if (syncData == null)
			return false;

		final Set<IResource> includedResources = syncData
				.getIncludedResources();
		if (includedResources == null)
			return true;

		IPath path = res.getLocation();
		if (path != null) {
			for (IResource resource : includedResources) {
				IPath inclResourceLocation = resource.getLocation();
				if (inclResourceLocation != null
						&& inclResourceLocation.isPrefixOf(path))
					return true;
			}
		}
		return false;
	}

	/**
	 * @return {@code true} when fetch action should be forced before
	 *         synchronization, {@code false} otherwise.
	 */
	public boolean forceFetch() {
		return forceFetch;
	}


	/**
	 * Disposes all nested resources
	 */
	public void dispose() {
		if (projectMapping != null)
			projectMapping.clear();

		if (gsdSet != null)
			for (GitSynchronizeData gsd : gsdSet)
				gsd.dispose();

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (GitSynchronizeData data : gsdSet) {
			builder.append(data.getRepository().getWorkTree());
			builder.append(" "); //$NON-NLS-1$
		}

		return builder.toString();
	}

}

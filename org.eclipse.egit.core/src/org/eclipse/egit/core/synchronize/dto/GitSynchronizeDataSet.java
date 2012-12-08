/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.lib.Repository;

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
		gsdSet = new HashSet<GitSynchronizeData>();
		projectMapping = new HashMap<String, GitSynchronizeData>();
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
		if (data.getIncludedPaths() != null
				&& data.getIncludedPaths().size() > 0)
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

	/**
	 * @param repo
	 * @return <code>null</code> if project does not have corresponding data
	 */
	public GitSynchronizeData getData(Repository repo) {
		for (GitSynchronizeData gsd : gsdSet)
			if (repo.equals(gsd.getRepository()))
				return gsd;
		return null;
	}

	public Iterator<GitSynchronizeData> iterator() {
		return gsdSet.iterator();
	}

	/**
	 * @return list of all resources
	 */
	public IProject[] getAllProjects() {
		Set<IProject> resource = new HashSet<IProject>();
		for (GitSynchronizeData data : gsdSet) {
			resource.addAll(data.getProjects());
		}
		return resource.toArray(new IProject[resource.size()]);
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

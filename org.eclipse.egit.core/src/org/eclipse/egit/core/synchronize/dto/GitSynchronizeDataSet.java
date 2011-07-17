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

/**
 *
 */
public class GitSynchronizeDataSet implements Iterable<GitSynchronizeData> {

	private final Set<GitSynchronizeData> gsd;

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
		gsd = new HashSet<GitSynchronizeData>();
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
		gsd.add(data);
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
	 * @return number of {@link GitSynchronizeData} that are included in this
	 *         set
	 */
	public int size() {
		return gsd.size();
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

	public Iterator<GitSynchronizeData> iterator() {
		return gsd.iterator();
	}

	/**
	 * @return list of all resources
	 */
	public IProject[] getAllProjects() {
		Set<IProject> resource = new HashSet<IProject>();
		for (GitSynchronizeData data : gsd) {
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (GitSynchronizeData data : gsd) {
			builder.append(data.getRepository().getWorkTree());
			builder.append(" "); //$NON-NLS-1$
		}

		return builder.toString();
	}

}

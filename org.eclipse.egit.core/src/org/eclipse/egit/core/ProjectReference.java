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
 *******************************************************************************/
package org.eclipse.egit.core;

import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * ProjectReference for Team project sets
 */
public final class ProjectReference {

	private static final String DEFAULT_BRANCH = Constants.MASTER;

	/**
	 * the version of the reference string
	 */
	private String version;

	/**
	 * a relative path (from the repository root) to a project
	 */
	private String projectDir;

	/**
	 * <code>repository</code> parameter
	 */
	private URIish repository;

	/**
	 * the remote branch that will be checked out, see <code>--branch</code>
	 * option
	 */
	private String branch = DEFAULT_BRANCH;

	/**
	 * use this name instead of using the remote name origin to keep track
	 * of the upstream repository, see <code>--origin</code> option.
	 */
	private String origin = Constants.DEFAULT_REMOTE_NAME;

	static final String SEPARATOR = ","; //$NON-NLS-1$

	/**
	 * @param reference
	 * @throws URISyntaxException
	 * @throws IllegalArgumentException
	 */
	@SuppressWarnings("boxing")
	public ProjectReference(final String reference) throws URISyntaxException, IllegalArgumentException {
		final String[] tokens = reference.split(Pattern.quote(ProjectReference.SEPARATOR));
		if (tokens.length != 4)
			throw new IllegalArgumentException(NLS.bind(
					CoreText.ProjectReference_InvalidTokensCount, new Object[] {
							4, tokens.length, tokens }));

		this.version = tokens[0];
		this.repository = new URIish(tokens[1]);
		if (!tokens[2].isEmpty()) {
			this.branch = tokens[2];
		}
		this.projectDir = tokens[3];
	}

	/**
	 * @return <code>repository</code> parameter
	 */
	public URIish getRepository() {
		return repository;
	}

	/**
	 * @return the remote branch that will be checked out, see <code>--branch</code>
	 * option
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * @return name of the upstream repository
	 */
	public String getOrigin() {
		return origin;
	}

	/**
	 * @return a relative path (from the repository root) to a project
	 */
	public String getProjectDir() {
		return projectDir;
	}

	String getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((branch == null) ? 0 : branch.hashCode());
		result = prime * result
				+ ((projectDir == null) ? 0 : projectDir.hashCode());
		result = prime * result
				+ ((repository == null) ? 0 : repository.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ProjectReference))
			return false;
		ProjectReference other = (ProjectReference) obj;
		if (branch == null) {
			if (other.branch != null)
				return false;
		} else if (!branch.equals(other.branch))
			return false;
		if (projectDir == null) {
			if (other.projectDir != null)
				return false;
		} else if (!projectDir.equals(other.projectDir))
			return false;
		if (repository == null) {
			if (other.repository != null)
				return false;
		} else if (!repository.equals(other.repository))
			return false;
		return true;
	}
}

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
 *******************************************************************************/
package org.eclipse.egit.core;

import java.net.URISyntaxException;
import java.util.regex.Pattern;

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

	/**TODO
	 * @param reference
	 * @throws URISyntaxException
	 * @throws IllegalArgumentException
	 */
	@SuppressWarnings("boxing")
	public ProjectReference(final String reference) throws URISyntaxException, IllegalArgumentException {
		final String[] tokens = reference.split(Pattern.quote(GitProjectSetCapability.SEPARATOR));
		if (tokens.length != 4)
			throw new IllegalArgumentException(NLS.bind(
					CoreText.GitProjectSetCapability_InvalidTokensCount, new Object[] {
							4, tokens.length, tokens }));

		this.version = tokens[0];
		this.repository = new URIish(tokens[1]);
		if (!"".equals(tokens[2])) //$NON-NLS-1$
			this.branch = tokens[2];
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
	 * @return the version of the reference string
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return a relative path (from the repository root) to a project
	 */
	public String getProjectDir() {
		return projectDir;
	}
}
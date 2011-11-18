/*******************************************************************************
 * Copyright (c) 2011, IBM Corporation
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tomasz Zarna (IBM) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ProjectSetCapability;

/**
 * This URI like construct used for a Git SCM URL. See
 * http://maven.apache.org/scm/scm-url-format.html for the format description.
 */
public class GitURI {
	private static final String SCHEME_GIT = "git"; //$NON-NLS-1$

	private final URIish repository;

	private IPath path;

	private String tag;

	private String projectName;

	/**
	 * Convert the given URI to a {@link GitURI}.
	 *
	 * @param uri
	 *            the URI in the SCM URL format
	 * @return a Git URI or <code>null</code> if the URI is not in the expected
	 *         format
	 */
	public static GitURI fromUri(URI uri) {
		try {
			if (ProjectSetCapability.SCHEME_SCM.equals(uri.getScheme())) {
				final String ssp = uri.getSchemeSpecificPart();
				if (ssp.startsWith(SCHEME_GIT)) {
					int indexOfSemicolon = ssp.indexOf(';');
					URIish repository = new URIish(ssp.substring(
							SCHEME_GIT.length() + 1, indexOfSemicolon));
					IPath path = null;
					String tag = Constants.MASTER; // default
					String project = null;
					String[] params = ssp.substring(indexOfSemicolon)
							.split(";"); //$NON-NLS-1$
					for (String param : params) {
						if (param.startsWith("path=")) { //$NON-NLS-1$
							path = new Path(
									param.substring(param.indexOf('=') + 1));
						} else if (param.startsWith("tag=")) { //$NON-NLS-1$
							tag = param.substring(param.indexOf('=') + 1);
						} else if (param.startsWith("project=")) { //$NON-NLS-1$
							project = param.substring(param.indexOf('=') + 1);
						}
					}
					return new GitURI(repository, path, tag, project);
				}
			}
			return null;
		} catch (URISyntaxException e) {
			Activator.logError(e.getMessage(), e);
			throw new IllegalArgumentException(NLS.bind(
					CoreText.GitURI_InvalidURI, new String[] { uri.toString(),
							e.getMessage() }));
		}
	}

	/**
	 * Construct the {@link GitURI}
	 *
	 * @param repository
	 * @param path
	 * @param tag
	 * @param projectName
	 */
	public GitURI(URIish repository, IPath path, String tag, String projectName) {
		this.repository = repository;
		this.path = path;
		this.tag = tag;
		this.projectName = projectName;
	}

	/**
	 * @return path
	 */
	public IPath getPath() {
		return path;
	}

	/**
	 * @return repository
	 */
	public URIish getRepository() {
		return repository;
	}

	/**
	 * @return tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @return project name
	 */
	public String getProjectName() {
		return projectName;
	}
}

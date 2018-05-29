/*******************************************************************************
 * Copyright (c) 2011, IBM Corporation
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * This URI like construct used for a Git SCM URL. See
 * http://maven.apache.org/scm/scm-url-format.html for the format description.
 */
public class GitURI {
	// TODO replace with org.eclipse.team.core.ProjectSetCapability.SCHEME_SCM
	// when we drop support for Galileo
	private static final String SCHEME_SCM = "scm"; //$NON-NLS-1$

	private static final String SCHEME_GIT = "git"; //$NON-NLS-1$

	private static final String KEY_PATH = "path"; //$NON-NLS-1$

	private static final String KEY_PROJECT = "project"; //$NON-NLS-1$

	private static final String KEY_TAG = "tag"; //$NON-NLS-1$

	private final URIish repository;

	private IPath path;

	private String tag;

	private String projectName;

	/**
	 * Construct the {@link GitURI} for the given URI.
	 *
	 * @param uri
	 *            the URI in the SCM URL format
	 */
	public GitURI(URI uri) {
		try {
			if (SCHEME_SCM.equals(uri.getScheme())) {
				final String ssp = uri.getSchemeSpecificPart();
				int indexOfSemicolon = ssp.indexOf(';');
				if (indexOfSemicolon < 0) {
					throw new IllegalArgumentException(
							NLS.bind(CoreText.GitURI_InvalidSCMURL,
									new String[] { uri.toString() }));
				}
				if (ssp.startsWith(SCHEME_GIT)) {
					URIish r = new URIish(ssp.substring(
							SCHEME_GIT.length() + 1, indexOfSemicolon));
					IPath p = null;
					String t = Constants.MASTER; // default
					String pn = null;
					String[] params = ssp.substring(indexOfSemicolon)
							.split(";"); //$NON-NLS-1$
					for (String param : params) {
						if (param.startsWith(KEY_PATH + '=')) {
							p = new Path(
									param.substring(param.indexOf('=') + 1));
						} else if (param.startsWith(KEY_TAG + '=')) {
							t = param.substring(param.indexOf('=') + 1);
						} else if (param.startsWith(KEY_PROJECT + '=')) {
							pn = param.substring(param.indexOf('=') + 1);
						}
					}
					this.repository = r;
					this.path = p;
					this.tag = t;
					this.projectName = pn;
					return;
				}
			}
			throw new IllegalArgumentException(NLS.bind(
					CoreText.GitURI_InvalidSCMURL,
					new String[] { uri.toString() }));
		} catch (URISyntaxException e) {
			Activator.logError(e.getMessage(), e);
			throw new IllegalArgumentException(NLS.bind(
					CoreText.GitURI_InvalidURI, new String[] { uri.toString(),
							e.getMessage() }));
		}
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

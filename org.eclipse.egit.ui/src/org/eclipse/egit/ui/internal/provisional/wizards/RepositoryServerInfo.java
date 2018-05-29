/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.provisional.wizards;

import java.net.URI;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as part
 * of a work in progress. There is no guarantee that this API will work or that
 * it will remain the same. Please do not use this API without consulting with
 * the egit team.
 * </p>
 *
 * Contains info of a server which hosts git repositories.
 */
public class RepositoryServerInfo {

	private final String label;

	private final URI uri;

	private UserPasswordCredentials credentials;

	/**
	 * @param label
	 *            the human readable label of the repository server to be shown
	 *            in the UI
	 * @param uri
	 *            the URI of the repository server
	 */
	public RepositoryServerInfo(String label, URI uri) {
		this.label = label;
		this.uri = uri;
	}

	/**
	 * @return label the human readable label of the repository server to be
	 *         shown in the UI
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the URI of the repository server which can be used for queries
	 *         for repositories
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * @param user the user name needed to log in
	 * @param password  the password needed to log in
	 */
	public void setCredentials(String user, String password) {
		credentials = new UserPasswordCredentials(user, password);
	}

	/**
	 * @return the credentials needed to log in
	 */
	public UserPasswordCredentials getCredentials() {
		return credentials;
	}

}

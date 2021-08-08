/*******************************************************************************
 * Copyright (C) 2021, Thomas wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.credentials;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;

/**
 * An OSGi service interface for a service for getting credentials. Invoked by
 * EGit core when no stored credentials in the Eclipse secure storage can be
 * found. EGit UI provides a service implementation with the default service
 * ranking.
 */
public interface CredentialsUI {

	/**
	 * Fills arbitrary {@link CredentialItem} items. May be called from any
	 * thread; if UI interaction is needed, the implementation is responsible
	 * for running the UI on the UI thread.
	 *
	 * @param uri
	 *            describing the resource the items should be filled for
	 * @param items
	 *            to fill
	 * @return {@code true} if the items were filled; {@code false} otherwise
	 */
	boolean fillCredentials(URIish uri, CredentialItem... items);

	/**
	 * Provides a user name and password/passphrase. May be called from any
	 * thread; if UI interaction is needed, the implementation is responsible
	 * for running the UI on the UI thread.
	 *
	 * @param uri
	 *            describing the resource the user name and password should be
	 *            returned for
	 * @return a {@link UserPasswordCredentials} object containing the user name
	 *         and password, or {@code null} if no credentials can be provided
	 */
	@Nullable UserPasswordCredentials getCredentials(URIish uri);
}

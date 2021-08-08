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

import java.io.IOException;

import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.transport.URIish;

/**
 * The EGit secure credentials store provides access to stored EGit credentials.
 * This is an OSGi service; EGit core provides a default implementation based on
 * the Eclipse secure storage, with the default service ranking.
 * <p>
 * Credentials in EGit are always for some resource described by an URI, which
 * can be a git remote URI, but which could also be refer to something else, for
 * instance a file URI denoting an encrypted SSH private key.
 * </p>
 */
public interface CredentialsStore {

	/**
	 * Puts credentials for the given URI into the store.
	 *
	 * @param uri
	 *            {@link URIish} denoting the resource the {@code credentials}
	 *            are for
	 * @param credentials
	 *            {@link UserPasswordCredentials} to store for the given
	 *            {@code uri}
	 * @throws StorageException
	 *             if the credentials cannot be stored
	 * @throws IOException
	 *             if the credentials cannot be stored
	 */
	void putCredentials(@NonNull URIish uri,
			@NonNull UserPasswordCredentials credentials)
			throws StorageException, IOException;

	/**
	 * Retrieves credentials stored for the given URI from the store.
	 *
	 * @param uri
	 *            {@link URIish} denoting the resource to get the
	 *            {@code credentials} of
	 * @return credentials the {@link UserPasswordCredentials} found in the
	 *         store, {@code null} if none are found
	 * @throws StorageException
	 *             if the credentials store cannot be accessed
	 */
	@Nullable
	UserPasswordCredentials getCredentials(@NonNull URIish uri)
			throws StorageException;

	/**
	 * Clears the stored credentials, if any, for the given URI.
	 *
	 * @param uri {@link URIish} denoting the resource to remove the stored
	 *            {@code credentials} of
	 * @throws IOException if the credentials store cannot be accessed
	 */
	void clearCredentials(@NonNull URIish uri) throws IOException;
}

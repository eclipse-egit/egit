/*******************************************************************************
 * Copyright (C) 2019, 2020 Salesforce and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Gunnar Wagenknecht 2019 - initial API
 *    Thomas Wolf        2020 - extracted from CommmitMessageComponent
 *******************************************************************************/
package org.eclipse.egit.ui.internal.credentials;

import org.eclipse.egit.core.internal.signing.GpgSetup;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.UnsupportedSigningFormatException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.GpgObjectSigner;
import org.eclipse.jgit.lib.GpgSigner;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * Utilities for working with signatures.
 */
public final class SignatureUtils {

	private SignatureUtils() {
		// No instantiation
	}

	/**
	 * Checks whether a signing key for the given identification can be found by
	 * the default {@link GpgSigner#getDefault() GpgSigner}.
	 *
	 * @param config
	 *            to use
	 * @param personIdent
	 *            to use as a fallback if no {@link GpgConfig#getSigningKey()
	 *            signing key ID} is provided by {@code config}
	 * @return {@code true} if signing appears to be possible, {@code false}
	 *         otherwise
	 */
	public static boolean checkSigningKey(@NonNull GpgConfig config,
			@NonNull PersonIdent personIdent) {
		return checkSigningKey(GpgSetup.getDefault(), config, personIdent);
	}

	/**
	 * Checks whether a signing key for the given identification can be found by
	 * the given {@link GpgSigner}.
	 *
	 * @param signer
	 *            to use for the check
	 * @param config
	 *            to use
	 * @param personIdent
	 *            to use as a fallback if no {@link GpgConfig#getSigningKey()
	 *            signing key ID} is provided by {@code config}
	 * @return {@code true} if signing appears to be possible, {@code false}
	 *         otherwise
	 */
	public static boolean checkSigningKey(GpgSigner signer,
			@NonNull GpgConfig config, @NonNull PersonIdent personIdent) {
		if (signer != null) {
			try {
				CredentialsProvider credentials = new CredentialsProvider() {

					@Override
					public boolean supports(CredentialItem... items) {
						return true;
					}

					@Override
					public boolean isInteractive() {
						return false;
					}

					@Override
					public boolean get(URIish uri, CredentialItem... items)
							throws UnsupportedCredentialItem {
						return false;
					}
				};
				if (signer instanceof GpgObjectSigner) {
					return ((GpgObjectSigner) signer).canLocateSigningKey(
							config.getSigningKey(), personIdent, credentials,
							config);
				}
				return signer.canLocateSigningKey(config.getSigningKey(),
						personIdent, credentials);
			} catch (CanceledException e) {
				// interpret this as "ok" - a passphrase was asked and canceled
				// by our no-op CredentialsProvider
				return true;
			} catch (UnsupportedSigningFormatException e) {
				return false;
			}
		}
		return false;
	}
}

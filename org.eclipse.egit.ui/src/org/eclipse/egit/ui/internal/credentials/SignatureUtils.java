/*******************************************************************************
 * Copyright (C) 2019, 2020 EGit contributors.
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

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
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
	 * @param signingKey
	 *            from the GPG git config, may be {@code null}
	 * @param personIdent
	 *            to use as a fallback if no {@code signingKey} ID is provided
	 * @return {@code true} if signing appears to be possible, {@code false}
	 *         otherwise
	 */
	public static boolean checkSigningKey(String signingKey,
			@NonNull PersonIdent personIdent) {
		return checkSigningKey(GpgSigner.getDefault(), signingKey, personIdent);
	}

	/**
	 * Checks whether a signing key for the given identification can be found by
	 * the given {@link GpgSigner}.
	 *
	 * @param signer
	 *            to use for the check
	 * @param signingKey
	 *            from the GPG git config, may be {@code null}
	 * @param personIdent
	 *            to use as a fallback if no {@code signingKey} ID is provided
	 * @return {@code true} if signing appears to be possible, {@code false}
	 *         otherwise
	 */
	public static boolean checkSigningKey(GpgSigner signer, String signingKey,
			@NonNull PersonIdent personIdent) {
		if (signer != null) {
			try {
				return signer.canLocateSigningKey(signingKey, personIdent,
						new CredentialsProvider() {

							@Override
							public boolean supports(CredentialItem... items) {
								return true;
							}

							@Override
							public boolean isInteractive() {
								return false;
							}

							@Override
							public boolean get(URIish uri,
									CredentialItem... items)
									throws UnsupportedCredentialItem {
								return false;
							}
						});
			} catch (CanceledException e) {
				// interpret this as "ok" - a passphrase was asked and canceled
				// by our no-op CredentialsProvider
				return true;
			}
		}
		return false;
	}
}

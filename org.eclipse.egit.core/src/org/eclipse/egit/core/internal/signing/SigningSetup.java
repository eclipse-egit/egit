/*******************************************************************************
 * Copyright (c) 2021, 2024 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.signing;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.GpgConfig.GpgFormat;
import org.eclipse.jgit.lib.SignatureVerifiers;
import org.eclipse.jgit.lib.Signers;
import org.eclipse.jgit.signing.ssh.SshSignatureVerifierFactory;
import org.eclipse.jgit.signing.ssh.SshSignerFactory;

/**
 * Utility class to set up the selected GpgSigner.
 */
public final class SigningSetup {

	private SigningSetup() {
		// No instantiation
	}

	/**
	 * Available signers (for OpenPGP or X.509 signatures).
	 */
	public enum Signer {
		/** The default signer provided by JGit, using Bouncy Castle. */
		BC,

		/** The EGit signer, using an external gpg-compatible program. */
		GPG
	}

	private static final Object LOCK = new Object();

	private static Signer current;

	/**
	 * Ensures that SSH signing and signature verification is available.
	 */
	public static void setSshSigning() {
		// JGit may not find the SSH signer and signature verifier through its
		// ServiceLoader in an environment with separated classloaders, like
		// OSGi. Set it explicitly.
		Signers.set(GpgFormat.SSH, new SshSignerFactory().create());
		SignatureVerifiers.set(GpgFormat.SSH,
				new SshSignatureVerifierFactory().create());
	}

	/**
	 * Updates JGit settings to use the signer and signature verifier defined in
	 * the preferences.
	 */
	public static void update() {
		Signer signer = getSigner();
		synchronized (LOCK) {
			if (signer != current) {
				current = signer;
				switch (signer) {
				case BC:
					Signers.set(GpgFormat.OPENPGP, null);
					SignatureVerifiers.set(GpgFormat.OPENPGP, null);
					Signers.set(GpgFormat.X509, null);
					SignatureVerifiers.set(GpgFormat.X509, null);
					break;
				case GPG:
					Signers.set(GpgFormat.OPENPGP, new ExternalGpgSigner());
					SignatureVerifiers.set(GpgFormat.OPENPGP,
							new ExternalGpgSignatureVerifier());
					Signers.set(GpgFormat.X509, new ExternalGpgSigner(true));
					SignatureVerifiers.set(GpgFormat.X509,
							new ExternalGpgSignatureVerifier(true));
					break;
				default:
					// Internal error, no translation
					throw new IllegalStateException("Unknown signer " + signer); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Retrieves the currently configured signer.
	 *
	 * @return the {@link Signer}, never {@code null}
	 */
	@NonNull
	public static Signer getSigner() {
		String pref = Platform.getPreferencesService().getString(
				Activator.PLUGIN_ID, GitCorePreferences.core_gpgSigner, null,
				null);
		if (pref != null) {
			for (Signer s : Signer.values()) {
				if (pref.equalsIgnoreCase(s.name())) {
					return s;
				}
			}
		}
		Activator.logWarning(
				MessageFormat.format(CoreText.GpgSetup_signerUnknown, pref),
				null);
		return Signer.BC;
	}
}

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
import org.eclipse.jgit.gpg.bc.BouncyCastleGpgSignerFactory;
import org.eclipse.jgit.lib.GpgSignatureVerifierFactory;
import org.eclipse.jgit.lib.GpgSigner;

/**
 * Utility class to set up the selected GpgSigner.
 */
public final class GpgSetup {

	private GpgSetup() {
		// No instantiation
	}

	private enum Signer {
		BC, GPG
	}

	private static final Object LOCK = new Object();

	private static Signer current;

	private static GpgSignatureVerifierFactory defaultFactory = GpgSignatureVerifierFactory
			.getDefault();

	/**
	 * Ensures that the default {@link GpgSigner} is the one set from the
	 * preferences.
	 *
	 * @return the {@link GpgSigner}
	 */
	public static GpgSigner getDefault() {
		update();
		return GpgSigner.getDefault();
	}

	/**
	 * Updates GPG settings to use the signer and signature verifier defined in
	 * the preferences.
	 */
	public static void update() {
		Signer signer = getSigner();
		synchronized (LOCK) {
			if (signer != current) {
				current = signer;
				switch (signer) {
				case BC:
					GpgSigner.setDefault(BouncyCastleGpgSignerFactory.create());
					GpgSignatureVerifierFactory.setDefault(defaultFactory);
					break;
				case GPG:
					GpgSigner.setDefault(new ExternalGpgSigner());
					GpgSignatureVerifierFactory.setDefault(
							new ExternalGpgSignatureVerifierFactory());
					break;
				default:
					// Internal error, no translation
					throw new IllegalStateException("Unknown signer " + signer); //$NON-NLS-1$
				}
			}
		}
	}

	private static Signer getSigner() {
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

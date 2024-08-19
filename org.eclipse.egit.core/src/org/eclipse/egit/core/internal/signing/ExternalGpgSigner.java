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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.UnsupportedSigningFormatException;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.GpgSignature;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Signer;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.TemporaryBuffer;

/**
 * A {@link Signer} that calls out to an external GPG program. If no GPG program
 * is given, it tries to find a GPG executable on the $PATH to use.
 */
public class ExternalGpgSigner implements Signer {

	// A GPG environment variable name. We remove this environment variable when
	// calling gpg.
	private static final String PINENTRY_USER_DATA = "PINENTRY_USER_DATA"; //$NON-NLS-1$

	// Another GPG environment variable name. We remove this environment
	// variable when calling gpg.
	private static final String GPG_TTY = "GPG_TTY"; //$NON-NLS-1$

	@Override
	public boolean canLocateSigningKey(Repository repository, GpgConfig config,
			PersonIdent committer, String signingKey,
			CredentialsProvider credentialsProvider) throws CanceledException {
		// Ignore the CredentialsProvider. We let GPG handle all this.
		String program = config.getProgram();
		if (StringUtils.isEmptyOrNull(program)) {
			program = ExternalGpg.getGpg();
			if (StringUtils.isEmptyOrNull(program)) {
				return false;
			}
		}
		String keySpec = signingKey;
		if (keySpec == null) {
			keySpec = config.getSigningKey();
		}
		if (StringUtils.isEmptyOrNull(keySpec)) {
			keySpec = '<' + committer.getEmailAddress() + '>';
		}
		ProcessBuilder process = new ProcessBuilder();
		// For the output format, see
		// https://github.com/gpg/gnupg/blob/master/doc/DETAILS
		//
		// --no-auto-key-locate prevents GPG from asking external sources (like
		// key servers). See
		// https://www.gnupg.org/documentation/manuals/gnupg/GPG-Configuration-Options.html#index-auto_002dkey_002dlocate
		process.command(program, "--locate-keys", //$NON-NLS-1$
				"--no-auto-key-locate", //$NON-NLS-1$
				"--with-colons", //$NON-NLS-1$
				"--batch", //$NON-NLS-1$
				"--no-tty", //$NON-NLS-1$
				keySpec);
		gpgEnvironment(process);
		try {
			boolean[] result = { false };
			ExternalProcessRunner.run(process, null, b -> {
				try (BufferedReader r = new BufferedReader(
						new InputStreamReader(b.openInputStream(),
								StandardCharsets.UTF_8))) {
					// --with-colons always writes UTF-8
					boolean keyFound = false;
					String line;
					while ((line = r.readLine()) != null) {
						if (line.startsWith("pub:") //$NON-NLS-1$
								|| line.startsWith("sub:")) { //$NON-NLS-1$
							String[] fields = line.split(":"); //$NON-NLS-1$
							if (fields.length > 11
									&& fields[11].indexOf('s') >= 0) {
								// It's a signing key.
								keyFound = true;
								break;
							}
						}
					}
					result[0] = keyFound;
				}
			}, null);
			if (!result[0]) {
				if (!StringUtils.isEmptyOrNull(signingKey)) {
					Activator.logWarning(MessageFormat.format(
							CoreText.ExternalGpgSigner_noKeyFound,
							signingKey), null);
				}
			}
			return result[0];
		} catch (IOException e) {
			Activator.logError(e.getLocalizedMessage(), e);
			return false;
		}
	}

	@Override
	public GpgSignature sign(Repository repository, GpgConfig config,
			byte[] data, PersonIdent committer, String signingKey,
			CredentialsProvider credentialsProvider) throws CanceledException,
			IOException, UnsupportedSigningFormatException {
		// Sign an object with an external GPG executable. GPG handles
		// passphrase entry, including gpg-agent and native keychain
		// integration.
		String program = config.getProgram();
		if (StringUtils.isEmptyOrNull(program)) {
			program = ExternalGpg.getGpg();
			if (StringUtils.isEmptyOrNull(program)) {
				throw new IOException(CoreText.ExternalGpgSigner_gpgNotFound);
			}
		}
		String keySpec = signingKey;
		if (keySpec == null) {
			keySpec = config.getSigningKey();
		}
		if (StringUtils.isEmptyOrNull(keySpec)) {
			keySpec = '<' + committer.getEmailAddress() + '>';
		}
		ProcessBuilder process = new ProcessBuilder();
		process.command(program,
				// Detached signature, sign, armor, user
				"-bsau", //$NON-NLS-1$
				keySpec,
				// No extra output
				"--batch", //$NON-NLS-1$
				"--no-tty", //$NON-NLS-1$
				// Write extra status messages to stderr
				"--status-fd", //$NON-NLS-1$
				"2", //$NON-NLS-1$
				// Force output of the signature to stdout
				"--output", //$NON-NLS-1$
				"-"); //$NON-NLS-1$
		gpgEnvironment(process);
		try (ByteArrayInputStream dataIn = new ByteArrayInputStream(data)) {
			class Holder {
				byte[] rawData;
			}
			Holder result = new Holder();
			ExternalProcessRunner.run(process, dataIn, b -> {
				// Sanity check: do we have a signature?
				boolean isValid = false;
				Throwable error = null;
				try {
					isValid = isValidSignature(b);
				} catch (IOException | PGPException e) {
					error = e;
				}
				if (!isValid) {
					throw new IOException(MessageFormat.format(
							CoreText.ExternalGpgSigner_noSignature,
							ExternalProcessRunner.toString(b)), error);
				}
				result.rawData = b.toByteArray();
			}, e -> {
				// Error handling: parse stderr to figure out whether we have a
				// cancellation. Unfortunately, GPG does record cancellation not
				// via a [GNUPG:] stable status but by printing "gpg: signing
				// failed: Operation cancelled". Since we don't know whether
				// this string is stable (or may even be localized), we check
				// for a "[GNUPG:] PINENTRY_LAUNCHED" line followed by the next
				// [GNUPG:] line being "[GNUPG:] FAILURE sign".
				//
				// The [GNUPG:] strings are part of GPG's public API. See
				// https://github.com/gpg/gnupg/blob/master/doc/DETAILS
				try (BufferedReader r = new BufferedReader(
						new InputStreamReader(e.openInputStream(),
								StandardCharsets.UTF_8))) {
					String line;
					boolean pinentry = false;
					while ((line = r.readLine()) != null) {
						if (!pinentry && line
								.startsWith("[GNUPG:] PINENTRY_LAUNCHED")) { //$NON-NLS-1$
							pinentry = true;
							checkTerminalPrompt(line);
						} else if (pinentry) {
							if (line.startsWith("[GNUPG:] FAILURE sign")) { //$NON-NLS-1$
								throw new CanceledException(
										CoreText.ExternalGpgSigner_signingCanceled);
							}
							if (line.startsWith("[GNUPG:]")) { //$NON-NLS-1$
								pinentry = false;
							}
						}
					}
				} catch (IOException ex) {
					// Swallow it here; runProcess will raise one anyway.
				}
			});
			return new GpgSignature(result.rawData);
		}
	}

	private PGPSignature parseSignature(InputStream in)
			throws IOException, PGPException {
		try (InputStream sigIn = PGPUtil.getDecoderStream(in)) {
			PGPObjectFactory pgpFactory = new BcPGPObjectFactory(sigIn);
			Object obj = pgpFactory.nextObject();
			if (obj instanceof PGPCompressedData) {
				obj = new BcPGPObjectFactory(
						((PGPCompressedData) obj).getDataStream()).nextObject();
			}
			if (obj instanceof PGPSignatureList) {
				return ((PGPSignatureList) obj).get(0);
			} else if (obj instanceof PGPSignature) {
				return (PGPSignature) obj;
			}
			return null;
		}
	}

	private boolean isValidSignature(TemporaryBuffer b)
			throws IOException, PGPException {
		try (InputStream data = b.openInputStream()) {
			return parseSignature(data) != null;
		}
	}

	private void gpgEnvironment(ProcessBuilder process) {
		try {
			Map<String, String> childEnv = process.environment();
			// The map is "typically case sensitive on all platforms", whatever
			// that means. "Typically"? Or really on all platforms? It would
			// make more sense if it were case-insensitive on Windows.
			//
			// Remove the PINENTRY_USER_DATA variable. On Linux, some people use
			// this sometimes in combination with a custom script configured as
			// the gpg-agent's pinentry-program to force pinentry-tty or
			// pinentry-curses to be used when in a shell, and a graphical
			// prompt otherwise. When Eclipse gets started from the shell, it
			// may inherit that environment variable, but when it calls gpg, it
			// needs a graphical pinentry. So remove this variable.
			//
			// If the variable is not set, a well-written custom pinentry script
			// should fall back to the default gpg pinentry which _is_ a
			// graphical one (pinentry-mac on Mac, pinentry-qt on Windows,
			// pinentry-gtk or pinentry-gnome or similar on Linux). Not sure if
			// this PINENTRY_USER_DATA method is still needed or used with
			// modern gpg; at least pinentry-gtk and pinentry-gnome should fall
			// back to prompting on the terminal if $DISPLAY of the calling
			// process is not set. $DISPLAY for Eclipse on Linux/OS X will
			// always be set, and be inherited by the child process.
			String value = childEnv.get(PINENTRY_USER_DATA);
			if (!StringUtils.isEmptyOrNull(value)) {
				childEnv.remove(PINENTRY_USER_DATA);
			}
			// If gpg-agent is not running already, gpg will start it. If
			// GPG_TTY is set, the newly started gpg-agent may decide to use a
			// terminal prompt (pinentry-tty or pinentry-curses) for
			// passphrases, which doesn't work for us. So clear GPG_TTY, too.
			//
			// If gpg-agent is already running and is using a terminal prompt,
			// the signing may fail. We detect this in checkTerminalPrompt()
			// and throw an exception.
			value = childEnv.get(GPG_TTY);
			if (!StringUtils.isEmptyOrNull(value)) {
				childEnv.remove(GPG_TTY);
			}
		} catch (SecurityException | UnsupportedOperationException
				| IllegalArgumentException e) {
			Activator.logWarning(CoreText.ExternalGpgSigner_environmentError,
					e);
		}
	}

	private void checkTerminalPrompt(String gpgTraceLine) {
		// @formatter:off
		// Expected format: [GNUPG:] PINENTRY_LAUNCHED <pid> <pinentry-type> <version> <tty> <tty-type> <display>
		// Example: [GNUPG:] PINENTRY_LAUNCHED 22245 curses 1.1.1 - xterm-256color <$DISPLAY>
		// @formatter:on
		String[] parts = gpgTraceLine.split(" "); //$NON-NLS-1$
		if (parts.length > 3 && "[GNUPG:]".equals(parts[0]) //$NON-NLS-1$
				&& "PINENTRY_LAUNCHED".equals(parts[1])) { //$NON-NLS-1$
			String pinentryType = parts[3];
			if ("tty".equals(pinentryType) || "curses".equals(pinentryType)) { //$NON-NLS-1$ //$NON-NLS-2$
				throw new GpgConfigurationException(MessageFormat.format(
						CoreText.ExternalGpgSigner_ttyInput, gpgTraceLine));
			}
		}
	}
}

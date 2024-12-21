/*******************************************************************************
 * Copyright (c) 2024 Thomas Wolf <twolf@apache.org> and others.
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;

import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SignatureVerifier;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.TemporaryBuffer;

/**
 * A {@link SignatureVerifier} that calls an external GPG executable for
 * signature verification.
 */
public class ExternalGpgSignatureVerifier implements SignatureVerifier {

	private static final DateTimeFormatter GPG_DATE_FORMAT = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendValue(ChronoField.DAY_OF_MONTH, 2)
			.appendLiteral('T')
			.appendValue(ChronoField.HOUR_OF_DAY, 2)
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
			.toFormatter(Locale.ROOT);

	private final boolean x509;

	/**
	 * Creates a verifier for OpenPGP signatures.
	 */
	public ExternalGpgSignatureVerifier() {
		this(false);
	}

	/**
	 * Creates a verifier for OpenPGP or x.509 signatures.
	 *
	 * @param x509
	 *            {@code true} to verify x.509 signatures, {@code false} for
	 *            OpenPGP
	 */
	public ExternalGpgSignatureVerifier(boolean x509) {
		this.x509 = x509;
	}

	/**
	 * Verifies a given signature for given data.
	 *
	 * @param config
	 *            the {@link GpgConfig}
	 * @param data
	 *            the signature is for
	 * @param signatureData
	 *            the ASCII-armored signature
	 * @return a
	 *         {@link org.eclipse.jgit.lib.SignatureVerifier.SignatureVerification}
	 *         describing the outcome
	 * @throws IOException
	 *             if the signature cannot be verified
	 */
	@Override
	public SignatureVerification verify(Repository repository, GpgConfig config,
			byte[] data, byte[] signatureData) throws IOException {
		String program = config.getProgram();
		if (StringUtils.isEmptyOrNull(program)) {
			program = x509 ? ExternalGpg.getGpgSm() : ExternalGpg.getGpg();
			if (StringUtils.isEmptyOrNull(program)) {
				throw new IOException(CoreText.ExternalGpgSigner_gpgNotFound);
			}
		} else {
			program = ExternalGpg.findExecutable(program);
		}
		File signatureFile = null;
		SignatureVerification[] verification = { null };
		String[] name = { null };
		try {
			signatureFile = File.createTempFile("egit", ".sig"); //$NON-NLS-1$ //$NON-NLS-2$
			Files.write(signatureFile.toPath(), signatureData);
			ProcessBuilder process = new ProcessBuilder();
			process.command(program,
					// Write status lines to stdout
					"--status-fd", //$NON-NLS-1$
					"1", //$NON-NLS-1$
					"--verify", //$NON-NLS-1$
					signatureFile.getAbsolutePath(), //
					"-"); //$NON-NLS-1$ // Read from stdin
			try (ByteArrayInputStream dataIn = new ByteArrayInputStream(data)) {
				ExternalProcessRunner.run(process, dataIn, b -> {
					verification[0] = fromGpg(b);
				}, b -> {
					name[0] = extractProgramName(b);
				});
			} catch (CanceledException e) {
				// Ignored; user cannot cancel
			}
		} finally {
			if (signatureFile != null && !signatureFile.delete()) {
				signatureFile.deleteOnExit();
			}
		}
		SignatureVerification v = verification[0];
		if (v != null && name[0] != null && !name[0].equals(v.verifierName())) {
			return new SignatureVerification(name[0], v.creationDate(),
					v.signer(), v.keyFingerprint(), v.keyUser(), v.verified(),
					v.expired(), v.trustLevel(), v.message());
		}
		return v;
	}

	private String extractProgramName(TemporaryBuffer buffer) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				buffer.openInputStream(), StandardCharsets.UTF_8))) {
			String line = r.readLine();
			if (line != null) {
				int i = line.indexOf(": "); //$NON-NLS-1$
				if (i > 0) {
					return line.substring(0, i);
				}
			}
		} catch (IOException e) {
			// Ignore
		}
		return null;
	}

	private SignatureVerification fromGpg(TemporaryBuffer buffer) {
		Instant createdAt = null;
		TrustLevel trust = TrustLevel.UNKNOWN;
		String fingerprint = null;
		boolean validates = false;
		boolean expired = false;
		String message = null;
		String keyId = null;
		String userId = null;
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(buffer.openInputStream(),
						StandardCharsets.UTF_8))) {
			String line;
			while ((line = r.readLine()) != null) {
				if (!line.startsWith("[GNUPG:]")) { //$NON-NLS-1$
					continue;
				}
				line = line.substring(8).trim();
				if (line.startsWith("TRUST_")) { //$NON-NLS-1$
					int i = line.indexOf(' ');
					if (i < 0) {
						i = line.length();
					}
					String level = line.substring(6, i);
					// FULL or FULLY?? gpgsm prints the latter.
					if ("FULLY".equals(level)) { //$NON-NLS-1$
						trust = TrustLevel.FULL;
					} else {
						try {
							trust = TrustLevel.valueOf(level);
						} catch (IllegalArgumentException e) {
							// Ignore -- unknown trust level
						}
					}
				} else if (line.startsWith("SIG_ID")) { //$NON-NLS-1$
					// SIG_ID sig YYYY-MM-DD timestamp. gpgsm doesn't print
					// this.
					String[] parts = line.split(" "); //$NON-NLS-1$
					if (parts.length > 3) {
						// Timestamp of creation of signature
						try {
							createdAt = parseDate(parts[3].trim());
						} catch (RuntimeException e) {
							// Ignore.
						}
					}
				} else if (line.startsWith("VALIDSIG")) { //$NON-NLS-1$
					// VALIDSIG fingerprint YYYY-MM-DD timestamp ...
					// Extract the fingerprint.
					int i = line.indexOf(' ');
					int j = -1;
					if (i > 0) {
						j = line.indexOf(' ', i + 1);
					}
					if (j > i) {
						fingerprint = line.substring(i + 1, j);
					}
					// If we don't have createdAt yet, parse the time. gpg
					// writes a unix timestamp, but gpgsm prints UTC
					// yyyyMMDDTHHmmss.
					if (createdAt == null) {
						i = line.indexOf(' ', j + 1);
						if (i > j) {
							j = line.indexOf(' ', i + 1);
							String dateTime = line.substring(i+1, j);
							try {
								createdAt = parseDate(dateTime);
							} catch (RuntimeException e) {
								// Ignore
							}
						}
					}
				} else {
					int i = line.indexOf(' ');
					if (i < 0) {
						i = line.length();
					}
					String key = line.substring(0, i);
					boolean haveUserId = true;
					switch (key) {
					case "ERRSIG": //$NON-NLS-1$
						haveUserId = false;
						message = CoreText.ExternalGpgVerifier_erroneousSignature;
						break;
					case "GOODSIG": //$NON-NLS-1$
						validates = true;
						break;
					case "BADSIG": //$NON-NLS-1$
						message = CoreText.ExternalGpgVerifier_badSignature;
						break;
					case "EXPSIG": //$NON-NLS-1$
						expired = true;
						message = CoreText.ExternalGpgVerifier_expiredSignature;
						break;
					case "EXPKEYSIG": //$NON-NLS-1$
						expired = true;
						message = CoreText.ExternalGpgVerifier_expiredKeySignature;
						break;
					case "REVKEYSIG": //$NON-NLS-1$
						message = CoreText.ExternalGpgVerifier_revokedKeySignature;
						break;
					default:
						continue;
					}
					if (keyId != null) {
						// May occur only once. git does not really support
						// having multiple signatures.
						validates = false;
						message = CoreText.ExternalGpgVerifier_multipleSignatures;
						break;
					}
					// Next is the key ID, then potentially following a user ID
					int j = line.indexOf(' ', i + 1);
					if (j <= i) {
						j = line.length();
					}
					keyId = line.substring(i + 1, j);
					if (haveUserId && j < line.length()) {
						userId = line.substring(j + 1);
					}
					if (message != null) {
						message = MessageFormat.format(message, keyId, userId);
					}
				}
			}
			if (StringUtils.isEmptyOrNull(fingerprint)) {
				fingerprint = keyId;
			}
		} catch (IOException e) {
			message = MessageFormat.format(CoreText.ExternalGpgVerifier_failure,
					e.getLocalizedMessage());
			validates = false;
		}
		return new SignatureVerification(getName(),
				createdAt != null ? Date.from(createdAt) : null,
				userId, fingerprint, userId, validates, expired, trust,
				message);
	}

	private Instant parseDate(String dateTime) {
		// GPG timestamps are sometimes unix timestamps and sometimes in the
		// format yyyyMMDDTHHmmss (in UTC).
		if (dateTime.indexOf('T') > 0) {
			return GPG_DATE_FORMAT.parse(dateTime, LocalDateTime::from)
					.atOffset(ZoneOffset.UTC).toInstant();
		}
		return Instant.ofEpochSecond(Long.parseLong(dateTime));
	}

	@Override
	public String getName() {
		return x509 ? "gpgsm" : "gpg"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void clear() {
		// Nothing to do -- we don't keep state. We rely on the external GPG to
		// somehow cache public keys.
		//
		// Gpg4Win does; it runs a keyboxd daemon that maintains an in-memory
		// cache of loaded public keys.
	}
}

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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.stream.Collectors;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS.ExecutionResult;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.jgit.util.TemporaryBuffer;

/**
 * A utility class for running an external process.
 */
class ExternalProcessRunner {

	private ExternalProcessRunner() {
		// No instantiation of utility class
	}

	interface ResultHandler {
		void accept(TemporaryBuffer b) throws IOException, CanceledException;
	}

	static void run(ProcessBuilder process, InputStream in,
			ResultHandler stdout, ResultHandler stderr)
			throws IOException, CanceledException {
		String command = process.command().stream()
				.collect(Collectors.joining(" ")); //$NON-NLS-1$
		ExecutionResult result = null;
		int code = 0;
		try {
			if (GitTraceLocation.GPG.isActive()) {
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.GPG.getLocation(),
						"Spawning process: " + command); //$NON-NLS-1$
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.GPG.getLocation(),
						"Environment: " + process.environment()); //$NON-NLS-1$
			}
			result = FS.DETECTED.execute(process, in);
			code = result.getRc();
			if (GitTraceLocation.GPG.isActive()) {
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.GPG.getLocation(),
						"stderr:\n" + toString(result.getStderr())); //$NON-NLS-1$
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.GPG.getLocation(),
						"stdout:\n" + toString(result.getStdout())); //$NON-NLS-1$
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.GPG.getLocation(),
						"Spawned process exited with exit code " + code); //$NON-NLS-1$
			}
			if (code != 0) {
				if (stderr != null) {
					stderr.accept(result.getStderr());
				}
				throw new IOException(MessageFormat.format(
						CoreText.ExternalGpgSigner_processFailed, command,
						Integer.toString(code) + ": " //$NON-NLS-1$
								+ toString(result.getStderr())));
			}
			stdout.accept(result.getStdout());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(MessageFormat.format(
					CoreText.ExternalGpgSigner_processInterrupted, command), e);
		} catch (IOException e) {
			if (GitTraceLocation.GPG.isActive()) {
				if (result != null) {
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.GPG.getLocation(),
							"stderr:\n" + toString(result.getStderr())); //$NON-NLS-1$
					GitTraceLocation.getTrace().trace(
							GitTraceLocation.GPG.getLocation(),
							"stdout:\n" + toString(result.getStdout())); //$NON-NLS-1$
				}
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.GPG.getLocation(),
						"Spawned process failed: " + command, e); //$NON-NLS-1$
			}
			if (code != 0) {
				throw e;
			}
			if (result != null) {
				throw new IOException(MessageFormat.format(
						CoreText.ExternalGpgSigner_processFailed, command,
						toString(result.getStderr())), e);
			}
			throw new IOException(MessageFormat.format(
					CoreText.ExternalGpgSigner_processFailed, command,
					e.getLocalizedMessage()), e);
		} finally {
			if (result != null) {
				if (result.getStderr() != null) {
					result.getStderr().destroy();
				}
				if (result.getStdout() != null) {
					result.getStdout().destroy();
				}
			}
		}
	}

	static String toString(TemporaryBuffer b) {
		if (b != null) {
			try {
				return new String(b.toByteArray(4000),
						SystemReader.getInstance().getDefaultCharset());
			} catch (IOException e) {
				Activator.logWarning(CoreText.ExternalGpgSigner_bufferError, e);
			}
		}
		return ""; //$NON-NLS-1$
	}

}

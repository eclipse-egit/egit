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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.SystemReader;

/**
 * A utility class for finding an external GPG executable from $PATH.
 */
class ExternalGpg {

	private static final Map<String, String> EXECUTABLES = new ConcurrentHashMap<>();

	static String getGpg() {
		return get("gpg"); //$NON-NLS-1$
	}

	static String getGpgSm() {
		return get("gpgsm"); //$NON-NLS-1$
	}

	static String findExecutable(String program) {
		try {
			Path exe = Paths.get(program);
			if (!exe.isAbsolute() && exe.getNameCount() == 1) {
				String resolved = get(program);
				if (resolved != null) {
					return resolved;
				}
			}
		} catch (InvalidPathException e) {
			Activator.logWarning(
					MessageFormat.format(CoreText.ExternalGpg_invalidPath,
							program),
					e);
		}
		return program;
	}

	private static String get(String program) {
		String resolved = EXECUTABLES.computeIfAbsent(program,
				ExternalGpg::findProgram);
		return resolved.isEmpty() ? null : resolved;
	}

	private static String findProgram(String program) {
		// Called only for simple names.
		SystemReader system = SystemReader.getInstance();
		String path = system.getenv("PATH"); //$NON-NLS-1$
		String exe = null;
		if (system.isMacOS()) {
			// On Mac, $PATH is typically much shorter in programs launched
			// from the graphical UI than in the shell. Use the shell $PATH
			// first.
			String bash = searchPath(path, "bash"); //$NON-NLS-1$
			if (bash != null) {
				ProcessBuilder process = new ProcessBuilder();
				process.command(bash, "--login", "-c", "which " + program); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				process.directory(FS.DETECTED.userHome());
				String[] result = { null };
				try {
					ExternalProcessRunner.run(process, null, b -> {
						try (BufferedReader r = new BufferedReader(
								new InputStreamReader(b.openInputStream(),
										system.getDefaultCharset()))) {
							result[0] = r.readLine();
						}
					}, null);
				} catch (IOException | CanceledException e) {
					Activator.logWarning(
							CoreText.ExternalGpgSigner_cannotSearch, e);
				}
				exe = result[0];
			}
		}
		if (exe == null) {
			exe = searchPath(path,
					system.isWindows() ? completeWindowsPath(program)
							: program);
		}
		return exe == null ? "" : exe; //$NON-NLS-1$
	}

	private static String completeWindowsPath(String program) {
		// On Windows, Java Process follows the CreateProcess function, which
		// appends ".exe" if the program name has no extension and does not end
		// in a period, unless it includes a path.
		//
		// See
		// https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-createprocessa
		// and
		// https://github.com/openjdk/jdk/blob/43b7e9f54/src/java.base/windows/classes/java/lang/ProcessImpl.java#L338
		String name = new File(program).getName();
		if (name.equals(program) && name.indexOf('.') < 0) {
			return program + ".exe"; //$NON-NLS-1$
		}
		return program;
	}

	private static String searchPath(String path, String name) {
		if (StringUtils.isEmptyOrNull(path)) {
			return null;
		}
		for (String p : path.split(File.pathSeparator)) {
			File exe = new File(p, name);
			try {
				if (exe.isFile() && exe.canExecute()) {
					return exe.getAbsolutePath();
				}
			} catch (SecurityException e) {
				Activator.logWarning(MessageFormat.format(
						CoreText.ExternalGpgSigner_skipNotAccessiblePath,
						exe.getPath()), e);
			}
		}
		return null;
	}
}

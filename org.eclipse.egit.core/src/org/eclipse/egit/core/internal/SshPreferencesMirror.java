/*******************************************************************************
 * Copyright (C) 2018, 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static java.text.MessageFormat.format;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.annotations.NonNull;

/**
 * Mirrors the Eclipse ssh-related preferences. The values are mirrored in
 * formats suitable for the Apache MINA sshd client implementation, and are
 * updated when the preferences change.
 * <p>
 * All operations are thread-safe.
 * </p>
 */
public class SshPreferencesMirror {

	// No listener support: it's probably not a good idea or even impossible to
	// reconfigure ongoing ssh sessions. Our session factory simply gets the
	// current values whenever a new session is started.

	private static final String PREFERENCES_NODE = "org.eclipse.jsch.core"; //$NON-NLS-1$

	/** The singleton instance of the {@link SshPreferencesMirror}. */
	public static final SshPreferencesMirror INSTANCE = new SshPreferencesMirror();

	private IEclipsePreferences preferences;

	private IPreferenceChangeListener listener = event -> reloadPreferences();

	private File sshDirectory;

	private List<String> defaultIdentities;

	private String defaultMechanisms;

	private boolean started;

	private SshPreferencesMirror() {
		// This is a singleton.
	}

	/** Starts mirroring the ssh preferences. */
	public void start() {
		if (started) {
			return;
		}
		started = true;
		preferences = InstanceScope.INSTANCE.getNode(PREFERENCES_NODE);
		if (preferences != null) {
			preferences.addPreferenceChangeListener(listener);
		}
		reloadPreferences();
	}

	/** Stops mirroring the ssh preferences. */
	public void stop() {
		started = false;
		if (preferences != null) {
			preferences.removePreferenceChangeListener(listener);
		}
	}

	private void reloadPreferences() {
		synchronized (this) {
			setSshDirectory();
			setDefaultIdentities();
			setPreferredAuthentications();
		}
	}

	private String get(@NonNull String key) {
		return Platform.getPreferencesService().getString(PREFERENCES_NODE, key,
				null, null);
	}

	private void setSshDirectory() {
		String sshDir = get("SSH2HOME"); //$NON-NLS-1$
		if (sshDir != null) {
			try {
				sshDirectory = Paths.get(sshDir).toFile();
				return;
			} catch (InvalidPathException e) {
				Activator.logWarning(
						format(CoreText.SshPreferencesMirror_invalidDirectory,
								sshDir),
						null);
			}
		}
		sshDirectory = null;
	}

	private void setDefaultIdentities() {
		String defaultKeys = get("PRIVATEKEY"); //$NON-NLS-1$
		if (defaultKeys == null || defaultKeys.isEmpty()) {
			defaultIdentities = null;
			return;
		}
		defaultIdentities = Arrays.stream(defaultKeys.trim().split("\\s*,\\s*")) //$NON-NLS-1$
				.map(s -> {
					if (s.isEmpty()) {
						return null;
					}
					try {
						Paths.get(s);
						return s;
					} catch (InvalidPathException e) {
						Activator.logWarning(
								format(CoreText.SshPreferencesMirror_invalidKeyFile,
										s),
								null);
						return null;
					}
				}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private void setPreferredAuthentications() {
		String mechanisms = get("CVSSSH2PreferencePage.PREF_AUTH_METHODS"); //$NON-NLS-1$
		if (mechanisms == null || mechanisms.isEmpty()) {
			defaultMechanisms = null;
		} else {
			defaultMechanisms = mechanisms;
		}
	}

	/**
	 * Gets the ssh directory.
	 *
	 * @return the configured ssh directory, or {@code null} if the
	 *         configuration is invalid
	 */
	public File getSshDirectory() {
		synchronized (this) {
			return sshDirectory;
		}
	}

	/**
	 * Gets the configured default key files.
	 *
	 * @param sshDir
	 *            the directory that represents ~/.ssh/
	 * @return a possibly empty list of paths containing the configured default
	 *         identities (private keys), or {@code null} if the user didn't
	 *         configure any. An empty list indicates that user did configure
	 *         something invalid.
	 */
	public List<Path> getDefaultIdentities(@NonNull File sshDir) {
		synchronized (this) {
			if (defaultIdentities == null) {
				return null;
			}
			return defaultIdentities.stream()
					.map(s -> {
						File f = new File(s);
						if (!f.isAbsolute()) {
							f = new File(sshDir, s);
						}
						return f.toPath();
					}).filter(Files::exists).collect(Collectors.toList());
		}
	}

	/**
	 * Gets the configured default authentication mechanisms.
	 *
	 * @return the default authentication mechanisms as a single comma-separated
	 *         string
	 */
	public String getPreferredAuthentications() {
		synchronized (this) {
			return defaultMechanisms;
		}
	}
}

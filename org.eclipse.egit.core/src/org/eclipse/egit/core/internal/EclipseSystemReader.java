/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.IOException;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

/**
 * A system reader that hides certain global git environment variables from
 * JGit.
 */
public class EclipseSystemReader extends SystemReader {

	/**
	 * Hide these variables lest JGit tries to use them for different
	 * repositories.
	 */
	private static final String[] HIDDEN_VARIABLES = {
			Constants.GIT_DIR_KEY, Constants.GIT_WORK_TREE_KEY,
			Constants.GIT_OBJECT_DIRECTORY_KEY,
			Constants.GIT_INDEX_FILE_KEY,
			Constants.GIT_ALTERNATE_OBJECT_DIRECTORIES_KEY };

	private final SystemReader delegate;

	/**
	 * Creates a new instance based on the delegate.
	 *
	 * @param delegate
	 *            to use
	 */
	public EclipseSystemReader(SystemReader delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getenv(String variable) {
		String result = delegate.getenv(variable);
		if (result == null) {
			return result;
		}
		boolean isWin = isWindows();
		for (String gitvar : HIDDEN_VARIABLES) {
			if (isWin && gitvar.equalsIgnoreCase(variable)
					|| !isWin && gitvar.equals(variable)) {
				return null;
			}
		}
		return result;
	}

	@Override
	public String getHostname() {
		return delegate.getHostname();
	}

	@Override
	public String getProperty(String key) {
		return delegate.getProperty(key);
	}

	@Override
	public FileBasedConfig openUserConfig(Config parent, FS fs) {
		return delegate.openUserConfig(parent, fs);
	}

	@Override
	public FileBasedConfig openJGitConfig(Config parent, FS fs) {
		return delegate.openJGitConfig(parent, fs);
	}

	@Override
	public FileBasedConfig openSystemConfig(Config parent, FS fs) {
		return delegate.openSystemConfig(parent, fs);
	}

	@Override
	public long getCurrentTime() {
		return delegate.getCurrentTime();
	}

	@Override
	public int getTimezone(long when) {
		return delegate.getTimezone(when);
	}

	@Override
	public StoredConfig getUserConfig()
			throws IOException, ConfigInvalidException {
		return delegate.getUserConfig();
	}

	@Override
	public StoredConfig getJGitConfig()
			throws IOException, ConfigInvalidException {
		return delegate.getJGitConfig();
	}

	@Override
	public StoredConfig getSystemConfig()
			throws IOException, ConfigInvalidException {
		return delegate.getSystemConfig();
	}
}
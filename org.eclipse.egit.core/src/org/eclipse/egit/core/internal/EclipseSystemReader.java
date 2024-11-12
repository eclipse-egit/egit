/*******************************************************************************
 * Copyright (c) 2018, 2024 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.SystemReader;

/**
 * A system reader that hides certain global git environment variables from
 * JGit.
 */
public class EclipseSystemReader extends SystemReader.Delegate {

	/**
	 * Hide these variables lest JGit tries to use them for different
	 * repositories.
	 */
	private static final String[] HIDDEN_VARIABLES = {
			Constants.GIT_DIR_KEY, Constants.GIT_WORK_TREE_KEY,
			Constants.GIT_OBJECT_DIRECTORY_KEY,
			Constants.GIT_INDEX_FILE_KEY,
			Constants.GIT_ALTERNATE_OBJECT_DIRECTORIES_KEY };

	/**
	 * Creates a new instance based on the delegate.
	 *
	 * @param delegate
	 *            to use
	 */
	public EclipseSystemReader(@NonNull SystemReader delegate) {
		super(delegate);
	}

	@Override
	public String getenv(String variable) {
		String result = super.getenv(variable);
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
}
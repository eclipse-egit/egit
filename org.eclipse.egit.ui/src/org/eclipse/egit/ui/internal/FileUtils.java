/*******************************************************************************
 * Copyright (C) 2015 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for {@link File}s.
 */
public final class FileUtils {

	private FileUtils() {
		// Prevent instatiation
	}

	/**
	 * Best-effort variation of {@link File#getCanonicalFile()} returning the
	 * input file if the file cannot be canonicalized instead of throwing
	 * IOException.
	 *
	 * @param file
	 *            to be canonicalized
	 * @return canonicalized file, of the input file if canonicalization failed
	 *         of {@code file == null}
	 */
	public static File canonical(File file) {
		if (file == null) {
			return null;
		}
		try {
			return file.getCanonicalFile();
		} catch (IOException e) {
			return file;
		}
	}
}

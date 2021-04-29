/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import org.eclipse.jgit.annotations.NonNull;

/**
 * Possible input modes for the EGit merge editor.
 */
public enum MergeInputMode {

	/**
	 * Use the working tree file as input; as pre-merged by git, including
	 * conflict markers.
	 */
	WORKTREE,

	/** Use stage_2, i.e., the 'ours' version. */
	STAGE_2;

	/**
	 * Leniently converts an integer to a {@link MergeInputMode}. Unknown
	 * integers are converted to {@link #STAGE_2}.
	 *
	 * @param i
	 *            integer to convert
	 * @return the {@link MergeInputMode}
	 */
	@NonNull
	public static MergeInputMode fromInteger(int i) {
		if (i == 1) {
			return WORKTREE;
		}
		return STAGE_2;
	}

	/**
	 * Obtains an integer encoding of the enum value suitable for storing as a
	 * preference. When passed to {@link #fromInteger(int)}, {@code this} value
	 * is returned again.
	 *
	 * @return the integer, strictly greater than zero
	 */
	public int toInteger() {
		return ordinal() + 1;
	}
}

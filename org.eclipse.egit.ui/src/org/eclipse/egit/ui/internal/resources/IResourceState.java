/*******************************************************************************
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.resources;

import org.eclipse.jgit.annotations.NonNull;

/**
 * Provides information about the repository state of an item in the repository.
 */
public interface IResourceState {

	/**
	 * Set of possible staging states for a resource.
	 */
	enum StagingState {
		/** Represents a resource that is not staged. */
		NOT_STAGED,

		/** Represents a resource that has been modified. */
		MODIFIED,

		/** Represents a resource that is added to Git. */
		ADDED,

		/** Represents a resource that is removed from Git. */
		REMOVED,

		/** Represents a resource that has been renamed. */
		RENAMED
	}

	/**
	 * Returns whether or not the resource is tracked by Git.
	 *
	 * @return whether or not the resource is tracked by Git
	 */
	boolean isTracked();

	/**
	 * Returns whether or not the resource is ignored, either by a global team
	 * ignore in Eclipse, or by .git/info/exclude et al.
	 *
	 * @return whether or not the resource is ignored
	 */
	boolean isIgnored();

	/**
	 * Returns whether or not the resource has changes that are not staged.
	 *
	 * @return whether or not the resource is dirty
	 */
	boolean isDirty();

	/**
	 * Returns whether or not the resource has been deleted locally (unstaged
	 * deletion).
	 *
	 * @return whether or not the resource is missing
	 */
	boolean isMissing();

	/**
	 * Returns the {@link StagingState} of the resource.
	 *
	 * @return the state of the resource
	 */
	@NonNull
	StagingState getStagingState();

	/**
	 * Returns whether the resource is staged. Note that a resource may at the
	 * same time be staged and dirty, i.e., may have staged and unstaged
	 * changes.
	 * <p>
	 * This is equivalent to {@link #getStagingState}() !=
	 * {@link StagingState#NOT_STAGED}.
	 * </p>
	 *
	 * @return whether the resource is staged
	 */
	boolean isStaged();

	/**
	 * Returns whether or not the resource has merge conflicts.
	 *
	 * @return whether or not the resource has merge conflicts
	 */
	boolean hasConflicts();

	/**
	 * Returns whether or not the resource is assumed unchanged.
	 *
	 * @return whether or not the resource is assumed unchanged
	 */
	boolean isAssumeUnchanged();

	/**
	 * Returns whether or not the resource has any changes that are unstaged.
	 *
	 * @return whether or not the resource has any changes that are unstaged
	 */
	boolean hasUnstagedChanges();

}

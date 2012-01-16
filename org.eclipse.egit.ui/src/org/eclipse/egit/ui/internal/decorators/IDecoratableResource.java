/*******************************************************************************
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;

/**
 * Represents the state of a resource that can be used as a basis for decoration
 */
public interface IDecoratableResource {

	/**
	 * Set of possible staging states for a resource
	 */
	public enum Staged {
		/** Represents a resource that is not staged */
		NOT_STAGED,
		/** Represents a resource that has been modified */
		MODIFIED,
		/** Represents a resource that is added to Git */
		ADDED,
		/** Represents a resource that is removed from Git */
		REMOVED,
		/** Represents a resource that has been renamed */
		RENAMED
	}

	/**
	 * Gets the type of the resource as defined by {@link IResource}
	 *
	 * @return the type of the resource
	 */
	int getType();

	/**
	 * Gets the name of the resource
	 *
	 * @return the name of the resource
	 */
	String getName();

	/**
	 * Gets the name of the repository of the resource
	 *
	 * @return the name of the current branch, or <code>null</code> if not
	 *         applicable
	 */
	String getRepositoryName();

	/**
	 * Gets the current branch of the resource if applicable
	 *
	 * @return the name of the current branch, or <code>null</code> if not
	 *         applicable
	 */
	String getBranch();

	/**
	 * @return a symbol indicating the branch status relative to the remote
	 *         tracking branch, or <code>null</code> if not applicable
	 */
	String getBranchStatus();

	/**
	 * Returns whether or not the resource is tracked by Git
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
	 * Returns whether or not the resource has changes that are not staged
	 *
	 * @return whether or not the resource is dirty
	 */
	boolean isDirty();

	/**
	 * Returns the staged state of the resource
	 *
	 * The set of allowed values are defined by the <code>Staged</code> enum
	 *
	 * @return the staged state of the resource
	 */
	Staged staged();

	/**
	 * Returns whether or not the resource has merge conflicts
	 *
	 * @return whether or not the resource has merge conflicts
	 */
	boolean hasConflicts();

	/**
	 * Returns whether or not the resource is assumed valid
	 *
	 * @return whether or not the resource is assumed valid
	 */
	boolean isAssumeValid();
}

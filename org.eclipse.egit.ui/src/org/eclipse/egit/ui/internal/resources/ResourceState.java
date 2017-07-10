/*******************************************************************************
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.resources;

import org.eclipse.jgit.annotations.NonNull;

/**
 * Base implementation of an {@link IResourceState}.
 */
public class ResourceState implements IResourceState {

	/**
	 * Flag indicating whether or not the resource is tracked
	 */
	private boolean tracked;

	/**
	 * Flag indicating whether or not the resource is ignored
	 */
	private boolean ignored;

	/**
	 * Flag indicating whether or not the resource has changes that are not
	 * staged
	 */
	private boolean dirty;

	/**
	 * Flag indicating whether or not the resource has been deleted locally
	 * (unstaged deletion).
	 */
	private boolean missing;

	/**
	 * Staged state of the resource
	 */
	@NonNull
	private StagingState staged = StagingState.NOT_STAGED;

	/**
	 * Flag indicating whether or not the resource has merge conflicts
	 */
	private boolean conflicts;

	/**
	 * Flag indicating whether or not the resource is assumed unchanged
	 */
	private boolean assumeUnchanged;

	@Override
	public boolean isTracked() {
		return tracked;
	}

	@Override
	public boolean isIgnored() {
		return ignored;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isMissing() {
		return missing;
	}

	@Override
	public StagingState getStagingState() {
		return staged;
	}

	@Override
	public final boolean isStaged() {
		return staged != StagingState.NOT_STAGED;
	}

	@Override
	public boolean hasConflicts() {
		return conflicts;
	}

	@Override
	public boolean isAssumeUnchanged() {
		return assumeUnchanged;
	}

	@Override
	public final boolean hasUnstagedChanges() {
		return !isIgnored()
				&& (!isTracked() || isDirty() || isMissing() || hasConflicts());
	}

	/**
	 * Sets the staged property.
	 *
	 * @param staged
	 *            value to set.
	 */
	protected void setStagingState(@NonNull StagingState staged) {
		this.staged = staged;
	}

	/**
	 * Sets the conflicts property.
	 *
	 * @param conflicts
	 *            value to set.
	 */
	protected void setConflicts(boolean conflicts) {
		this.conflicts = conflicts;
	}

	/**
	 * Sets the tracked property.
	 *
	 * @param tracked
	 *            value to set.
	 */
	protected void setTracked(boolean tracked) {
		this.tracked = tracked;
	}

	/**
	 * Sets the ignored property.
	 *
	 * @param ignored
	 *            value to set.
	 */
	protected void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	/**
	 * Sets the dirty property.
	 *
	 * @param dirty
	 *            value to set.
	 */
	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * Sets the missing property.
	 *
	 * @param missing
	 *            value to set.
	 */
	protected void setMissing(boolean missing) {
		this.missing = missing;
	}

	/**
	 * Sets the assumeUnchanged property.
	 *
	 * @param assumeUnchanged
	 *            value to set.
	 */
	protected void setAssumeUnchanged(boolean assumeUnchanged) {
		this.assumeUnchanged = assumeUnchanged;
	}

}

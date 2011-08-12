/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;

/**
 * Basic implementation of <code>IDecoratableResource</code>
 *
 * @see IDecoratableResource
 */
public class DecoratableResource implements IDecoratableResource {

	/**
	 * Resource to be decorated
	 */
	protected IResource resource = null;

	/**
	 * Name of the repository of the resource
	 */
	protected String repositoryName = null;

	/**
	 * Current branch of the resource
	 */
	protected String branch = null;

	/**
	 * Branch status relative to remote tracking branch
	 */
	protected String branchStatus = null;

	/**
	 * Flag indicating whether or not the resource is tracked
	 */
	protected boolean tracked = false;

	/**
	 * Flag indicating whether or not the resource is ignored
	 */
	protected boolean ignored = false;

	/**
	 * Flag indicating whether or not the resource has changes that are not
	 * staged
	 */
	protected boolean dirty = false;

	/**
	 * Staged state of the resource
	 */
	protected Staged staged = Staged.NOT_STAGED;

	/**
	 * Flag indicating whether or not the resource has merge conflicts
	 */
	protected boolean conflicts = false;

	/**
	 * Flag indicating whether or not the resource is assumed valid
	 */
	protected boolean assumeValid = false;

	/**
	 * Constructs a new decoratable resource
	 *
	 * This object represents the state of a resource used as a basis for
	 * decoration.
	 *
	 * @param resource
	 *            resource to be decorated
	 */
	protected DecoratableResource(IResource resource) {
		this.resource = resource;
	}

	public int getType() {
		return resource != null ? resource.getType() : 0;
	}

	public String getName() {
		return resource != null ? resource.getName() : null;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public String getBranch() {
		return branch;
	}

	public String getBranchStatus() {
		return branchStatus;
	}

	public boolean isTracked() {
		return tracked;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public boolean isDirty() {
		return dirty;
	}

	public Staged staged() {
		return staged;
	}

	public boolean hasConflicts() {
		return conflicts;
	}

	public boolean isAssumeValid() {
		return assumeValid;
	}
}

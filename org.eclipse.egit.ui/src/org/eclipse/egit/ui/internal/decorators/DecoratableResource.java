/*******************************************************************************
 * Copyright (C) 2011, 2015 Philipp Thun <philipp.thun@sap.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Factored out ResourceState
 *    Andre Bossert <anb0s@anbos.de> - Cleaning up the DecoratableResourceAdapter
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.resources.ResourceState;

/**
 * Basic implementation of <code>IDecoratableResource</code>
 *
 * @see IDecoratableResource
 */
public class DecoratableResource extends ResourceState
		implements IDecoratableResource {

	/**
	 * Resource to be decorated
	 */
	protected IResource resource = null;

	/**
	 * Name of the repository of the resource
	 */
	protected String repositoryName = null;

	/**
	 * Head commit of the repository of the resource
	 */
	protected String commitMessage = null;

	/**
	 * Current branch of the resource
	 */
	protected String branch = null;

	/**
	 * Branch status relative to remote tracking branch
	 */
	protected String branchStatus = null;

	/**
	 * is resource a repository container ?
	 */
	protected boolean isRepositoryContainer = false;

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

	/**
	 * @param isContainer
	 *            set to true if the resource is a repository container
	 */
	protected void setIsRepositoryContainer(boolean isContainer) {
		isRepositoryContainer = isContainer;
	}

	@Override
	public int getType() {
		return resource != null ? resource.getType() : 0;
	}

	@Override
	public String getName() {
		return resource != null ? resource.getName() : null;
	}

	@Override
	public String getRepositoryName() {
		return repositoryName;
	}

	@Override
	public String getCommitMessage() {
		return commitMessage;
	}

	@Override
	public String getBranch() {
		return branch;
	}

	@Override
	public String getBranchStatus() {
		return branchStatus;
	}

	@Override
	public boolean isRepositoryContainer() {
		return isRepositoryContainer;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getName() //$NON-NLS-1$
				+ (isTracked() ? ", tracked" : "") //$NON-NLS-1$ //$NON-NLS-2$
				+ (isIgnored() ? ", ignored" : "") //$NON-NLS-1$ //$NON-NLS-2$
				+ (isDirty() ? ", dirty" : "") //$NON-NLS-1$//$NON-NLS-2$
				+ (hasConflicts() ? ", conflicts" : "") //$NON-NLS-1$//$NON-NLS-2$
				+ ", staged=" + getStagingState() //$NON-NLS-1$
				+ "]"; //$NON-NLS-1$
	}

}

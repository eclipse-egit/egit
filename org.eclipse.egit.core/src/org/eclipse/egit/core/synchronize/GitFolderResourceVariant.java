/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.resource.GitResource;
import org.eclipse.team.core.TeamException;

class GitFolderResourceVariant extends GitResourceVariant {

	GitFolderResourceVariant(GitResource resource) {
		super(resource);
	}

	IContainer getContainer() {
		return (IContainer) getResource();
	}

	public boolean isContainer() {
		return true;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		return null;
	}

	public String getContentIdentifier() {
		return getResource().getContentIdentifier();
	}

	@Override
	public int hashCode() {
		return getResource().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() != obj.getClass()) {
			return false;
		}
		GitFolderResourceVariant other = (GitFolderResourceVariant) obj;
		return getResource().equals(other.getResource());
	}

	public byte[] asBytes() {
		return getName().getBytes();
	}

}

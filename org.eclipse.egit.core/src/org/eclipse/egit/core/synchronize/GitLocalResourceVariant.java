/*******************************************************************************
 * Copyright (c) 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Laurent Goubet <laurent.goubet@obeo.fr> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;

/**
 * Used by the GitSourceVariantTree when it needs to include local changes.
 * <p>
 * Mimics Team's LocalResourceVariant.
 * </p>
 */
class GitLocalResourceVariant implements IResourceVariant {
	private final IResource resource;

	GitLocalResourceVariant(IResource resource) {
		this.resource = resource;
	}

	@Override
	public byte[] asBytes() {
		try {
			return getContentIdentifier().getBytes("UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getContentIdentifier() {
		return new Date(resource.getLocalTimeStamp()).toString();
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		if (resource.getType() == IResource.FILE)
			return (IFile) resource;
		return null;
	}

	@Override
	public boolean isContainer() {
		return resource.getType() != IResource.FILE;
	}

	@Override
	public String getName() {
		return resource.getName();
	}

	IResource getResource() {
		return resource;
	}
}

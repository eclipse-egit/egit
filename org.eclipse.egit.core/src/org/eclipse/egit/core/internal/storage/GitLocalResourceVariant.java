/******************************************************************************
 * Copyright (c) 2015 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Laurent Goubet <laurent.goubet@obeo.fr> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

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
public class GitLocalResourceVariant implements IResourceVariant {
	private final IResource resource;

	/**
	 * Default constructor.
	 *
	 * @param resource
	 *            The resource we need a local variant for.
	 */
	public GitLocalResourceVariant(IResource resource) {
		this.resource = resource;
	}

	public byte[] asBytes() {
		try {
			return getContentIdentifier().getBytes("UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String getContentIdentifier() {
		return new Date(resource.getLocalTimeStamp()).toString();
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		if (resource.getType() == IResource.FILE) {
			return (IFile)resource;
		}
		return null;
	}

	public boolean isContainer() {
		return resource.getType() != IResource.FILE;
	}

	public String getName() {
		return resource.getName();
	}

	/**
	 * @return the underlying resource of this variant.
	 */
	public IResource getResource() {
		return resource;
	}
}

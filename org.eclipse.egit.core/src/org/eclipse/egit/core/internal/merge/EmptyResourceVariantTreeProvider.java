/*******************************************************************************
 * Copyright (C) 2014, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantTree;

/**
 * The "null" variant tree provider, devoid of supervised resources.
 */
public class EmptyResourceVariantTreeProvider implements
		GitResourceVariantTreeProvider {
	/** Empty base tree. */
	private IResourceVariantTree baseTree;

	/** Empty remote tree. */
	private IResourceVariantTree remoteTree;

	/** Empty source tree. */
	private IResourceVariantTree sourceTree;

	/**
	 * Default constructor.
	 */
	public EmptyResourceVariantTreeProvider() {
		this.baseTree = new EmptyResourceVariantTree();
		this.remoteTree = new EmptyResourceVariantTree();
		this.sourceTree = new EmptyResourceVariantTree();
	}

	public IResourceVariantTree getBaseTree() {
		return baseTree;
	}

	public IResourceVariantTree getRemoteTree() {
		return remoteTree;
	}

	public IResourceVariantTree getSourceTree() {
		return sourceTree;
	}

	public Set<IResource> getRoots() {
		return Collections.emptySet();
	}

	public Set<IResource> getKnownResources() {
		return Collections.emptySet();
	}

	private static class EmptyResourceVariantTree implements
			IResourceVariantTree {
		public IResource[] roots() {
			return new IResource[0];
		}

		public IResource[] members(IResource resource) throws TeamException {
			return new IResource[0];
		}

		public IResourceVariant getResourceVariant(IResource resource)
				throws TeamException {
			return null;
		}

		public boolean hasResourceVariant(IResource resource)
				throws TeamException {
			return false;
		}

		public IResource[] refresh(IResource[] resources, int depth,
				IProgressMonitor monitor) throws TeamException {
			return new IResource[0];
		}

		public void flushVariants(IResource resource, int depth)
				throws TeamException {
			// Empty implementation
		}

	}
}

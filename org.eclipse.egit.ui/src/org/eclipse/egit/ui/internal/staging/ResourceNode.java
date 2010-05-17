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
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A node in a tree for representing prospective folders or files reflecting a
 * repository's working tree status.
 */
class ResourceNode implements IAdaptable {

	private StatusNode root;

	private IResource resource;

	private ResourceNode parent;

	ResourceNode(StatusNode root, IResource resource, ResourceNode parent) {
		this.resource = resource;
		this.root = root;
		this.parent = parent;
	}

	StatusNode getRoot() {
		return root;
	}

	IResource getResource() {
		return resource;
	}

	ResourceNode getParent() {
		return parent;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IResource.class) {
			return resource;
		}
		return null;
	}

	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
			ResourceNode otherNode = (ResourceNode) other;
			return resource.equals(otherNode.resource)
					&& root.equals(otherNode.root);
		}
		return false;
	}

	public int hashCode() {
		return 31 * root.hashCode() * resource.hashCode();
	}

	public String toString() {
		if (parent != null && parent.getResource() instanceof IProject) {
			return resource.getProjectRelativePath().toString();
		}
		return resource.getName();
	}

}

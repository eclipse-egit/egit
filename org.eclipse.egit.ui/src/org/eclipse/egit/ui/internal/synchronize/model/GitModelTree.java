/*******************************************************************************

 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

/**
 * Representation of Git tree's in Git ChangeSet model
 */
public class GitModelTree extends GitModelObjectContainer {

	private final int kind;

	/**
	 * Absolute repository path
	 */
	protected final IPath path;

	final Map<String, GitModelObject> cachedTreeMap = new HashMap<String, GitModelObject>();

	/**
	 * @param parent
	 *            parent object
	 * @param fullPath
	 *            absolute object path
	 * @param kind
	 *            type of change
	 */
	public GitModelTree(GitModelObjectContainer parent, IPath fullPath,
			int kind) {
		super(parent);
		this.kind = kind;
		this.path = fullPath;
	}

	@Override
	public String getName() {
		return path.lastSegment();
	}

	@Override
	public IPath getLocation() {
		return path;
	}

	@Override
	public int getKind() {
		return kind;
	}

	@Override
	public int repositoryHashCode() {
		return getParent().repositoryHashCode();
	}

	@Override
	public GitModelObject[] getChildren() {
		Collection<GitModelObject> values = cachedTreeMap.values();

		return values.toArray(new GitModelObject[values.size()]);
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public void dispose() {
		for (GitModelObject value : cachedTreeMap.values())
			value.dispose();

		cachedTreeMap.clear();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getParent() == null) ? 0 : getParent().hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GitModelTree other = (GitModelTree) obj;
		if (getParent() == null) {
			if (other.getParent() != null)
				return false;
		} else if (!getParent().equals(other.getParent()))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ModelTree[location=" + getLocation() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}

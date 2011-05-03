/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.jgit.lib.Repository;


/**
 * A staged/unstaged entry in the table
 */
public class StagingEntry implements IAdaptable {
	/**
	 * State of the node
	 */
	public static enum State {
		/** added to the index, not in the tree */
		ADDED,
		/** changed from tree to index */
		CHANGED,
		/** removed from index, but in tree */
		REMOVED,
		/** in index, but not filesystem */
		MISSING,
		/** modified on disk relative to the index */
		MODIFIED,
		/** partially staged, modified in workspace and in index */
		PARTIALLY_MODIFIED,
		/** not ignored, and not in the index */
		UNTRACKED,
		/** in conflict */
		CONFLICTING;
	}

	private Repository repository;

	private State state;

	private String path;

	/**
	 *
	 * @param repository TODO
	 * @param modified
	 * @param file
	 */
	public StagingEntry(Repository repository, State modified, String file) {
		this.repository = repository;
		this.state = modified;
		this.path = file;
	}

	/**
	 * @return the full path for this node
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the state for this node
	 */
	public State getState() {
		return state;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IResource.class) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

			IContainer findContainer = IteratorService.findContainer(root, repository.getWorkTree());
			if (findContainer != null) {
				IResource findMember = findContainer.findMember(path);
				return findMember;
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
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
		StagingEntry other = (StagingEntry) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (state != other.state)
			return false;
		return true;
	}
}

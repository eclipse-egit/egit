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
 * A staged/unstaged node in the table
 */
public class GitXStagedNode implements IAdaptable {
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
		/** not ignored, and not in the index */
		UNTRACKED,
		/** in conflict */
		CONFLICTING
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
	public GitXStagedNode(Repository repository, State modified, String file) {
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
			IResource findMember = findContainer.findMember(path);

			return findMember;
		}
		return null;
	}
}

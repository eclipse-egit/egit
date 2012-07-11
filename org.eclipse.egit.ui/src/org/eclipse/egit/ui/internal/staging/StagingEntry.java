/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
		ADDED(EnumSet.of(Action.UNSTAGE)),

		/** changed from tree to index */
		CHANGED(EnumSet.of(Action.REPLACE_WITH_HEAD_REVISION, Action.UNSTAGE)),

		/** removed from index, but in tree */
		REMOVED(EnumSet.of(Action.REPLACE_WITH_HEAD_REVISION, Action.UNSTAGE)),

		/** in index, but not filesystem */
		MISSING(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX, Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE)),

		/** modified on disk relative to the index */
		MODIFIED(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX, Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE)),

		/** partially staged, modified in workspace and in index */
		PARTIALLY_MODIFIED(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX, Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE)),

		/** not ignored, and not in the index */
		UNTRACKED(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX, Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE)),

		/** in conflict */
		CONFLICTING(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX, Action.REPLACE_WITH_HEAD_REVISION,
					Action.STAGE, Action.LAUNCH_MERGE_TOOL));

		private final Set<Action> availableActions;

		private State(Set<Action> availableActions) {
			this.availableActions = availableActions;
		}

		/**
		 * @return set of available actions for the current state
		 */
		public Set<Action> getAvailableActions() {
			return availableActions;
		}
	}

	/**
	 * Possible actions available on a staging entry.
	 */
	enum Action {
		REPLACE_WITH_FILE_IN_GIT_INDEX,
		REPLACE_WITH_HEAD_REVISION,
		STAGE,
		UNSTAGE,
		LAUNCH_MERGE_TOOL
	}

	private Repository repository;

	private State state;

	private String path;

	private boolean submodule;

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
	 * @param submodule
	 */
	public void setSubmodule(final boolean submodule) {
		this.submodule = submodule;
	}

	/**
	 * @return true if submodule, false otherwise
	 */
	public boolean isSubmodule() {
		return submodule;
	}

	/**
	 * @return the full path for this node
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the repository for this node
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return the state for this node
	 */
	public State getState() {
		return state;
	}

	Set<Action> getAvailableActions() {
		return state.getAvailableActions();
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IResource.class) {
			IPath absolutePath = new Path(repository.getWorkTree().getAbsolutePath()).append(path);
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource resource = root.getFileForLocation(absolutePath);
			if (resource == null)
				resource = root.getFile(absolutePath);
			return resource;
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
